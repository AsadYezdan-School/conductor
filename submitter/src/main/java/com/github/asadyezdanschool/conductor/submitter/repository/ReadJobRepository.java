package com.github.asadyezdanschool.conductor.submitter.repository;

import com.github.asadyezdanschool.conductor.submitter.exception.NotFoundException;
import com.github.asadyezdanschool.conductor.submitter.model.FailureModeStat;
import com.github.asadyezdanschool.conductor.submitter.model.JobDetail;
import com.github.asadyezdanschool.conductor.submitter.model.JobHealthStat;
import com.github.asadyezdanschool.conductor.submitter.model.JobRunSummary;
import com.github.asadyezdanschool.conductor.submitter.model.JobSummary;
import com.github.asadyezdanschool.conductor.submitter.model.RunEvent;
import com.github.asadyezdanschool.conductor.submitter.model.RunTrendBucket;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class ReadJobRepository {

    private static final Logger log = Logger.getLogger(ReadJobRepository.class.getName());

    private final DataSource dataSource;

    @Inject
    public ReadJobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Returns all active (non-deleted, latest) jobs with schedule info and last run status. */
    public List<JobSummary> listJobs() {
        String sql =
                "SELECT jd.job_family_id, jd.id AS job_definition_id, jd.version, jd.name, jd.cron, " +
                "  jd.job_type::text, jd.is_parked, " +
                "  js.next_scheduled_at, js.last_triggered_at, " +
                "  (SELECT jr.status::text FROM job_runs jr " +
                "   WHERE jr.job_family_id = jd.job_family_id " +
                "   ORDER BY jr.scheduled_at DESC LIMIT 1) AS latest_run_status " +
                "FROM job_definitions jd " +
                "LEFT JOIN job_schedules js ON js.job_definition_id = jd.id " +
                "WHERE jd.is_latest = TRUE AND jd.is_deleted = FALSE " +
                "ORDER BY jd.name ASC";

        List<JobSummary> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(new JobSummary(
                        rs.getObject("job_family_id").toString(),
                        rs.getObject("job_definition_id").toString(),
                        rs.getInt("version"),
                        rs.getString("name"),
                        rs.getString("cron"),
                        rs.getString("job_type"),
                        rs.getBoolean("is_parked"),
                        tsToString(rs.getTimestamp("next_scheduled_at")),
                        tsToString(rs.getTimestamp("last_triggered_at")),
                        rs.getString("latest_run_status")
                ));
            }
        } catch (SQLException e) {
            log.severe("Failed to list jobs: " + e.getMessage());
            throw new RuntimeException("Failed to list jobs", e);
        }
        return results;
    }

    /** Returns full detail for a single job family, including HTTP config. */
    public JobDetail getJob(UUID familyId) {
        String sql =
                "SELECT jd.job_family_id, jd.id AS job_definition_id, jd.version, jd.name, jd.cron, " +
                "  jd.job_type::text, jd.is_parked, jd.max_retries, jd.created_at, " +
                "  js.next_scheduled_at, js.last_triggered_at, " +
                "  c.url, c.method::text, c.payload::text, c.headers::text, c.timeout_seconds " +
                "FROM job_definitions jd " +
                "LEFT JOIN job_schedules js ON js.job_definition_id = jd.id " +
                "LEFT JOIN job_type_http_configs c ON c.job_definition_id = jd.id " +
                "WHERE jd.job_family_id = ? AND jd.is_latest = TRUE AND jd.is_deleted = FALSE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, familyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("No job found for family id: " + familyId);
                }
                return new JobDetail(
                        rs.getObject("job_family_id").toString(),
                        rs.getObject("job_definition_id").toString(),
                        rs.getInt("version"),
                        rs.getString("name"),
                        rs.getString("cron"),
                        rs.getString("job_type"),
                        rs.getBoolean("is_parked"),
                        rs.getInt("max_retries"),
                        tsToString(rs.getTimestamp("created_at")),
                        tsToString(rs.getTimestamp("next_scheduled_at")),
                        tsToString(rs.getTimestamp("last_triggered_at")),
                        rs.getString("url"),
                        rs.getString("method"),
                        rs.getString("payload"),
                        rs.getString("headers"),
                        rs.getInt("timeout_seconds")
                );
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (SQLException e) {
            log.severe("Failed to get job " + familyId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get job", e);
        }
    }

    /** Returns paginated run history for a job family, newest first. */
    public List<JobRunSummary> listRuns(UUID familyId, int limit, int offset) {
        String sql =
                "SELECT jr.id AS run_id, jr.job_family_id, jr.status::text, jr.attempt_number, " +
                "  jr.scheduled_at, jr.started_at, jr.finished_at, " +
                "  EXTRACT(EPOCH FROM (jr.finished_at - jr.started_at)) * 1000 AS duration_ms " +
                "FROM job_runs jr " +
                "WHERE jr.job_family_id = ? " +
                "ORDER BY jr.scheduled_at DESC " +
                "LIMIT ? OFFSET ?";

        List<JobRunSummary> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, familyId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long durationMs = rs.getLong("duration_ms");
                    results.add(new JobRunSummary(
                            rs.getObject("run_id").toString(),
                            rs.getObject("job_family_id").toString(),
                            rs.getString("status"),
                            rs.getInt("attempt_number"),
                            tsToString(rs.getTimestamp("scheduled_at")),
                            tsToString(rs.getTimestamp("started_at")),
                            tsToString(rs.getTimestamp("finished_at")),
                            rs.wasNull() ? null : durationMs
                    ));
                }
            }
        } catch (SQLException e) {
            log.severe("Failed to list runs for family " + familyId + ": " + e.getMessage());
            throw new RuntimeException("Failed to list runs", e);
        }
        return results;
    }

    /** Returns all audit events for a single run, oldest first. */
    public List<RunEvent> listRunEvents(UUID runId) {
        String sql =
                "SELECT jre.id AS event_id, jre.status::text, jre.message, " +
                "  jre.http_status_code, jre.response_body, jre.occurred_at, jre.source " +
                "FROM job_run_events jre " +
                "WHERE jre.job_run_id = ? " +
                "ORDER BY jre.occurred_at ASC";

        List<RunEvent> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int httpCode = rs.getInt("http_status_code");
                    results.add(new RunEvent(
                            rs.getObject("event_id").toString(),
                            rs.getString("status"),
                            rs.getString("message"),
                            rs.wasNull() || httpCode == 0 ? null : httpCode,
                            rs.getString("response_body"),
                            tsToString(rs.getTimestamp("occurred_at")),
                            rs.getString("source")
                    ));
                }
            }
        } catch (SQLException e) {
            log.severe("Failed to list events for run " + runId + ": " + e.getMessage());
            throw new RuntimeException("Failed to list run events", e);
        }
        return results;
    }

    /** Job health overview: success rate and avg duration per job over the last 7 days. */
    public List<JobHealthStat> getJobHealthStats() {
        String sql =
                "SELECT jd.name, jd.job_family_id::text, jd.cron, jd.is_parked, " +
                "  COUNT(jr.id) AS total_runs, " +
                "  COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED') AS succeeded, " +
                "  COUNT(jr.id) FILTER (WHERE jr.status = 'FAILED') AS failed, " +
                "  ROUND(100.0 * COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED') " +
                "    / NULLIF(COUNT(jr.id), 0), 1) AS success_rate_pct, " +
                "  ROUND(AVG(EXTRACT(EPOCH FROM (jr.finished_at - jr.started_at)) * 1000) FILTER (WHERE jr.status = 'SUCCEEDED'))::bigint AS avg_duration_ms " +
                "FROM job_definitions jd " +
                "LEFT JOIN job_runs jr ON jr.job_family_id = jd.job_family_id " +
                "  AND jr.scheduled_at >= NOW() - INTERVAL '7 days' " +
                "WHERE jd.is_latest = TRUE AND jd.is_deleted = FALSE " +
                "GROUP BY jd.name, jd.job_family_id, jd.cron, jd.is_parked " +
                "ORDER BY total_runs DESC";

        List<JobHealthStat> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double successRate = rs.getDouble("success_rate_pct");
                boolean successRateNull = rs.wasNull();
                long avgDuration = rs.getLong("avg_duration_ms");
                boolean avgDurationNull = rs.wasNull();
                results.add(new JobHealthStat(
                        rs.getString("name"),
                        rs.getObject("job_family_id").toString(),
                        rs.getString("cron"),
                        rs.getBoolean("is_parked"),
                        rs.getLong("total_runs"),
                        rs.getLong("succeeded"),
                        rs.getLong("failed"),
                        successRateNull ? null : successRate,
                        avgDurationNull ? null : avgDuration
                ));
            }
        } catch (SQLException e) {
            log.severe("Failed to get job health stats: " + e.getMessage());
            throw new RuntimeException("Failed to get job health stats", e);
        }
        return results;
    }

    /** Hourly run volume and status counts for the last 24 hours. */
    public List<RunTrendBucket> getRunTrend() {
        String sql =
                "SELECT DATE_TRUNC('hour', jr.scheduled_at) AS bucket, " +
                "  COUNT(*) AS total_runs, " +
                "  COUNT(*) FILTER (WHERE jr.status = 'SUCCEEDED') AS succeeded, " +
                "  COUNT(*) FILTER (WHERE jr.status = 'FAILED') AS failed, " +
                "  ROUND(AVG(EXTRACT(EPOCH FROM (jr.finished_at - jr.started_at)) * 1000))::bigint AS avg_duration_ms " +
                "FROM job_runs jr " +
                "JOIN job_definitions jd ON jr.job_definition_id = jd.id " +
                "WHERE jr.scheduled_at >= NOW() - INTERVAL '24 hours' " +
                "GROUP BY DATE_TRUNC('hour', jr.scheduled_at) " +
                "ORDER BY bucket ASC";

        List<RunTrendBucket> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long avgDuration = rs.getLong("avg_duration_ms");
                boolean avgNull = rs.wasNull();
                results.add(new RunTrendBucket(
                        tsToString(rs.getTimestamp("bucket")),
                        rs.getLong("total_runs"),
                        rs.getLong("succeeded"),
                        rs.getLong("failed"),
                        avgNull ? null : avgDuration
                ));
            }
        } catch (SQLException e) {
            log.severe("Failed to get run trend: " + e.getMessage());
            throw new RuntimeException("Failed to get run trend", e);
        }
        return results;
    }

    /** Top 20 failure modes by HTTP status code across jobs in the last 7 days. */
    public List<FailureModeStat> getFailureModes() {
        String sql =
                "SELECT jd.name, jd.job_family_id::text, jre.http_status_code, " +
                "  COUNT(*) AS occurrences, " +
                "  MAX(jre.occurred_at) AS last_seen_at " +
                "FROM job_run_events jre " +
                "JOIN job_runs jr ON jre.job_run_id = jr.id " +
                "JOIN job_definitions jd ON jr.job_definition_id = jd.id " +
                "WHERE jre.status = 'FAILED' " +
                "  AND jre.occurred_at >= NOW() - INTERVAL '7 days' " +
                "GROUP BY jd.name, jd.job_family_id, jre.http_status_code " +
                "ORDER BY occurrences DESC " +
                "LIMIT 20";

        List<FailureModeStat> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int httpCode = rs.getInt("http_status_code");
                boolean httpNull = rs.wasNull();
                results.add(new FailureModeStat(
                        rs.getString("name"),
                        rs.getObject("job_family_id").toString(),
                        httpNull ? null : httpCode,
                        rs.getLong("occurrences"),
                        tsToString(rs.getTimestamp("last_seen_at"))
                ));
            }
        } catch (SQLException e) {
            log.severe("Failed to get failure modes: " + e.getMessage());
            throw new RuntimeException("Failed to get failure modes", e);
        }
        return results;
    }

    private static String tsToString(Timestamp ts) {
        return ts != null ? ts.toInstant().toString() : null;
    }
}
