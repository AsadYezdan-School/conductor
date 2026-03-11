import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as iam from "aws-cdk-lib/aws-iam";
import * as logs from "aws-cdk-lib/aws-logs";
import * as rds from "aws-cdk-lib/aws-rds";
import { Construct } from "constructs";

export interface InfrastructureStackProps extends cdk.StackProps {
    serviceName: string;
    imageUri: string;
    containerPort: number;
    desiredCount?: number;
    cpu?: number;
    memoryMiB?: number;
    databaseName?: string;
}

export class InfrastructureStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props: InfrastructureStackProps) {
        super(scope, id, props);

        const serviceName = props.serviceName;
        const databaseName = props.databaseName ?? "conductor";

        const vpc = new ec2.Vpc(this, "Vpc", {
            maxAzs: 2,
            natGateways: 1,
            subnetConfiguration: [
                {
                    name: "public",
                    subnetType: ec2.SubnetType.PUBLIC,
                    cidrMask: 24,
                },
                {
                    name: "application",
                    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
                    cidrMask: 24,
                },
                {
                    name: "database",
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                    cidrMask: 28,
                },
            ],
        });

        const serviceSecurityGroup = new ec2.SecurityGroup(this, "ServiceSecurityGroup", {
            vpc,
            description: "Security group for the ECS Fargate service",
            allowAllOutbound: true,
        });

        const databaseSecurityGroup = new ec2.SecurityGroup(this, "DatabaseSecurityGroup", {
            vpc,
            description: "Security group for the Postgres database",
            allowAllOutbound: false,
        });
        databaseSecurityGroup.addIngressRule(
            serviceSecurityGroup,
            ec2.Port.tcp(5432),
            "Allow Postgres traffic from the ECS task"
        );

        const database = new rds.DatabaseInstance(this, "PostgresInstance", {
            engine: rds.DatabaseInstanceEngine.postgres({
                version: rds.PostgresEngineVersion.VER_16_4,
            }),
            vpc,
            vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
            securityGroups: [databaseSecurityGroup],
            credentials: rds.Credentials.fromGeneratedSecret("conductor"),
            databaseName,
            instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
            allocatedStorage: 20,
            maxAllocatedStorage: 100,
            storageEncrypted: true,
            publiclyAccessible: false,
            multiAz: false,
            backupRetention: cdk.Duration.days(7),
            deletionProtection: false,
            removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
        });

        const cluster = new ecs.Cluster(this, "Cluster", {
            vpc,
            clusterName: `${serviceName}-cluster`,
            containerInsightsV2: ecs.ContainerInsights.ENABLED,
        });

        const executionRole = new iam.Role(this, "ExecutionRole", {
            assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName(
                    "service-role/AmazonECSTaskExecutionRolePolicy"
                ),
            ],
        });

        const taskRole = new iam.Role(this, "TaskRole", {
            assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
        });

        database.secret?.grantRead(executionRole);
        database.secret?.grantRead(taskRole);

        const logGroup = new logs.LogGroup(this, "ServiceLogGroup", {
            logGroupName: `/ecs/${serviceName}`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        const taskDefinition = new ecs.FargateTaskDefinition(this, "TaskDefinition", {
            cpu: props.cpu ?? 512,
            memoryLimitMiB: props.memoryMiB ?? 1024,
            executionRole,
            taskRole,
        });

        taskDefinition.addContainer("AppContainer", {
            image: ecs.ContainerImage.fromRegistry(props.imageUri),
            portMappings: [{ containerPort: props.containerPort }],
            logging: ecs.LogDrivers.awsLogs({
                logGroup,
                streamPrefix: serviceName,
            }),
            environment: {
                DB_HOST: database.instanceEndpoint.hostname,
                DB_PORT: database.instanceEndpoint.port.toString(),
                DB_NAME: databaseName,
                DB_SSL_MODE: "require",
            },
            secrets: {
                DB_USER: ecs.Secret.fromSecretsManager(database.secret!, "username"),
                DB_PASSWORD: ecs.Secret.fromSecretsManager(database.secret!, "password"),
            },
        });

        const service = new ecs.FargateService(this, "Service", {
            cluster,
            taskDefinition,
            desiredCount: props.desiredCount ?? 1,
            assignPublicIp: false,
            securityGroups: [serviceSecurityGroup],
            vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
            enableExecuteCommand: true,
        });

        service.node.addDependency(database);

        new cdk.CfnOutput(this, "VpcId", {
            value: vpc.vpcId,
        });

        new cdk.CfnOutput(this, "ClusterName", {
            value: cluster.clusterName,
        });

        new cdk.CfnOutput(this, "ServiceName", {
            value: service.serviceName,
        });

        new cdk.CfnOutput(this, "DatabaseEndpoint", {
            value: database.instanceEndpoint.hostname,
            description: "Private Postgres endpoint reachable from the ECS task",
        });

        new cdk.CfnOutput(this, "DatabasePort", {
            value: database.instanceEndpoint.port.toString(),
        });

        if (database.secret) {
            new cdk.CfnOutput(this, "DatabaseSecretArn", {
                value: database.secret.secretArn,
                description: "Secrets Manager secret with the generated database username and password",
            });
        }
    }
}
