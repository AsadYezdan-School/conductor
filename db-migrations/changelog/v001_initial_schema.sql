--liquibase formatted sql

--changeset conductor:v001, create definition for http jobs
CREATE TYPE request_type AS ENUM ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS','HEAD');
CREATE TABLE http_jobs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT         NOT NULL,
    payload     JSONB,
    status      TEXT         NOT NULL DEFAULT 'CREATED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cron        TEXT         NOT NULL,
    url         TEXT         NOT NULL,
    method      request_type NOT NULL
);