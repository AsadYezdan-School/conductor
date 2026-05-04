CREATE TABLE job_family_alert_configs (
    job_family_id        UUID        PRIMARY KEY,
    min_success_rate_pct NUMERIC(5,2),
    max_avg_duration_ms  INTEGER,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- No FK on job_family_id: job_definitions has no UNIQUE constraint on that column.
-- Orphan rows for deleted job families are harmless.

CREATE TABLE job_dependencies (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    upstream_family_id   UUID        NOT NULL,
    downstream_family_id UUID        NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_dep  UNIQUE (upstream_family_id, downstream_family_id),
    CONSTRAINT no_self CHECK  (upstream_family_id != downstream_family_id)
);

CREATE INDEX idx_dep_upstream   ON job_dependencies (upstream_family_id);
CREATE INDEX idx_dep_downstream ON job_dependencies (downstream_family_id);
