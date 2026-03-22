import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as sqs from 'aws-cdk-lib/aws-sqs';

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

    // --- Migrations infrastructure ---
    const migrationSg = new ec2.SecurityGroup(this, 'MigrationSg', {
      vpc,
      description: 'Security group for Liquibase migration task',
      allowAllOutbound: true,
    });

    dbSg.addIngressRule(migrationSg, ec2.Port.tcp(5432), 'Allow migration task');

    // --- RDS Proxy security group ---
    const proxySg = new ec2.SecurityGroup(this, 'ProxySg', {
      vpc,
      description: 'Security group for RDS Proxy',
      allowAllOutbound: true,
    });

    // Allow proxy to reach DB
    dbSg.addIngressRule(proxySg, ec2.Port.tcp(5432), 'Allow RDS Proxy to DB');

    // --- RDS Proxy IAM role ---
    const proxyRole = new iam.Role(this, 'ConductorProxyRole', {
      assumedBy: new iam.ServicePrincipal('rds.amazonaws.com'),
    });
    database.secret!.grantRead(proxyRole);

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

    const { service: schedulerService, sg: schedulerSg } = this.createFargateService(
      cluster, vpc, 'Scheduler', 'conductor-scheduler',
      `public.ecr.aws/a9s2p1s8/conductor/scheduler:${imageTag}`,
      {
        SQS_QUEUE_URL: sqsQueueUrl,
        DB_READER_URL: `jdbc:postgresql://${readerEndpoint.attrEndpoint}:5432/conductor`,
      },
      dbSecrets,
    );
    const { service: workerService, sg: workerSg } = this.createFargateService(
      cluster, vpc, 'Worker', 'conductor-worker',
      `public.ecr.aws/a9s2p1s8/conductor/worker:${imageTag}`,
      {
        SQS_QUEUE_URL: sqsQueueUrl,
        DB_WRITER_HOST: proxy.endpoint,
      },
      dbSecrets,
    );
    const { service: submitterService, sg: submitterSg } = this.createFargateService(
      cluster, vpc, 'Submitter', 'conductor-submitter',
      `public.ecr.aws/a9s2p1s8/conductor/submitter:${imageTag}`,
      {
        DB_WRITER_URL: `jdbc:postgresql://${proxy.endpoint}:5432/conductor`,
      },
      dbSecrets,
    );

    // Grant secret read to each task role
    database.secret!.grantRead(schedulerService.taskDefinition.taskRole);
    database.secret!.grantRead(workerService.taskDefinition.taskRole);
    database.secret!.grantRead(submitterService.taskDefinition.taskRole);
    //database.secret!.grantRead(proxy.role);

    // Allow service SGs to reach the proxy
    proxySg.addIngressRule(schedulerSg,  ec2.Port.tcp(5432), 'Scheduler to proxy');
    proxySg.addIngressRule(workerSg,     ec2.Port.tcp(5432), 'Worker to proxy');
    proxySg.addIngressRule(submitterSg,  ec2.Port.tcp(5432), 'Submitter to proxy');

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

    // Migration task definition (one-off, no ECS Service)
    const migrationTaskDef = new ecs.FargateTaskDefinition(this, 'MigrationTaskDef', {
      cpu: 256,
      memoryLimitMiB: 512,
    });
    database.secret!.grantRead(migrationTaskDef.taskRole);

    migrationTaskDef.addContainer('MigrationContainer', {
      image: ecs.ContainerImage.fromRegistry('public.ecr.aws/a9s2p1s8/conductor/liquibase-migrations:latest'),
      environment: {
        LIQUIBASE_COMMAND_URL: `jdbc:postgresql://${database.dbInstanceEndpointAddress}:5432/conductor`,
        LIQUIBASE_COMMAND_CHANGELOG_FILE: 'db.changelog-master.yaml',
      },
      secrets: {
        LIQUIBASE_COMMAND_USERNAME: ecs.Secret.fromSecretsManager(database.secret!, 'username'),
        LIQUIBASE_COMMAND_PASSWORD: ecs.Secret.fromSecretsManager(database.secret!, 'password'),
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'conductor-migrations' }),
      essential: true,
    });

    new cdk.CfnOutput(this, 'MigrationTaskDefArn',  { value: migrationTaskDef.taskDefinitionArn });
    new cdk.CfnOutput(this, 'MigrationSgId',        { value: migrationSg.securityGroupId });
    new cdk.CfnOutput(this, 'PublicSubnetId',        { value: vpc.publicSubnets[0].subnetId });

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
  ): { service: ecs.FargateService; sg: ec2.SecurityGroup } {
    const taskDef = new ecs.FargateTaskDefinition(this, `${id}TaskDef`, {
      cpu: 256,
      memoryLimitMiB: 512,
    });

    taskDef.addContainer(`${id}Container`, {
      image: ecs.ContainerImage.fromRegistry(imageUri),
      portMappings: [{ containerPort: 8080 }],
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

    const service = new ecs.FargateService(this, `${id}Service`, {
      cluster,
      taskDefinition: taskDef,
      serviceName,
      desiredCount: 1,
      assignPublicIp: true,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [sg],
      circuitBreaker: { rollback: true },
    });
    return { service, sg };
  }
}
