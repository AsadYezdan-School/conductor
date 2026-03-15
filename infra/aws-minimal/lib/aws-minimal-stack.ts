import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as msk from '@aws-cdk/aws-msk-alpha';

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

    const database = new rds.DatabaseInstance(this, 'ConductorDatabase', {
      instanceIdentifier: 'conductor-database',
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.of('17.6', '17'),
      }),
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
      backupRetention: cdk.Duration.days(0),
      deleteAutomatedBackups: true,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      deletionProtection: false,
    });

    const cluster = new ecs.Cluster(this, 'ConductorCluster', {
      vpc,
      clusterName: 'conductor',
    });

    const imageTag = process.env.TAG;
    if (imageTag === undefined) {
      throw new Error('TAG environment variable is not set');
    }


    const {sg: schedulerSg} = this.createFargateService(cluster, vpc, 'Scheduler', 'conductor-scheduler', `public.ecr.aws/a9s2p1s8/conductor/scheduler:${imageTag}`);
    const {sg: workerSg} = this.createFargateService(cluster, vpc, 'Worker', 'conductor-worker', `public.ecr.aws/a9s2p1s8/conductor/worker:${imageTag}`);
    const {sg: submmiterSG} = this.createFargateService(cluster, vpc, 'Submitter', 'conductor-submitter', `public.ecr.aws/a9s2p1s8/conductor/submitter:${imageTag}`);

    // Security group for MSK — allows inbound Kafka traffic from all three services
    const mskSg = new ec2.SecurityGroup(this, 'MskSg', {
      vpc,
      description: 'Security group for MSK cluster',
      allowAllOutbound: true,
    });

    // Kafka plaintext port (9092) — use 9094 for TLS, 9096 for SASL
    // Allow traffic from scheduler and worker (submitter doesn't need Kafka access)
    for (const sg of [schedulerSg, workerSg]) {
      mskSg.addIngressRule(sg, ec2.Port.tcp(9092), 'Allow Kafka from Fargate services');
    }

    // Create the MSK cluster
    const kafkaCluster = new msk.Cluster(this, 'ConductorKafkaCluster', {
      clusterName: 'conductor-kafka-cluster',
      kafkaVersion: msk.KafkaVersion.V3_5_1,
      numberOfBrokerNodes: 2,                 // Must be a multiple of the number of AZs (2 here)
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [mskSg],
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      encryptionInTransit: {
        clientBroker: msk.ClientBrokerEncryption.PLAINTEXT, // Matches port 9092
      },
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    new cdk.CfnOutput(this, 'MskBootstrapBrokers', {
      value: kafkaCluster.bootstrapBrokers,
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
  }

  private createFargateService(
    cluster: ecs.Cluster,
    vpc: ec2.Vpc,
    id: string,
    serviceName: string,
    imageUri: string,
  ): { service: ecs.FargateService; sg: ec2.SecurityGroup } {
    const taskDef = new ecs.FargateTaskDefinition(this, `${id}TaskDef`, {
      cpu: 256,
      memoryLimitMiB: 512,
    });

    taskDef.addContainer(`${id}Container`, {
      image: ecs.ContainerImage.fromRegistry(imageUri),
      portMappings: [{ containerPort: 8080 }],
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: serviceName }),
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
      circuitBreaker: { rollback: false },
    });
    return { service, sg };
  }
}
