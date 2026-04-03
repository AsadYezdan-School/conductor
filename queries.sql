-- Conductor: Analytical SQL Queries
-- These queries draw on data from multiple tables to support data-driven decision-making.

-- ============================================================================
-- Query 1: Job Success Rate by Family
-- Decision support: Which jobs are unreliable and need attention?
-- Tables: job_definitions, job_runs
-- ============================================================================
SELECT
    jd.job_family_id,
    MAX(jd.name)                                                          AS job_name,
    COUNT(jr.id)                                                          AS total_runs,
    COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED')                  AS succeeded,
    COUNT(jr.id) FILTER (WHERE jr.status = 'FAILED')                     AS failed,
    COUNT(jr.id) FILTER (WHERE jr.parent_run_id IS NOT NULL)             AS retries,
    ROUND(
        100.0 * COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED')
        / NULLIF(COUNT(jr.id) FILTER (WHERE jr.parent_run_id IS NULL), 0),
        2
    )                                                                     AS success_rate_pct,
    ROUND(AVG(jr.duration_ms) FILTER (WHERE jr.status = 'SUCCEEDED'))    AS avg_duration_ms
FROM job_definitions jd
JOIN job_runs jr ON jr.job_family_id = jd.job_family_id
WHERE jd.is_latest = TRUE
GROUP BY jd.job_family_id
ORDER BY success_rate_pct ASC NULLS LAST;


-- ============================================================================
-- Query 2: HTTP Response Time and Status Code Distribution (last 7 days)
-- Decision support: Which downstream services are slow or unhealthy?
-- Tables: job_definitions, job_runs, job_run_events
-- ============================================================================
SELECT
    jd.name                                                               AS job_name,
    jre.http_status_code,
    COUNT(*)                                                              AS occurrences,
    ROUND(AVG(jr.duration_ms))                                           AS avg_duration_ms,
    ROUND(
        PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY jr.duration_ms)
    )                                                                     AS p95_duration_ms,
    MAX(jr.duration_ms)                                                   AS max_duration_ms
FROM job_definitions jd
JOIN job_runs jr         ON jr.job_definition_id = jd.id
JOIN job_run_events jre  ON jre.job_run_id = jr.id
                        AND jre.http_status_code IS NOT NULL
WHERE jd.is_latest = TRUE
  AND jr.finished_at >= NOW() - INTERVAL '7 days'
GROUP BY jd.name, jre.http_status_code
ORDER BY jd.name, occurrences DESC;


-- ============================================================================
-- Query 3: Retry Depth and Compute Cost by Job
-- Decision support: Which jobs are thrashing (repeated retries) and consuming
-- disproportionate worker capacity?
-- Tables: job_definitions, job_runs
-- ============================================================================
SELECT
    jd.name,
    jd.job_family_id,
    COUNT(DISTINCT jr.id) FILTER (WHERE jr.parent_run_id IS NULL)        AS original_runs,
    COUNT(DISTINCT jr.id) FILTER (WHERE jr.parent_run_id IS NOT NULL)    AS retry_runs,
    ROUND(
        100.0 * COUNT(DISTINCT jr.id) FILTER (WHERE jr.parent_run_id IS NOT NULL)
        / NULLIF(COUNT(DISTINCT jr.id), 0),
        2
    )                                                                     AS retry_rate_pct,
    SUM(jr.duration_ms)                                                   AS total_compute_ms,
    ROUND(AVG(jr.attempt_number))                                         AS avg_attempts,
    MAX(jr.attempt_number)                                                AS max_retry_depth
FROM job_definitions jd
JOIN job_runs jr ON jr.job_family_id = jd.job_family_id
WHERE jd.is_latest = TRUE
GROUP BY jd.name, jd.job_family_id
ORDER BY retry_runs DESC, total_compute_ms DESC;
