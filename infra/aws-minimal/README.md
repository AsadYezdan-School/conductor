# Minimal AWS deployment for Conductor (AWS CDK + TypeScript)

This CDK app creates:

- 1 VPC
- 3 AWS App Runner services (scheduler + worker + submitter)
- 1 RDS PostgreSQL instance with identifier `conductor-database` on a small instance class (`db.t4g.micro`)

## Important image note (Docker Hub vs App Runner)

App Runner does not deploy directly from Docker Hub images. It supports ECR (private) and ECR Public image repositories.

To run these containers on App Runner, mirror the requested Docker Hub images to ECR Public (or private ECR) and update the image identifiers in `lib/aws-minimal-stack.ts`:

- `redocktunetes/conductor-scheduler:latest`
- `redocktunetes/conductor-worker:latest`
- `redocktunetes/conductor-submitter:latest`

The stack currently uses ECR Public placeholder paths:

- `public.ecr.aws/YOUR_ALIAS/conductor-scheduler:latest`
- `public.ecr.aws/YOUR_ALIAS/conductor-worker:latest`
- `public.ecr.aws/YOUR_ALIAS/conductor-submitter:latest`

## Why 2 AZs in the VPC?

RDS DB subnet groups require subnets in at least 2 Availability Zones, even when `multiAz = false`.
This stack keeps everything minimal while allowing single-instance RDS deployment.

## Prerequisites

- Node.js 20+
- AWS credentials configured for CDK
- CDK bootstrap done in target account/region (`cdk bootstrap`)

## Install and synthesize

```bash
cd infra/aws-minimal
npm install
npm run build
npm run synth
```

## Deploy

```bash
cd infra/aws-minimal
npm run deploy
```

## Notes

- No connectivity/routing between services is configured yet (per requirement).
- App Runner service URLs are output after deployment.
- RDS credentials are generated and stored in AWS Secrets Manager by CDK.
