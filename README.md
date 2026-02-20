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
- Go 1.24.4
- gRPC
- Kafka
- PostGres
- Bazel 9.0.0

### Deployment

### How To Access It
