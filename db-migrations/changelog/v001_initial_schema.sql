--liquibase formatted sql

--changeset conductor:v001_initial_schema
CREATE TABLE IF NOT EXISTS jobs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    payload     JSONB,
    status      TEXT        NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE IF EXISTS jobs;
