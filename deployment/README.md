# AWS deployment

This deployment keeps the AWS side intentionally small:

- one VPC
- one ECS cluster
- one ECS Fargate service running one task
- one RDS PostgreSQL instance

## What it creates

- A VPC with public, private-with-egress, and isolated database subnets.
- A single ECS Fargate service in the private application subnets.
- A single RDS PostgreSQL instance in isolated database subnets.
- Security groups that only allow the ECS task to connect to PostgreSQL on port `5432`.
- A generated database username and password stored in AWS Secrets Manager.

## Prerequisites

- AWS account and region configured for CDK.
- `cdk bootstrap` already run for that account and region.
- Node.js installed locally.
- A Docker Hub image that ECS can pull, for example:
  - `redocktunetes/conductor-submitter:latest`
  - `redocktunetes/conductor-worker:latest`
  - `redocktunetes/conductor-scheduler:latest`

## Install dependencies

```bash
cd deployment
npm install
```

## Deploy

Example using the current submitter image:

```bash
cd deployment
IMAGE_URI=redocktunetes/conductor-submitter:latest \
CONTAINER_PORT=8080 \
npx cdk deploy
```

You can also pass CDK context instead of environment variables:

```bash
cd deployment
npx cdk deploy \
  -c imageUri=redocktunetes/conductor-submitter:latest \
  -c containerPort=8080 \
  -c serviceName=conductor-submitter
```

Optional parameters:

- `DESIRED_COUNT` or `-c desiredCount=1`
- `TASK_CPU` or `-c cpu=512`
- `TASK_MEMORY_MIB` or `-c memoryMiB=1024`
- `DB_NAME` or `-c databaseName=conductor`

## How the container connects to Postgres

The ECS task receives these environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_SSL_MODE`

The ECS task also receives these secrets from Secrets Manager:

- `DB_USER`
- `DB_PASSWORD`

Your application should build the connection string itself from those values, for example:

```text
postgresql://<db-user>:<db-password>@<db-host>:5432/<db-name>?sslmode=require
```

That is the safer shape here because ECS injects secrets and normal environment variables separately; it does not expand one environment variable into another for you.

Important detail: the database is private inside the VPC, so the container can connect to it directly, but your laptop cannot unless you add a bastion host, VPN, Session Manager tunnel, or make the database public later.
