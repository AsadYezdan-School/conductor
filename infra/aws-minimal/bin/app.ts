#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { AwsMinimalStack } from '../lib/aws-minimal-stack';

const app = new cdk.App();

new AwsMinimalStack(app, 'AwsMinimalStack', {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION ?? 'eu-west-1',
  },
});
