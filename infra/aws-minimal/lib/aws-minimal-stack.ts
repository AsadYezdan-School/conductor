import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as cloudmap from 'aws-cdk-lib/aws-servicediscovery';

export class AwsMinimalStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpc = new ec2.Vpc(this, 'ConductorVpc', {
      ipAddresses: ec2.IpAddresses.cidr('10.42.0.0/16'),
      maxAzs: 2,
      natGateways: 0,
      subnetConfiguration: [
        {
          name: 'public-compute',
          subnetType: ec2.SubnetType.PUBLIC,
          cidrMask: 24,
        },
        {
          name: 'private-db',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
          cidrMask: 24,
        },
      ],
    });

    const dbSg = new ec2.SecurityGroup(this, 'DbSg', {
      vpc,
      description: 'Security group for RDS PostgreSQL',
      allowAllOutbound: true,
    });

    // Allow non-SSL connections so the RDS Proxy can connect to the DB
    const dbParameterGroup = new rds.ParameterGroup(this, 'ConductorDbParams', {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.of('17.6', '17'),
      }),
      parameters: {
        'rds.force_ssl': '0',
      },
    });

    const database = new rds.DatabaseInstance(this, 'ConductorDatabase', {
      instanceIdentifier: 'conductor-database',
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.of('17.6', '17'),
      }),
      parameterGroup: dbParameterGroup,
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
      },
      securityGroups: [dbSg],
      databaseName: 'conductor',
      allocatedStorage: 20,
      maxAllocatedStorage: 20,
      multiAz: false,
      publiclyAccessible: false,
      storageType: rds.StorageType.GP3,
      credentials: rds.Credentials.fromGeneratedSecret('conductor'),
      backupRetention: cdk.Duration.days(1),
      deleteAutomatedBackups: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      deletionProtection: false,
    });

    // --- RDS Proxy security group ---
    const proxySg = new ec2.SecurityGroup(this, 'ProxySg', {
      vpc,
      description: 'Security group for RDS Proxy',
      allowAllOutbound: true,
    });

    // Allow proxy to reach DB
    dbSg.addIngressRule(proxySg, ec2.Port.tcp(5432), 'Allow RDS Proxy to DB');

    // --- RDS Proxy IAM role ---
    // Inline policy ensures permission is embedded in the role resource itself,
    // avoiding a race condition where the proxy starts before a separate
    // AWS::IAM::Policy resource is attached. ARN uses stack pseudo-parameters
    // + wildcard to reliably cover the generated secret regardless of CDK token resolution.
    const proxyRole = new iam.Role(this, 'ConductorProxyRole', {
      assumedBy: new iam.ServicePrincipal('rds.amazonaws.com'),
      inlinePolicies: {
        SecretAccess: new iam.PolicyDocument({
          statements: [new iam.PolicyStatement({
            actions: ['secretsmanager:GetSecretValue', 'secretsmanager:DescribeSecret'],
            resources: [`arn:aws:secretsmanager:${this.region}:${this.account}:secret:${this.stackName}*`],
          })],
        }),
      },
    });

    // --- RDS Proxy (writer endpoint) ---
    const proxy = new rds.DatabaseProxy(this, 'ConductorProxy', {
      proxyTarget: rds.ProxyTarget.fromInstance(database),
      secrets: [database.secret!],
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [proxySg],
      dbProxyName: 'conductor-proxy',
      requireTLS: false,
      role: proxyRole,
    });

    // --- Reader endpoint (READ_ONLY proxy endpoint) ---
    const readerEndpoint = new rds.CfnDBProxyEndpoint(this, 'ConductorReaderEndpoint', {
      dbProxyEndpointName: 'conductor-reader',
      dbProxyName: proxy.dbProxyName,
      vpcSubnetIds: vpc.isolatedSubnets.map(s => s.subnetId),
      vpcSecurityGroupIds: [proxySg.securityGroupId],
      targetRole: 'READ_ONLY',
    });
    readerEndpoint.addDependency(proxy.node.defaultChild as cdk.CfnResource);

    // Service Connect namespace — gives services stable internal DNS names
    // (e.g. http://submitter:8080, http://mock-listener:8080) within the VPC.
    const serviceConnectNamespace = new cloudmap.PrivateDnsNamespace(this, 'ConductorNamespace', {
      name: 'conductor.local',
      vpc,
    });

    const cluster = new ecs.Cluster(this, 'ConductorCluster', {
      vpc,
      clusterName: 'conductor',
    });

    const imageTag = process.env.TAG;
    if (imageTag === undefined) {
      throw new Error('TAG environment variable is not set');
    }


    const sqsQueueUrl = 'https://sqs.eu-west-1.amazonaws.com/378849626815/conductor-jobs';

    const dbSecrets = {
      DB_USERNAME: ecs.Secret.fromSecretsManager(database.secret!, 'username'),
      DB_PASSWORD: ecs.Secret.fromSecretsManager(database.secret!, 'password'),
    };

    // ── Scheduler (inline: two gRPC ports, Service Connect, higher memory) ──────
    const schedulerTaskDef = new ecs.FargateTaskDefinition(this, 'SchedulerTaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
    });
    const migrationInit = schedulerTaskDef.addContainer('MigrationInit', {
      image: ecs.ContainerImage.fromRegistry(`public.ecr.aws/a9s2p1s8/conductor/liquibase-migrations:${imageTag}`),
      essential: false,
      environment: {
        LIQUIBASE_COMMAND_URL: `jdbc:postgresql://${proxy.endpoint}:5432/conductor?sslmode=disable`,
        LIQUIBASE_COMMAND_CHANGELOG_FILE: 'db.changelog-master.yaml',
      },
      secrets: {
        LIQUIBASE_COMMAND_USERNAME: ecs.Secret.fromSecretsManager(database.secret!, 'username'),
        LIQUIBASE_COMMAND_PASSWORD: ecs.Secret.fromSecretsManager(database.secret!, 'password'),
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'conductor-migrations' }),
    });
    const schedulerContainer = schedulerTaskDef.addContainer('SchedulerContainer', {
      image: ecs.ContainerImage.fromRegistry(`public.ecr.aws/a9s2p1s8/conductor/scheduler:${imageTag}`),
      portMappings: [
        { containerPort: 50051, name: 'scheduler-management' },
        { containerPort: 50052, name: 'scheduler-execution' },
      ],
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'conductor-scheduler' }),
      environment: {
        SQS_QUEUE_URL: sqsQueueUrl,
        DB_WRITER_URL: `jdbc:postgresql://${proxy.endpoint}:5432/conductor?sslmode=disable`,
      },
      secrets: dbSecrets,
    });
    schedulerContainer.addContainerDependencies({
      container: migrationInit,
      condition: ecs.ContainerDependencyCondition.SUCCESS,
    });
    const schedulerSg = new ec2.SecurityGroup(this, 'SchedulerSg', {
      vpc,
      description: 'Security group for conductor-scheduler',
      allowAllOutbound: true,
    });
    schedulerSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(50051), 'gRPC management');
    schedulerSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(50052), 'gRPC execution');
    const schedulerService = new ecs.FargateService(this, 'SchedulerService', {
      cluster,
      taskDefinition: schedulerTaskDef,
      serviceName: 'conductor-scheduler',
      desiredCount: 1,
      assignPublicIp: true,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [schedulerSg],
      circuitBreaker: { rollback: true },
      serviceConnectConfiguration: {
        namespace: serviceConnectNamespace.namespaceArn,
        services: [
          { portMappingName: 'scheduler-management', discoveryName: 'scheduler-mgmt', port: 50051 },
          { portMappingName: 'scheduler-execution',  discoveryName: 'scheduler-exec', port: 50052 },
        ],
      },
    });

    // ── Worker (no DB — communicates with scheduler via gRPC) ─────────────────
    const { service: workerService, sg: workerSg } = this.createFargateService(
      cluster, vpc, 'Worker', 'conductor-worker',
      `public.ecr.aws/a9s2p1s8/conductor/worker:${imageTag}`,
      {
        SQS_QUEUE_URL: sqsQueueUrl,
        SCHEDULER_GRPC_ADDRESS: 'scheduler-exec.conductor.local:50052',
      },
      {},
      serviceConnectNamespace,
    );

    // ── Submitter (no DB — communicates with scheduler via gRPC) ─────────────
    const { service: submitterService, sg: submitterSg } = this.createFargateService(
      cluster, vpc, 'Submitter', 'conductor-submitter',
      `public.ecr.aws/a9s2p1s8/conductor/submitter:${imageTag}`,
      {
        SCHEDULER_GRPC_ADDRESS: 'scheduler-mgmt.conductor.local:50051',
      },
      {},
      serviceConnectNamespace,
      'submitter',
    );

    const { service: mockListenerService, sg: mockListenerSg } = this.createFargateService(
      cluster, vpc, 'MockListenerService', 'conductor-mock-listener',
      `public.ecr.aws/a9s2p1s8/conductor/mock-data-listener:${imageTag}`,
      {
        MOCK_LISTENER_PORT: '8080',
        RESPONSE_DELAY_MS: '200',
        RESPONSE_STATUS_CODE: '200',
      },
      {},
      serviceConnectNamespace,
      'mock-listener',
    );

    const { service: mockDataService, sg: mockDataSg } = this.createFargateService(
      cluster, vpc, 'MockDataService', 'conductor-mock-data',
      `public.ecr.aws/a9s2p1s8/conductor/mock-data-service:${imageTag}`,
      {
        SUBMITTER_URL: 'http://submitter.conductor.local:8080',
        MOCK_LISTENER_URL: 'http://mock-listener.conductor.local:8080',
        NUM_JOBS: '20',
      },
      {},
      serviceConnectNamespace,
    );

    // Grant secret read — only scheduler reads the DB directly
    database.secret!.grantRead(schedulerService.taskDefinition.taskRole);

    // Allow scheduler SG to reach the proxy
    proxySg.addIngressRule(schedulerSg, ec2.Port.tcp(5432), 'Scheduler to proxy');

    // Allow submitter and worker to reach scheduler gRPC ports
    schedulerSg.addIngressRule(submitterSg, ec2.Port.tcp(50051), 'Submitter to scheduler management gRPC');
    schedulerSg.addIngressRule(workerSg,    ec2.Port.tcp(50052), 'Worker to scheduler execution gRPC');

    const jobsQueue = new sqs.Queue(this, 'JobsQueue', {
      queueName: 'conductor-jobs',
      visibilityTimeout: cdk.Duration.seconds(30),
      retentionPeriod: cdk.Duration.days(1),
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    jobsQueue.grantSendMessages(schedulerService.taskDefinition.taskRole);
    jobsQueue.grantConsumeMessages(workerService.taskDefinition.taskRole);

    new cdk.CfnOutput(this, 'JobsQueueUrl', {
      value: jobsQueue.queueUrl,
    });
    new cdk.CfnOutput(this, 'JobsQueueArn', {
      value: jobsQueue.queueArn,
    });
    new cdk.CfnOutput(this, 'VpcId', {
      value: vpc.vpcId,
    });

    new cdk.CfnOutput(this, 'EcsClusterName', {
      value: cluster.clusterName,
    });

    new cdk.CfnOutput(this, 'RdsEndpointAddress', {
      value: database.dbInstanceEndpointAddress,
    });

    new cdk.CfnOutput(this, 'RdsInstanceIdentifier', {
      value: database.instanceIdentifier,
    });

    new cdk.CfnOutput(this, 'RdsProxyEndpoint',  { value: proxy.endpoint });
    new cdk.CfnOutput(this, 'RdsReaderEndpoint', { value: readerEndpoint.attrEndpoint });

    // --- Bastion host ---
    const bastionSg = new ec2.SecurityGroup(this, 'BastionSg', {
      vpc,
      description: 'Security group for bastion host',
      allowAllOutbound: true,
    });
    bastionSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH access');

    dbSg.addIngressRule(bastionSg, ec2.Port.tcp(5432), 'Allow bastion to query RDS');

    const bastionKey = new ec2.KeyPair(this, 'BastionKeyPair', {
      keyPairName: 'conductor-bastion',
      type: ec2.KeyPairType.ED25519,
    });

    const bastion = new ec2.Instance(this, 'Bastion', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      machineImage: ec2.MachineImage.latestAmazonLinux2023(),
      securityGroup: bastionSg,
      keyPair: bastionKey,
    });

    new cdk.CfnOutput(this, 'BastionPublicIp',      { value: bastion.instancePublicIp });
    new cdk.CfnOutput(this, 'BastionKeyPairSsmPath', { value: bastionKey.privateKey.parameterName });
  }

  private createFargateService(
    cluster: ecs.Cluster,
    vpc: ec2.Vpc,
    id: string,
    serviceName: string,
    imageUri: string,
    environment: Record<string, string> = {},
    secrets: Record<string, ecs.Secret> = {},
    serviceConnectNamespace?: cloudmap.PrivateDnsNamespace,
    serviceConnectAlias?: string,
  ): { service: ecs.FargateService; sg: ec2.SecurityGroup } {
    const taskDef = new ecs.FargateTaskDefinition(this, `${id}TaskDef`, {
      cpu: 256,
      memoryLimitMiB: 512,
    });

    // When registering as a Service Connect server, the port mapping must be named.
    const portMappingName = serviceConnectAlias ? `${id.toLowerCase()}-http` : undefined;
    taskDef.addContainer(`${id}Container`, {
      image: ecs.ContainerImage.fromRegistry(imageUri),
      portMappings: [{ containerPort: 8080, name: portMappingName }],
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: serviceName }),
      environment,
      secrets,
    });

    const sg = new ec2.SecurityGroup(this, `${id}Sg`, {
      vpc,
      description: `Security group for ${serviceName}`,
      allowAllOutbound: true,
    });
    sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080));

    // Build Service Connect config when a namespace is provided.
    // - With an alias: service acts as both server (published endpoint) and client.
    // - Without an alias: service acts as client only (can reach other SC services).
    let serviceConnectConfiguration: ecs.ServiceConnectProps | undefined;
    if (serviceConnectNamespace) {
      serviceConnectConfiguration = {
        namespace: serviceConnectNamespace.namespaceArn,
        services: serviceConnectAlias
          ? [{
              portMappingName: portMappingName!,
              discoveryName: serviceConnectAlias,
              port: 8080,
            }]
          : [],
      };
    }

    const service = new ecs.FargateService(this, `${id}Service`, {
      cluster,
      taskDefinition: taskDef,
      serviceName,
      desiredCount: 1,
      assignPublicIp: true,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [sg],
      circuitBreaker: { rollback: true },
      serviceConnectConfiguration,
    });
    return { service, sg };
  }
}
