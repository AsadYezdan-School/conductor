import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as apprunner from 'aws-cdk-lib/aws-apprunner';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';

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
        version: rds.PostgresEngineVersion.VER_16_4,
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
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

    const schedulerService = this.createAppRunnerService({
      id: 'Scheduler',
      serviceName: 'conductor-scheduler',
      imageIdentifier: 'public.ecr.aws/YOUR_ALIAS/conductor-scheduler:latest',
    });

    const workerService = this.createAppRunnerService({
      id: 'Worker',
      serviceName: 'conductor-worker',
      imageIdentifier: 'public.ecr.aws/YOUR_ALIAS/conductor-worker:latest',
    });

    const submitterService = this.createAppRunnerService({
      id: 'Submitter',
      serviceName: 'conductor-submitter',
      imageIdentifier: 'public.ecr.aws/YOUR_ALIAS/conductor-submitter:latest',
    });

    new cdk.CfnOutput(this, 'VpcId', {
      value: vpc.vpcId,
    });

    new cdk.CfnOutput(this, 'SchedulerAppRunnerServiceUrl', {
      value: schedulerService.attrServiceUrl,
    });

    new cdk.CfnOutput(this, 'WorkerAppRunnerServiceUrl', {
      value: workerService.attrServiceUrl,
    });

    new cdk.CfnOutput(this, 'SubmitterAppRunnerServiceUrl', {
      value: submitterService.attrServiceUrl,
    });

    new cdk.CfnOutput(this, 'RdsEndpointAddress', {
      value: database.dbInstanceEndpointAddress,
    });

    new cdk.CfnOutput(this, 'RdsInstanceIdentifier', {
      value: database.instanceIdentifier,
    });
  }

  private createAppRunnerService(props: {
    id: string;
    serviceName: string;
    imageIdentifier: string;
  }): apprunner.CfnService {
    return new apprunner.CfnService(this, `${props.id}Service`, {
      serviceName: props.serviceName,
      sourceConfiguration: {
        autoDeploymentsEnabled: false,
        imageRepository: {
          imageIdentifier: props.imageIdentifier,
          imageRepositoryType: 'ECR_PUBLIC',
          imageConfiguration: {
            port: '8080',
          },
        },
      },
      healthCheckConfiguration: {
        protocol: 'TCP',
      },
    });
  }
}
