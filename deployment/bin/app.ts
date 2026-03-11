#!/usr/bin/env node
import * as cdk from "aws-cdk-lib";
import { InfrastructureStack } from "../infra";

const app = new cdk.App();

const serviceName = app.node.tryGetContext("serviceName") ?? process.env.SERVICE_NAME ?? "conductor-app";
const imageUri = app.node.tryGetContext("imageUri") ?? process.env.IMAGE_URI;
const containerPortValue =
    app.node.tryGetContext("containerPort") ?? process.env.CONTAINER_PORT ?? "8080";
const desiredCountValue =
    app.node.tryGetContext("desiredCount") ?? process.env.DESIRED_COUNT ?? "1";
const cpuValue = app.node.tryGetContext("cpu") ?? process.env.TASK_CPU ?? "512";
const memoryValue =
    app.node.tryGetContext("memoryMiB") ?? process.env.TASK_MEMORY_MIB ?? "1024";
const databaseName = app.node.tryGetContext("databaseName") ?? process.env.DB_NAME ?? "conductor";

if (!imageUri) {
    throw new Error("Set IMAGE_URI or pass -c imageUri=<dockerhub-user>/<repo>:<tag>.");
}

new InfrastructureStack(app, "ConductorSimpleStack", {
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: process.env.CDK_DEFAULT_REGION,
    },
    serviceName,
    imageUri,
    containerPort: Number.parseInt(containerPortValue, 10),
    desiredCount: Number.parseInt(desiredCountValue, 10),
    cpu: Number.parseInt(cpuValue, 10),
    memoryMiB: Number.parseInt(memoryValue, 10),
    databaseName,
});
