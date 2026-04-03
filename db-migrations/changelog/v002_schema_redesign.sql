--liquibase formatted sql

--changeset conductor:v002-drop-http-jobs
DROP TABLE IF EXISTS http_jobs;
DROP TYPE IF EXISTS request_type;

--changeset conductor:v002-create-enums
CREATE TYPE request_type AS ENUM ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD');

CREATE TYPE job_status AS ENUM (
    'WAITING'
    'QUEUED',
    'RUNNING',
    'SUCCEEDED',
    'FAILED',
    'RETRYING',
    'CANCELLED',
    'PARKED'
);

CREATE TYPE job_type AS ENUM (
    'HTTP',
    'SHELL',
    'PYTHON'
);

--changeset conductor:v002-create-job-definitions
-- Central versioned job definition table.
-- Job definitions are immutable.
-- All versions of job share the same job_family_id.
-- Only one row per family may have is_latest=TRUE can i make the db enforce this?.
CREATE TABLE job_definitions (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_family_id  UUID          NOT NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    name           TEXT          NOT NULL,
    cron           TEXT          NOT NULL,
    job_type       job_type NOT NULL,
    is_latest      BOOLEAN       NOT NULL DEFAULT TRUE,
    is_parked      BOOLEAN       NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    max_retries    INTEGER       NOT NULL DEFAULT 3,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by     TEXT          NOT NULL DEFAULT 'system',
    CONSTRAINT uq_job_definitions_family_version UNIQUE (job_family_id, version)
);

-- Used by scheduler: find all active (non-parked, non-deleted, latest) jobs
CREATE INDEX idx_job_definitions_scheduler ON job_definitions (is_latest, is_parked, is_deleted);
-- Used for version history lookups and is_latest enforcement
CREATE INDEX idx_job_definitions_family_latest ON job_definitions (job_family_id, is_latest);

--changeset conductor:v002-create-job-type-http-configs
-- HTTP-specific configuration for job definitions where job_type = 'HTTP'.
-- UNIQUE on job_definition_id enforces the one-to-one relationship at the DB level.
-- Extensibility: future job types will have their own config tables
CREATE TABLE job_type_http_configs (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_definition_id  UUID          NOT NULL UNIQUE REFERENCES job_definitions (id),
    url                TEXT          NOT NULL,
    method             request_type  NOT NULL,
    payload            JSONB,
    headers            JSONB,
    timeout_seconds    INTEGER       NOT NULL DEFAULT 30
);

--changeset conductor:v002-create-job-runs
-- One row per execution attempt. Created by the scheduler when it enqueues a job.
-- Retry runs reference their origin via parent_run_id (self-referential FK).
-- job_family_id is denormalized here for analytics query performance:
-- strictly it is reachable via job_definition_id -> job_definitions.job_family_id,
-- but storing it directly avoids a join in the most frequent history query pattern.
CREATE TABLE job_runs (
    id                  UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    job_definition_id   UUID              NOT NULL REFERENCES job_definitions (id),
    job_family_id       UUID              NOT NULL,
    status              job_status   NOT NULL DEFAULT 'QUEUED',
    attempt_number      INTEGER           NOT NULL DEFAULT 1,
    parent_run_id       UUID              REFERENCES job_runs (id),
    scheduled_at        TIMESTAMPTZ       NOT NULL,
    queued_at           TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    duration_ms         INTEGER,
    sqs_message_id      TEXT
);

CREATE INDEX idx_job_runs_family_time    ON job_runs (job_family_id, scheduled_at DESC);
CREATE INDEX idx_job_runs_status         ON job_runs (status);
CREATE INDEX idx_job_runs_definition     ON job_runs (job_definition_id);
CREATE INDEX idx_job_runs_parent         ON job_runs (parent_run_id);

--changeset conductor:v002-create-job-run-events
-- Append-only event log. Every status transition appends a new row; rows are never updated.
-- Provides a full audit trail for debugging and compliance.
CREATE TABLE job_run_events (
    id               UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    job_run_id       UUID             NOT NULL REFERENCES job_runs (id),
    status           job_status  NOT NULL,
    message          TEXT,
    http_status_code INTEGER,
    response_body    TEXT,
    occurred_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    source           TEXT             NOT NULL DEFAULT 'worker'
);

CREATE INDEX idx_job_run_events_run_time ON job_run_events (job_run_id, occurred_at);

--changeset conductor:v002-create-job-schedules
-- Tracks the last time the scheduler evaluated each active job definition.
-- Prevents duplicate scheduling if the scheduler restarts mid-cycle.
-- Also provides the data needed to display the next scheduled run time in the UI.
CREATE TABLE job_schedules (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_definition_id   UUID         NOT NULL UNIQUE REFERENCES job_definitions (id),
    last_evaluated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_triggered_at   TIMESTAMPTZ,
    next_scheduled_at   TIMESTAMPTZ
);
