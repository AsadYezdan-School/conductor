package com.github.asadyezdanschool.conductor.scheduler.repository;

import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpHeader;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Single source of truth for all database interactions in the scheduler.
 *
 * <p>Uses raw JDBC with HikariCP pooling (one connection per operation / transaction).
 * No ORM — all SQL is written explicitly to stay consistent with the submitter pattern
 * and to keep query intent visible.
 *
 * <p>All public methods are called either from:
 * <ul>
 *   <li>gRPC service impls (job management operations)</li>
 *   <li>{@link com.github.asadyezdanschool.conductor.scheduler.service.EnqueueService} (virtual threads)</li>
 *   <li>{@link com.github.asadyezdanschool.conductor.scheduler.cache.InMemoryJobCache} (background refresh)</li>
 * </ul>
 */
@Singleton
public class JobRepository {

    private final DataSource dataSource;

    @Inject
    public JobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Cache loading ─────────────────────────────────────────────────────────

    /**
     * Load all active job definitions for cache initialisation and background refresh.
     *
     * <p>Uses {@code idx_job_definitions_scheduler} (is_latest, is_parked, is_deleted).
     * LEFT JOINs {@code job_schedules} so jobs that have never been triggered are included
     * with a null {@code last_triggered_at}.
     *
     * <p>Returns raw DB rows; callers must compute {@code nextScheduledAt} via
     * {@link com.github.asadyezdanschool.conductor.scheduler.scheduling.CronEvaluator}.
     */
    public List<ActiveJobRow> loadActiveJobs() throws SQLException {
        String sql = """
                SELECT jd.id, jd.job_family_id, jd.name, jd.cron, jd.job_type::text, jd.max_retries,
                       js.last_triggered_at, js.next_scheduled_at
                FROM job_definitions jd
                LEFT JOIN job_schedules js ON js.job_definition_id = jd.id
                WHERE jd.is_latest = TRUE AND jd.is_parked = FALSE AND jd.is_deleted = FALSE
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ActiveJobRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(rowFromResultSet(rs));
            }
            return rows;
        }
    }

    // ── Job management (called by JobManagementServiceImpl) ──────────────────

    /**
     * Insert a new job family (version 1) with its HTTP config. Transactional.
     *
     * @return the IDs and version of the newly created definition
     */
    public CreatedJobResult createJobDefinition(CreateJobParams params) throws SQLException {
        String defSql = """
                INSERT INTO job_definitions (job_family_id, version, name, cron, job_type,
                                             is_latest, is_parked, is_deleted, max_retries, created_by)
                VALUES (gen_random_uuid(), 1, ?, ?, ?::job_type, TRUE, FALSE, FALSE, 3, 'scheduler')
                RETURNING id, job_family_id, version
                """;
        String httpSql = """
                INSERT INTO job_type_http_configs (job_definition_id, url, method, payload, timeout_seconds)
                VALUES (?, ?, ?::request_type, ?::jsonb, ?)
                RETURNING id
                """;
        String headerSql = """
                INSERT INTO job_http_config_headers (http_config_id, header_name, header_value)
                VALUES (?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                UUID definitionId;
                UUID familyId;
                int version;
                try (PreparedStatement ps = c.prepareStatement(defSql)) {
                    ps.setString(1, params.name());
                    ps.setString(2, params.cron());
                    ps.setString(3, params.jobType());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        definitionId = (UUID) rs.getObject(1);
                        familyId     = (UUID) rs.getObject(2);
                        version      = rs.getInt(3);
                    }
                }
                UUID httpConfigId;
                try (PreparedStatement ps = c.prepareStatement(httpSql)) {
                    ps.setObject(1, definitionId);
                    ps.setString(2, params.url());
                    ps.setString(3, params.method());
                    ps.setObject(4, params.payload(), Types.OTHER);
                    ps.setInt(5, params.timeoutSeconds());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        httpConfigId = (UUID) rs.getObject(1);
                    }
                }
                if (params.headers() != null && !params.headers().isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(headerSql)) {
                        for (HttpHeader h : params.headers()) {
                            ps.setObject(1, httpConfigId);
                            ps.setString(2, h.name());
                            ps.setString(3, h.value());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                c.commit();
                return new CreatedJobResult(definitionId, familyId, version);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Create a new version of an existing job family with carry-forward for omitted fields.
     * Marks the previous latest version as not-latest. Transactional.
     *
     * @return the new definition ID and version
     */
    public EditedJobResult editJobDefinition(UUID familyId, EditJobParams params) throws SQLException {
        String selectSql = """
                SELECT jd.id, jd.version, jd.name, jd.cron,
                       c.id AS http_config_id, c.url, c.method::text, c.payload::text, c.timeout_seconds
                FROM job_definitions jd
                JOIN job_type_http_configs c ON c.job_definition_id = jd.id
                WHERE jd.job_family_id = ? AND jd.is_latest = TRUE AND jd.is_deleted = FALSE
                FOR UPDATE
                """;
        String markOldSql = "UPDATE job_definitions SET is_latest = FALSE WHERE id = ?";
        String insertNewSql = """
                INSERT INTO job_definitions (job_family_id, version, name, cron, job_type,
                                             is_latest, is_parked, is_deleted, max_retries, created_by)
                SELECT job_family_id, ? AS version, ? AS name, ? AS cron, job_type,
                       TRUE, is_parked, FALSE, max_retries, 'scheduler'
                FROM job_definitions WHERE id = ?
                RETURNING id, version
                """;
        String insertHttpSql = """
                INSERT INTO job_type_http_configs (job_definition_id, url, method, payload, timeout_seconds)
                VALUES (?, ?, ?::request_type, ?::jsonb, ?)
                RETURNING id
                """;
        String copyHeadersSql = """
                INSERT INTO job_http_config_headers (http_config_id, header_name, header_value)
                SELECT ?, header_name, header_value
                FROM job_http_config_headers
                WHERE http_config_id = ?
                """;
        String insertHeaderSql = """
                INSERT INTO job_http_config_headers (http_config_id, header_name, header_value)
                VALUES (?, ?, ?)
                """;
        String migrateScheduleSql = """
                INSERT INTO job_schedules (job_definition_id, last_triggered_at, next_scheduled_at)
                SELECT ?, last_triggered_at, next_scheduled_at
                FROM job_schedules WHERE job_definition_id = ?
                ON CONFLICT (job_definition_id) DO NOTHING
                """;

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Fetch current latest
                UUID   oldId;
                UUID   oldHttpConfigId;
                int    oldVersion;
                String existingName, existingCron, existingUrl, existingMethod, existingPayload;
                int    existingTimeout;
                try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                    ps.setObject(1, familyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new NotFoundException("No active job for family: " + familyId);
                        }
                        oldId           = (UUID) rs.getObject(1);
                        oldVersion      = rs.getInt(2);
                        existingName    = rs.getString(3);
                        existingCron    = rs.getString(4);
                        oldHttpConfigId = (UUID) rs.getObject(5);
                        existingUrl     = rs.getString(6);
                        existingMethod  = rs.getString(7);
                        existingPayload = rs.getString(8);
                        existingTimeout = rs.getInt(9);
                    }
                }

                // Mark old as not latest
                try (PreparedStatement ps = c.prepareStatement(markOldSql)) {
                    ps.setObject(1, oldId);
                    ps.executeUpdate();
                }

                // Insert new version with carry-forward
                String newName    = params.name()           != null ? params.name()           : existingName;
                String newCron    = params.cron()           != null ? params.cron()           : existingCron;
                String newUrl     = params.url()            != null ? params.url()            : existingUrl;
                String newMethod  = params.method()         != null ? params.method()         : existingMethod;
                String newPayload = params.payload()        != null ? params.payload()        : existingPayload;
                int    newTimeout = params.timeoutSeconds() != null ? params.timeoutSeconds() : existingTimeout;
                int    newVersion = oldVersion + 1;

                UUID newId;
                try (PreparedStatement ps = c.prepareStatement(insertNewSql)) {
                    ps.setInt(1, newVersion);
                    ps.setString(2, newName);
                    ps.setString(3, newCron);
                    ps.setObject(4, oldId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        newId = (UUID) rs.getObject(1);
                    }
                }

                // Insert new HTTP config
                UUID newHttpConfigId;
                try (PreparedStatement ps = c.prepareStatement(insertHttpSql)) {
                    ps.setObject(1, newId);
                    ps.setString(2, newUrl);
                    ps.setString(3, newMethod);
                    ps.setObject(4, newPayload, Types.OTHER);
                    ps.setInt(5, newTimeout);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        newHttpConfigId = (UUID) rs.getObject(1);
                    }
                }

                // Headers: carry forward from old config if not provided, else insert new rows
                if (params.headers() == null || params.headers().isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(copyHeadersSql)) {
                        ps.setObject(1, newHttpConfigId);
                        ps.setObject(2, oldHttpConfigId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(insertHeaderSql)) {
                        for (HttpHeader h : params.headers()) {
                            ps.setObject(1, newHttpConfigId);
                            ps.setString(2, h.name());
                            ps.setString(3, h.value());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                // Migrate job_schedules row to new definition ID
                try (PreparedStatement ps = c.prepareStatement(migrateScheduleSql)) {
                    ps.setObject(1, newId);
                    ps.setObject(2, oldId);
                    ps.executeUpdate();
                }

                c.commit();
                return new EditedJobResult(newId, newVersion);
            } catch (SQLException | NotFoundException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Update {@code is_parked} for the latest, non-deleted version of a job family.
     *
     * @throws NotFoundException if no active job exists for the family
     * @throws ConflictException if the job is already in the requested park state
     */
    public void setParkStatus(UUID familyId, boolean park) throws SQLException {
        String selectSql = """
                SELECT id, is_parked FROM job_definitions
                WHERE job_family_id = ? AND is_latest = TRUE AND is_deleted = FALSE
                FOR UPDATE
                """;
        String updateSql = "UPDATE job_definitions SET is_parked = ? WHERE id = ?";

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                UUID currentId;
                boolean currentParked;
                try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                    ps.setObject(1, familyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new NotFoundException("No active job for family: " + familyId);
                        }
                        currentId     = (UUID) rs.getObject(1);
                        currentParked = rs.getBoolean(2);
                    }
                }
                if (currentParked == park) {
                    throw new ConflictException("Job family " + familyId + " is already " +
                            (park ? "parked" : "unparked"));
                }
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setBoolean(1, park);
                    ps.setObject(2, currentId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | NotFoundException | ConflictException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Fetch the latest definition + schedule row needed to (re)populate the cache
     * after an unpark or edit operation.
     */
    public Optional<ActiveJobRow> fetchActiveJob(UUID familyId) throws SQLException {
        String sql = """
                SELECT jd.id, jd.job_family_id, jd.name, jd.cron, jd.job_type::text, jd.max_retries,
                       js.last_triggered_at, js.next_scheduled_at
                FROM job_definitions jd
                LEFT JOIN job_schedules js ON js.job_definition_id = jd.id
                WHERE jd.job_family_id = ? AND jd.is_latest = TRUE
                      AND jd.is_parked = FALSE AND jd.is_deleted = FALSE
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, familyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rowFromResultSet(rs));
            }
        }
    }

    // ── Enqueue (called by EnqueueService, one virtual thread per invocation) ─

    /**
     * Persist a new queued run. Three writes in a single transaction:
     * <ol>
     *   <li>INSERT INTO job_runs → returns runId</li>
     *   <li>INSERT INTO job_run_events (status=QUEUED, source='scheduler')</li>
     *   <li>UPSERT job_schedules (last_triggered_at, next_scheduled_at)</li>
     * </ol>
     *
     * @return the generated job run UUID
     */
    public UUID enqueueRun(UUID definitionId, UUID familyId,
                           Instant scheduledAt, Instant nextScheduledAt) throws SQLException {
        String runSql = """
                INSERT INTO job_runs (job_definition_id, job_family_id, status, scheduled_at)
                VALUES (?, ?, 'QUEUED'::job_status, ?)
                RETURNING id
                """;
        String eventSql = """
                INSERT INTO job_run_events (job_run_id, status, source)
                VALUES (?, 'QUEUED'::job_status, 'scheduler')
                """;
        String scheduleSql = """
                INSERT INTO job_schedules (job_definition_id, last_triggered_at, next_scheduled_at)
                VALUES (?, ?, ?)
                ON CONFLICT (job_definition_id) DO UPDATE
                  SET last_triggered_at = EXCLUDED.last_triggered_at,
                      next_scheduled_at = EXCLUDED.next_scheduled_at,
                      last_evaluated_at = NOW()
                """;

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                UUID runId;
                try (PreparedStatement ps = c.prepareStatement(runSql)) {
                    ps.setObject(1, definitionId);
                    ps.setObject(2, familyId);
                    ps.setTimestamp(3, Timestamp.from(scheduledAt));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        runId = (UUID) rs.getObject(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, runId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(scheduleSql)) {
                    ps.setObject(1, definitionId);
                    ps.setTimestamp(2, Timestamp.from(scheduledAt));
                    ps.setTimestamp(3, Timestamp.from(nextScheduledAt));
                    ps.executeUpdate();
                }
                c.commit();
                return runId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    // ── Execution (called by JobExecutionServiceImpl) ────────────────────────

    /**
     * Fetch the HTTP config for a run by joining job_runs → job_definitions → job_type_http_configs.
     * Also returns attempt_number and max_retries so the worker can log and the scheduler can
     * decide whether to enqueue a retry on failure.
     */
    public Optional<HttpRunDetails> getHttpRunDetails(UUID jobRunId) throws SQLException {
        String sql = """
                SELECT jr.id, jd.id, jr.job_family_id, c.url, c.method::text, c.payload::text,
                       c.timeout_seconds, jr.attempt_number, jd.max_retries,
                       h.header_name, h.header_value
                FROM job_runs jr
                JOIN job_definitions jd         ON jd.id = jr.job_definition_id
                JOIN job_type_http_configs c     ON c.job_definition_id = jd.id
                LEFT JOIN job_http_config_headers h ON h.http_config_id = c.id
                WHERE jr.id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, jobRunId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                UUID   runId        = (UUID) rs.getObject(1);
                UUID   definitionId = (UUID) rs.getObject(2);
                UUID   familyId     = (UUID) rs.getObject(3);
                String url          = rs.getString(4);
                String method       = rs.getString(5);
                String payload      = rs.getString(6);
                int    timeout      = rs.getInt(7);
                int    attempt      = rs.getInt(8);
                int    maxRetries   = rs.getInt(9);
                List<HttpHeader> headers = new ArrayList<>();
                do {
                    String name  = rs.getString(10);
                    String value = rs.getString(11);
                    if (name != null) headers.add(new HttpHeader(name, value));
                } while (rs.next());
                return Optional.of(new HttpRunDetails(
                        runId, definitionId, familyId,
                        url, method, payload,
                        Collections.unmodifiableList(headers),
                        timeout, attempt, maxRetries
                ));
            }
        }
    }

    /**
     * Insert a new retry run row and a QUEUED event for it. Does NOT update job_schedules —
     * that was already done when the original run was enqueued.
     *
     * @param failedRunId   the ID of the run that just failed (becomes parent_run_id)
     * @param definitionId  job definition to execute
     * @param familyId      job family
     * @param attemptNumber the new attempt number (failed attempt + 1)
     * @return the new run's UUID
     */
    public UUID enqueueRetry(UUID failedRunId, UUID definitionId, UUID familyId,
                             int attemptNumber) throws SQLException {
        String runSql = """
                INSERT INTO job_runs (job_definition_id, job_family_id, status, scheduled_at,
                                      attempt_number, parent_run_id)
                VALUES (?, ?, 'QUEUED'::job_status, NOW(), ?, ?)
                RETURNING id
                """;
        String eventSql = """
                INSERT INTO job_run_events (job_run_id, status, source)
                VALUES (?, 'QUEUED'::job_status, 'scheduler')
                """;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                UUID retryRunId;
                try (PreparedStatement ps = c.prepareStatement(runSql)) {
                    ps.setObject(1, definitionId);
                    ps.setObject(2, familyId);
                    ps.setInt(3, attemptNumber);
                    ps.setObject(4, failedRunId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        retryRunId = (UUID) rs.getObject(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, retryRunId);
                    ps.executeUpdate();
                }
                c.commit();
                return retryRunId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Update run status to RUNNING: set status + started_at.
     * Also inserts a job_run_events row. Transactional.
     */
    public void markRunning(UUID jobRunId) throws SQLException {
        String updateSql = "UPDATE job_runs SET status = 'RUNNING'::job_status, started_at = NOW() WHERE id = ?";
        String eventSql  = "INSERT INTO job_run_events (job_run_id, status, source) VALUES (?, 'RUNNING'::job_status, 'worker')";
        runStatusTransaction(jobRunId, updateSql, eventSql, null, null, null, 0);
    }

    /**
     * Update run status to SUCCEEDED: set status, finished_at.
     * Inserts a job_run_events row with http_status_code and response_body. Transactional.
     */
    public void markSucceeded(UUID jobRunId,
                              int httpStatusCode, String responseBody) throws SQLException {
        String updateSql = "UPDATE job_runs SET status = 'SUCCEEDED'::job_status, finished_at = NOW() WHERE id = ?";
        String eventSql  = """
                INSERT INTO job_run_events (job_run_id, status, http_status_code, response_body, source)
                VALUES (?, 'SUCCEEDED'::job_status, ?, ?, 'worker')
                """;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setObject(1, jobRunId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, jobRunId);
                    ps.setInt(2, httpStatusCode);
                    ps.setString(3, responseBody);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Update run status to FAILED: set status, finished_at.
     * Inserts a job_run_events row with message / http_status_code / response_body. Transactional.
     */
    public void markFailed(UUID jobRunId,
                           String message, int httpStatusCode, String responseBody) throws SQLException {
        String updateSql = "UPDATE job_runs SET status = 'FAILED'::job_status, finished_at = NOW() WHERE id = ?";
        String eventSql  = """
                INSERT INTO job_run_events (job_run_id, status, message, http_status_code, response_body, source)
                VALUES (?, 'FAILED'::job_status, ?, ?, ?, 'worker')
                """;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setObject(1, jobRunId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, jobRunId);
                    ps.setString(2, message);
                    ps.setInt(3, httpStatusCode);
                    ps.setString(4, responseBody);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    // ── Dependency gating ─────────────────────────────────────────────────────

    /**
     * Returns {@code true} if every upstream dependency of the given downstream job
     * has a most-recent terminal run (SUCCEEDED/FAILED/CANCELLED) with status SUCCEEDED,
     * or if the job has no upstream dependencies at all.
     *
     * <p>Returns {@code false} if any upstream:
     * <ul>
     *   <li>has never produced a terminal run, or</li>
     *   <li>whose last terminal run was FAILED or CANCELLED.</li>
     * </ul>
     */
    public boolean areAllUpstreamDepsSucceeded(UUID downstreamFamilyId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS blocking_count
                FROM job_dependencies d
                LEFT JOIN LATERAL (
                    SELECT status::text AS last_status
                    FROM job_runs
                    WHERE job_family_id = d.upstream_family_id
                      AND status IN ('SUCCEEDED', 'FAILED', 'CANCELLED')
                    ORDER BY finished_at DESC NULLS LAST, scheduled_at DESC NULLS LAST
                    LIMIT 1
                ) latest ON TRUE
                WHERE d.downstream_family_id = ?
                  AND (latest.last_status IS NULL OR latest.last_status != 'SUCCEEDED')
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, downstreamFamilyId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("blocking_count") == 0;
            }
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private ActiveJobRow rowFromResultSet(ResultSet rs) throws SQLException {
        Timestamp lastTriggered   = rs.getTimestamp(7);
        Timestamp nextScheduled   = rs.getTimestamp(8);
        return new ActiveJobRow(
                (UUID) rs.getObject(1),
                (UUID) rs.getObject(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getInt(6),
                lastTriggered  != null ? lastTriggered.toInstant()  : null,
                nextScheduled  != null ? nextScheduled.toInstant()  : null
        );
    }

    private void runStatusTransaction(UUID jobRunId, String updateSql, String eventSql,
                                      String message, String responseBody,
                                      Integer httpStatusCode, long durationMs) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setObject(1, jobRunId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, jobRunId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void runStatusTransactionWithDuration(UUID jobRunId, String updateSql,
                                                   String eventSql, long durationMs) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setLong(1, durationMs);
                    ps.setObject(2, jobRunId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(eventSql)) {
                    ps.setObject(1, jobRunId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    // ── Inner result / param types ────────────────────────────────────────────

    /** Raw DB row returned by loadActiveJobs / fetchActiveJob. */
    public record ActiveJobRow(
            UUID    id,
            UUID    familyId,
            String  name,
            String  cron,
            String  jobType,
            int     maxRetries,
            Instant lastTriggeredAt,   // null if never triggered
            Instant nextScheduledAt    // null if never computed
    ) {}

    public record CreateJobParams(
            String           name,
            String           cron,
            String           jobType,
            // HTTP config fields
            String           url,
            String           method,
            String           payload,
            List<HttpHeader> headers,
            int              timeoutSeconds
    ) {}

    public record EditJobParams(
            String           name,           // null = carry forward
            String           cron,           // null = carry forward
            String           url,            // null = carry forward
            String           method,         // null = carry forward
            String           payload,        // null = carry forward
            List<HttpHeader> headers,        // null or empty = carry forward
            Integer          timeoutSeconds  // null = carry forward
    ) {}

    public record CreatedJobResult(UUID definitionId, UUID familyId, int version) {}

    public record EditedJobResult(UUID newDefinitionId, int newVersion) {}

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }
}
