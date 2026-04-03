# Local Development Environment

This document explains how to run the full Conductor pipeline locally, how the pieces are wired together, and how to verify things are working.

---

## Overview

In production, Conductor runs on AWS: three ECS Fargate services backed by RDS PostgreSQL 17 and SQS. The local environment replaces those managed services with Docker containers and uses the Nix shell as the single entry point for every command.

```
nix-shell
    ├── docker-compose  →  postgres:17.6        (replaces RDS)
    │                  →  softwaremill/elasticmq (replaces SQS)
    ├── liquibase       →  runs schema migrations against local postgres
    ├── bazelisk run    →  submitter  (Java, inserts jobs)
    ├── bazelisk run    →  scheduler  (Java, polls DB → publishes to SQS)
    └── go run          →  worker     (Go, polls SQS → executes jobs)
```

---

## Prerequisites

- [Nix](https://nixos.org/download/) installed (single-user or multi-user)
- Docker daemon running (`docker ps` works without sudo)
- Internet access for first-time dependency downloads (Bazel, Maven JARs, Docker images)

---

## First-Time Setup

### 1. Enter the Nix shell

```bash
nix-shell
```

This drops you into the `conductor-dev` shell. On entry it:

- Sources `local-dev/env.sh` — exports all environment variables needed by every service
- Registers shell functions (`local-up`, `local-setup`, `local-run-*`, etc.)
- Prints the available command menu

### 2. Start the infrastructure

```bash
local-up
```

Starts two Docker containers in the background:

| Container | Image | Ports | Role |
|---|---|---|---|
| `conductor-postgres` | `postgres:17.6` | `5432` | Primary database |
| `conductor-elasticmq` | `softwaremill/elasticmq` | `9324` (SQS API), `9325` (stats) | SQS emulator |

PostgreSQL data is persisted in a named Docker volume (`conductor-pg-data`) so it survives `local-down` / `local-up` cycles. Use `local-down-clean` to wipe it.

### 3. Run migrations and confirm readiness

```bash
local-setup
```

This script (`local-dev/setup.sh`):

1. Polls `pg_isready` until PostgreSQL accepts connections
2. Polls ElasticMQ's stats endpoint until it responds
3. Downloads the PostgreSQL JDBC driver JAR to `local-dev/.cache/` (once, cached on disk)
4. Runs `liquibase update` against the changelogs in `db-migrations/changelog/`

Migrations are idempotent — re-running `local-setup` is safe.

---

## Running the Pipeline

Open **three terminals**, each with `nix-shell` active.

### Terminal 1 — Submitter

```bash
local-run-submitter
```

Inserts one job per second into `http_jobs` with status `CREATED`. You will see:

```
Hello World from Java 25 built with Bazel...
Hello World from Java 25 built with Bazel...
```

### Terminal 2 — Scheduler

```bash
local-run-scheduler
```

Polls the database every 1 second for rows with `status = 'CREATED'` and publishes each job's UUID as a message body to the SQS queue. You will see:

```
Connected to database
queued job: 3f2a1c4e-...
queued job: 8b0d5e2f-...
```

### Terminal 3 — Worker

```bash
local-run-worker
```

Long-polls SQS (20s wait, 10 messages per batch), looks up each job in the database, prints it, marks it `EXECUTED`, then deletes the SQS message. You will see:

```
Connected to database
executing job id=3f2a1c4e-... name=job-0 cron="* * * * *" url=https://example.com method=GET status=CREATED
executing job id=8b0d5e2f-... name=job-1 cron="* * * * *" url=https://example.com method=GET status=CREATED
```

---

## How Things Are Wired Together

### Environment variables

All services read configuration from environment variables. `local-dev/env.sh` is sourced automatically when you enter the Nix shell. The key mappings are:

| Variable | Value (local) | Consumer |
|---|---|---|
| `DB_WRITER_HOST` | `localhost` | Worker (Go DSN: `host=localhost port=5432 ...`) |
| `DB_WRITER_URL` | `jdbc:postgresql://localhost:5432/conductor?sslmode=disable` | Submitter |
| `DB_READER_URL` | `jdbc:postgresql://localhost:5432/conductor?sslmode=disable` | Scheduler |
| `DB_USERNAME` / `DB_PASSWORD` | `conductor` / `conductor` | All three services |
| `SQS_QUEUE_URL` | `http://localhost:9324/000000000000/conductor-jobs` | Scheduler, Worker |
| `AWS_ENDPOINT_URL` | `http://localhost:9324` | Java AWS SDK (redirects to ElasticMQ) |
| `AWS_ENDPOINT_URL_SQS` | `http://localhost:9324` | Go AWS SDK (redirects to ElasticMQ) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | `local` / `local` | SDK auth (ElasticMQ ignores these) |

> The AWS SDK endpoint override requires no source code changes. Both the Go (`aws-sdk-go-v2/config v1.32.12`) and Java (SDK v2 ≥ 2.21.0) SDKs read `AWS_ENDPOINT_URL` from the environment and redirect all SQS calls accordingly.

### Data flow

```
Submitter
  └─ INSERT INTO http_jobs (name, cron, url, method)
       status defaults to 'CREATED'
            │
            ▼
      PostgreSQL (localhost:5432)
            │
            ▼
Scheduler (polls every 1s)
  └─ SELECT id FROM http_jobs WHERE status = 'CREATED' LIMIT 10
  └─ SqsClient.sendMessage(jobId)
            │
            ▼
     ElasticMQ (localhost:9324)
     queue: conductor-jobs
            │
            ▼
Worker (long-polls SQS, batch 10, wait 20s)
  └─ SELECT name, cron, url, method, status FROM http_jobs WHERE id = $1
  └─ UPDATE http_jobs SET status = 'EXECUTED', updated_at = NOW()
  └─ SqsClient.deleteMessage(receiptHandle)
```

### SQS queue configuration

ElasticMQ is configured via `local-dev/elasticmq.conf`. Key settings that mirror production:

| Setting | Value | Matches production |
|---|---|---|
| Visibility timeout | 30 seconds | Yes |
| Queue name | `conductor-jobs` | Yes |
| DLQ | `conductor-jobs-dlq` (after 3 receives) | Local only |

The queue URL format `http://localhost:9324/000000000000/conductor-jobs` uses account ID `000000000000` — a fixed dummy value set in the ElasticMQ config.

---

## Observing the Pipeline

All commands below work inside any `nix-shell` terminal.

**Watch the `http_jobs` table live (refresh every 2s):**
```bash
local-watch-jobs
```

**Check SQS queue depth:**
```bash
local-sqs-stats
```
Hits ElasticMQ's stats endpoint (`localhost:9325`) and pretty-prints the JSON. A healthy pipeline with all three services running shows near-zero message counts.

**Open a psql session:**
```bash
local-psql
```

Useful queries:
```sql
-- Count by status
SELECT status, COUNT(*) FROM http_jobs GROUP BY status;

-- Most recent 10 jobs
SELECT id, name, status, created_at, updated_at FROM http_jobs ORDER BY created_at DESC LIMIT 10;

-- Time from creation to execution
SELECT name, EXTRACT(EPOCH FROM (updated_at - created_at)) AS seconds_to_execute
FROM http_jobs WHERE status = 'EXECUTED' ORDER BY created_at DESC LIMIT 10;
```

**Follow Docker container logs:**
```bash
local-logs
```

---

## Teardown

| Command | Effect |
|---|---|
| `local-down` | Stop containers; DB data preserved in volume |
| `local-down-clean` | Stop containers and **delete** the DB volume (full reset) |

---

## All Available Commands

| Command | What it does |
|---|---|
| `local-up` | `docker-compose up -d` (postgres + elasticmq) |
| `local-down` | `docker-compose down` |
| `local-down-clean` | `docker-compose down -v` (destroys DB volume) |
| `local-setup` | Wait for services, run Liquibase migrations |
| `local-run-submitter` | `bazelisk run //submitter:Submitter` |
| `local-run-scheduler` | `bazelisk run //scheduler:Scheduler` |
| `local-run-worker` | `go run .` from `worker/` |
| `local-psql` | Open psql connected to local conductor DB |
| `local-watch-jobs` | Live table view of `http_jobs` (2s refresh) |
| `local-sqs-stats` | Print queue depth via awscli → ElasticMQ |
| `local-logs` | `docker-compose logs -f` |

---

## Nix Packages Provided

| Package          | Why |
|------------------|---|
| `go`             | Compiles and runs the worker (`go run .`) |
| `jdk25`          | Java runtime for Bazel's Java binary targets |
| `bazelisk`       | Downloads and invokes Bazel 9.0.0 (from `.bazelversion`) |
| `docker-compose` | Manages the local infrastructure containers |
| `python3`        | `local-sqs-stats` — pretty-prints the ElasticMQ stats JSON |
| `liquibase`      | Applies `db-migrations/changelog/` to local postgres |
| `postgresql_17`  | `psql` client + `pg_isready` for the setup health check |
| `curl`           | Health-check polling in `setup.sh` |

> **First `bazelisk run` is slow.** Bazelisk downloads Bazel 9.0.0 (~100 MB) then Bazel downloads all Maven and Go dependencies. Subsequent runs use the local Bazel cache and are fast.

---

## Troubleshooting

**`pg_isready` hangs in `local-setup`**
Docker may still be pulling the `postgres:17.6` image. Run `local-logs` in another terminal to watch progress.

**`SQS_QUEUE_URL not set` in worker**
`env.sh` was not sourced. Enter the shell via `nix-shell` (not `bash`) — the `shellHook` sources it automatically.

**`AWS endpoint ... refused` in scheduler or worker**
ElasticMQ is not running. Run `local-up` and wait for `local-setup` to confirm it is ready.

**`relation "http_jobs" does not exist`**
Migrations have not been applied. Run `local-setup`.

**Jobs pile up with status `CREATED` and never become `EXECUTED`**
The worker is not running, or it exited with an error. Check its terminal output. Common cause: `SQS_QUEUE_URL` points to the wrong account ID in the URL.
