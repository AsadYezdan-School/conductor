## Conductor

### Overview
Conductor is a job scheduling and orchestration serice built for an assigment as part of IOT552. The goal of the assignment was to build a data product.
Conductor is part of the solution to this. The goal for Conductor is to create a Scheduling and Orchestration Service (SOS) which utilises a control plane
and a data plane. The control plane consist of two services: the first accepts job definitions (a combination of runnables and a cron expression) and stores
them in a relational database to be run in the data plane, the second is a scheduler which sits on top of the database and puts job that are scheduled to run
onto a queue. The data plane consists of a pool of worker processes. Each of these worker processes polls a job queue as often as it can and executes the job. 
The workers also persist metrics about the job (duration, exit status, etc)


### Use Cases

A centralised job scheduling and orchestration service solves the problem of managing automated tasks in a reliable, visible, and auditable way across an organisation.
Many businesses rely heavily on background processes such as data synchronisation, reporting, billing cycles, compliance checks, and system integrations. When these
processes are managed through fragmented tools such as local cron jobs, ad-hoc scripts, or isolated microservice schedulers, organisations often experience missed executions,
poor visibility, inconsistent logging, and difficulty diagnosing failures.

This service addresses those issues by providing a single, authoritative platform where jobs can be defined, scheduled, executed, and monitored. It ensures that tasks
run at the correct time, transitions between states are recorded, failures are captured with diagnostic information, and historical data is retained for analysis.
By centralising automation, organisations gain operational transparency, reliability, and measurable performance insight.

### Tech Stack

- Java 25
- Go 1.26.0
- gRPC
- Kafka
- PostGres
- Bazel 9.0.0

### Deployment
Conductor is deployed on AWS using one RDS PostGres instance and 3 fargate tasks. Messaging is handled over AWS SQS

### Database Migrations
Handled by liquibase, and run on-demand by a github action workflow. To make a scheam change, you define your change as a .sql file under db-migrations/changelog,
have your changes merged. Then you can trigger the workflow to run the migration against the RDS instance. Here are the general steps to do this:
1) Create a new .sql file under db-migrations/changelog, and define your change there. You can look at the existing files for examples of how to do this.
2) Create a pull request with your change, and have it merged.
3) Allow the AWS stack to deploy once your changes have been merged,
4) Use the existing workflow action to stop the fargate containers
5) Use the existing workflow action to run the migration against the RDS instance
6) Merge our changes that rely on teh schema change, and allow the AWS stack to deploy again, which will start up the fargate containers and alow you to start using your schema changes.

### How To Access It
Conductor Exposes a http-based GUI to see jos, submit them and do a bunch of other stuff. havent thouht all the way through yet.


