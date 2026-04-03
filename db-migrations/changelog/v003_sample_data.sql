--liquibase formatted sql

--changeset conductor:v003-sample-job-definitions
-- 5 job definitions across 4 families.
-- The 'daily-report-exporter' family has 2 versions to demonstrate immutable versioning:
--   v1 has is_latest=FALSE (superseded), v2 has is_latest=TRUE (active).
-- 'billing-cycle-trigger' is parked: defined but intentionally not scheduled by the scheduler.

INSERT INTO job_definitions (id, job_family_id, version, name, cron, job_type, is_latest, is_parked, is_deleted, max_retries, created_by) VALUES
    -- Family 1: daily-report-exporter, v1 (superseded)
    ('a1000000-0000-0000-0000-000000000001',
     'f1000000-0000-0000-0000-000000000001',
     1, 'daily-report-exporter', '0 6 * * *', 'HTTP', FALSE, FALSE, FALSE, 3, 'admin'),

    -- Family 1: daily-report-exporter, v2 (active)
    ('a1000000-0000-0000-0000-000000000002',
     'f1000000-0000-0000-0000-000000000001',
     2, 'daily-report-exporter', '0 6 * * *', 'HTTP', TRUE, FALSE, FALSE, 3, 'admin'),

    -- Family 2: health-check-ping, v1 (active)
    ('a2000000-0000-0000-0000-000000000001',
     'f2000000-0000-0000-0000-000000000002',
     1, 'health-check-ping', '*/5 * * * *', 'HTTP', TRUE, FALSE, FALSE, 1, 'system'),

    -- Family 3: billing-cycle-trigger, v1 (active but parked)
    ('a3000000-0000-0000-0000-000000000001',
     'f3000000-0000-0000-0000-000000000003',
     1, 'billing-cycle-trigger', '0 0 1 * *', 'HTTP', TRUE, TRUE, FALSE, 5, 'admin'),

    -- Family 4: stale-session-cleanup, v1 (active)
    ('a4000000-0000-0000-0000-000000000001',
     'f4000000-0000-0000-0000-000000000004',
     1, 'stale-session-cleanup', '0 2 * * 0', 'HTTP', TRUE, FALSE, FALSE, 3, 'system');

--changeset conductor:v003-sample-http-configs
-- HTTP config for each job definition (one-to-one).
-- v1 of daily-report-exporter uses an older endpoint; v2 uses the updated /v2 path
-- (this is the reason a new version was created).

INSERT INTO job_type_http_configs (id, job_definition_id, url, method, payload, headers, timeout_seconds) VALUES
    -- daily-report-exporter v1 (legacy endpoint)
    ('c1000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001',
     'https://api.internal/reports/export',
     'POST',
     '{"format": "pdf", "recipients": ["ops@example.com"]}',
     '{"Authorization": "Bearer legacy-token", "Content-Type": "application/json"}',
     60),

    -- daily-report-exporter v2 (updated endpoint and auth header)
    ('c1000000-0000-0000-0000-000000000002',
     'a1000000-0000-0000-0000-000000000002',
     'https://api.internal/reports/v2/export',
     'POST',
     '{"format": "pdf", "recipients": ["ops@example.com"], "include_charts": true}',
     '{"Authorization": "Bearer v2-token", "Content-Type": "application/json"}',
     60),

    -- health-check-ping
    ('c2000000-0000-0000-0000-000000000001',
     'a2000000-0000-0000-0000-000000000001',
     'https://api.internal/health',
     'GET',
     NULL,
     NULL,
     10),

    -- billing-cycle-trigger
    ('c3000000-0000-0000-0000-000000000001',
     'a3000000-0000-0000-0000-000000000001',
     'https://billing.internal/cycles/trigger',
     'POST',
     '{"cycle_type": "monthly"}',
     '{"Authorization": "Bearer billing-service-token", "Content-Type": "application/json"}',
     120),

    -- stale-session-cleanup
    ('c4000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000001',
     'https://auth.internal/sessions/stale',
     'DELETE',
     NULL,
     '{"Authorization": "Bearer auth-service-token"}',
     30);

--changeset conductor:v003-sample-job-runs
-- 6 job runs demonstrating various statuses and a retry chain.
-- Run r3 FAILED; run r4 is the retry of r3 (parent_run_id set, attempt_number=2).

INSERT INTO job_runs (id, job_definition_id, job_family_id, status, attempt_number, parent_run_id, scheduled_at, queued_at, started_at, finished_at, duration_ms, sqs_message_id) VALUES
    -- daily-report-exporter v2: SUCCEEDED
    ('b1000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000002',
     'f1000000-0000-0000-0000-000000000001',
     'SUCCEEDED', 1, NULL,
     NOW() - INTERVAL '2 days' + INTERVAL '6 hours',
     NOW() - INTERVAL '2 days' + INTERVAL '6 hours',
     NOW() - INTERVAL '2 days' + INTERVAL '6 hours 1 second',
     NOW() - INTERVAL '2 days' + INTERVAL '6 hours 3 seconds',
     2340, 'sqs-msg-001'),

    -- daily-report-exporter v2: SUCCEEDED
    ('b1000000-0000-0000-0000-000000000002',
     'a1000000-0000-0000-0000-000000000002',
     'f1000000-0000-0000-0000-000000000001',
     'SUCCEEDED', 1, NULL,
     NOW() - INTERVAL '1 day' + INTERVAL '6 hours',
     NOW() - INTERVAL '1 day' + INTERVAL '6 hours',
     NOW() - INTERVAL '1 day' + INTERVAL '6 hours 1 second',
     NOW() - INTERVAL '1 day' + INTERVAL '6 hours 4 seconds',
     3102, 'sqs-msg-002'),

    -- health-check-ping: FAILED (original attempt)
    ('b2000000-0000-0000-0000-000000000001',
     'a2000000-0000-0000-0000-000000000001',
     'f2000000-0000-0000-0000-000000000002',
     'FAILED', 1, NULL,
     NOW() - INTERVAL '30 minutes',
     NOW() - INTERVAL '30 minutes',
     NOW() - INTERVAL '29 minutes 58 seconds',
     NOW() - INTERVAL '29 minutes 49 seconds',
     9100, 'sqs-msg-003'),

    -- health-check-ping: RETRYING (retry of r3, attempt 2)
    ('b2000000-0000-0000-0000-000000000002',
     'a2000000-0000-0000-0000-000000000001',
     'f2000000-0000-0000-0000-000000000002',
     'SUCCEEDED', 2, 'b2000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '29 minutes',
     NOW() - INTERVAL '29 minutes',
     NOW() - INTERVAL '28 minutes 58 seconds',
     NOW() - INTERVAL '28 minutes 55 seconds',
     3200, 'sqs-msg-004'),

    -- stale-session-cleanup: SUCCEEDED
    ('b4000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000001',
     'f4000000-0000-0000-0000-000000000004',
     'SUCCEEDED', 1, NULL,
     NOW() - INTERVAL '6 days' + INTERVAL '2 hours',
     NOW() - INTERVAL '6 days' + INTERVAL '2 hours',
     NOW() - INTERVAL '6 days' + INTERVAL '2 hours 2 seconds',
     NOW() - INTERVAL '6 days' + INTERVAL '2 hours 14 seconds',
     12500, 'sqs-msg-005'),

    -- stale-session-cleanup: FAILED
    ('b4000000-0000-0000-0000-000000000002',
     'a4000000-0000-0000-0000-000000000001',
     'f4000000-0000-0000-0000-000000000004',
     'FAILED', 1, NULL,
     NOW() - INTERVAL '13 days' + INTERVAL '2 hours',
     NOW() - INTERVAL '13 days' + INTERVAL '2 hours',
     NOW() - INTERVAL '13 days' + INTERVAL '2 hours 2 seconds',
     NOW() - INTERVAL '13 days' + INTERVAL '2 hours 33 seconds',
     31000, 'sqs-msg-006');

--changeset conductor:v003-sample-job-run-events
-- Status transition events for each run above.
-- Each run has at minimum a QUEUED event and a terminal event.
-- The failed health-check run (b2...001) also has a RUNNING event before FAILED.

INSERT INTO job_run_events (id, job_run_id, status, message, http_status_code, response_body, occurred_at, source) VALUES
    -- Run b1...001 (daily-report-exporter, SUCCEEDED)
    ('e1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'QUEUED',    'Job enqueued by scheduler', NULL, NULL, NOW() - INTERVAL '2 days' + INTERVAL '6 hours',              'scheduler'),
    ('e1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 'RUNNING',   'Worker started execution',  NULL, NULL, NOW() - INTERVAL '2 days' + INTERVAL '6 hours 1 second',    'worker'),
    ('e1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000001', 'SUCCEEDED', NULL,                        200, '{"status":"ok","report_id":"rpt-20240101"}', NOW() - INTERVAL '2 days' + INTERVAL '6 hours 3 seconds', 'worker'),

    -- Run b1...002 (daily-report-exporter, SUCCEEDED)
    ('e1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000002', 'QUEUED',    'Job enqueued by scheduler', NULL, NULL, NOW() - INTERVAL '1 day' + INTERVAL '6 hours',               'scheduler'),
    ('e1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'RUNNING',   'Worker started execution',  NULL, NULL, NOW() - INTERVAL '1 day' + INTERVAL '6 hours 1 second',     'worker'),
    ('e1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000002', 'SUCCEEDED', NULL,                        200, '{"status":"ok","report_id":"rpt-20240102"}', NOW() - INTERVAL '1 day' + INTERVAL '6 hours 4 seconds',  'worker'),

    -- Run b2...001 (health-check-ping, FAILED)
    ('e2000000-0000-0000-0000-000000000001', 'b2000000-0000-0000-0000-000000000001', 'QUEUED',    'Job enqueued by scheduler', NULL, NULL, NOW() - INTERVAL '30 minutes',               'scheduler'),
    ('e2000000-0000-0000-0000-000000000002', 'b2000000-0000-0000-0000-000000000001', 'RUNNING',   'Worker started execution',  NULL, NULL, NOW() - INTERVAL '29 minutes 58 seconds',    'worker'),
    ('e2000000-0000-0000-0000-000000000003', 'b2000000-0000-0000-0000-000000000001', 'FAILED',    'Target returned 503',       503, '{"error":"service unavailable"}', NOW() - INTERVAL '29 minutes 49 seconds', 'worker'),

    -- Run b2...002 (health-check-ping retry, SUCCEEDED)
    ('e2000000-0000-0000-0000-000000000004', 'b2000000-0000-0000-0000-000000000002', 'QUEUED',    'Retry enqueued after failure', NULL, NULL, NOW() - INTERVAL '29 minutes',              'worker'),
    ('e2000000-0000-0000-0000-000000000005', 'b2000000-0000-0000-0000-000000000002', 'RUNNING',   'Worker started execution',     NULL, NULL, NOW() - INTERVAL '28 minutes 58 seconds',  'worker'),
    ('e2000000-0000-0000-0000-000000000006', 'b2000000-0000-0000-0000-000000000002', 'SUCCEEDED', NULL,                            200, '{"status":"healthy"}', NOW() - INTERVAL '28 minutes 55 seconds', 'worker'),

    -- Run b4...001 (stale-session-cleanup, SUCCEEDED)
    ('e4000000-0000-0000-0000-000000000001', 'b4000000-0000-0000-0000-000000000001', 'QUEUED',    'Job enqueued by scheduler', NULL, NULL, NOW() - INTERVAL '6 days' + INTERVAL '2 hours',              'scheduler'),
    ('e4000000-0000-0000-0000-000000000002', 'b4000000-0000-0000-0000-000000000001', 'RUNNING',   'Worker started execution',  NULL, NULL, NOW() - INTERVAL '6 days' + INTERVAL '2 hours 2 seconds',   'worker'),
    ('e4000000-0000-0000-0000-000000000003', 'b4000000-0000-0000-0000-000000000001', 'SUCCEEDED', NULL,                         204, NULL, NOW() - INTERVAL '6 days' + INTERVAL '2 hours 14 seconds',  'worker'),

    -- Run b4...002 (stale-session-cleanup, FAILED)
    ('e4000000-0000-0000-0000-000000000004', 'b4000000-0000-0000-0000-000000000002', 'QUEUED',    'Job enqueued by scheduler', NULL, NULL, NOW() - INTERVAL '13 days' + INTERVAL '2 hours',             'scheduler'),
    ('e4000000-0000-0000-0000-000000000005', 'b4000000-0000-0000-0000-000000000002', 'RUNNING',   'Worker started execution',  NULL, NULL, NOW() - INTERVAL '13 days' + INTERVAL '2 hours 2 seconds',  'worker'),
    ('e4000000-0000-0000-0000-000000000006', 'b4000000-0000-0000-0000-000000000002', 'FAILED',    'Connection timeout',        NULL, NULL, NOW() - INTERVAL '13 days' + INTERVAL '2 hours 33 seconds', 'worker');

--changeset conductor:v003-sample-job-schedules
-- One row per is_latest=TRUE job definition (billing-cycle-trigger is parked but still tracked).

INSERT INTO job_schedules (id, job_definition_id, last_evaluated_at, last_triggered_at, next_scheduled_at) VALUES
    -- daily-report-exporter v2
    ('s1000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000002',
     NOW() - INTERVAL '1 minute',
     NOW() - INTERVAL '1 day' + INTERVAL '6 hours',
     (NOW()::DATE + INTERVAL '1 day' + INTERVAL '6 hours')),

    -- health-check-ping
    ('s2000000-0000-0000-0000-000000000001',
     'a2000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '1 minute',
     NOW() - INTERVAL '28 minutes 55 seconds',
     NOW() + INTERVAL '4 minutes'),

    -- billing-cycle-trigger (parked — last_triggered_at is NULL, never fired)
    ('s3000000-0000-0000-0000-000000000001',
     'a3000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '1 minute',
     NULL,
     (DATE_TRUNC('month', NOW()) + INTERVAL '1 month')),

    -- stale-session-cleanup
    ('s4000000-0000-0000-0000-000000000001',
     'a4000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '1 minute',
     NOW() - INTERVAL '6 days' + INTERVAL '2 hours',
     (DATE_TRUNC('week', NOW()) + INTERVAL '1 week' + INTERVAL '2 hours'));
