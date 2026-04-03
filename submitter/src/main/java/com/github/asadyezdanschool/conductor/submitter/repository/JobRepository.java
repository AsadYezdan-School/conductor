package com.github.asadyezdanschool.conductor.submitter.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.asadyezdanschool.conductor.submitter.exception.NotFoundException;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class JobRepository {

    private static final Logger log = Logger.getLogger(JobRepository.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DataSource dataSource;

    @Inject
    public JobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CreateJobResponse createJob(CreateJobRequest req) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                UUID jobDefinitionId;
                UUID jobFamilyId;
                int version;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO job_definitions " +
                        "(job_family_id, version, name, cron, job_type, is_latest, is_parked, is_deleted, max_retries, created_by) " +
                        "VALUES (gen_random_uuid(), 1, ?, ?, 'HTTP'::job_type, TRUE, FALSE, FALSE, 3, 'submitter') " +
                        "RETURNING id, job_family_id, version")) {
                    ps.setString(1, req.name());
                    ps.setString(2, req.cron());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        jobDefinitionId = (UUID) rs.getObject("id");
                        jobFamilyId = (UUID) rs.getObject("job_family_id");
                        version = rs.getInt("version");
                    }
                }

                insertHttpConfig(conn, jobDefinitionId, req.url(), req.method(),
                        req.payload(), req.headers(), req.timeoutSeconds());

                conn.commit();
                log.info("Created job definition " + jobDefinitionId + " (family=" + jobFamilyId + ")");
                return new CreateJobResponse(jobFamilyId, jobDefinitionId, version);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public CreateJobResponse editJob(UUID familyId, EditJobRequest req) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Read current latest version with a row-level lock
                UUID currentId;
                int currentVersion;
                String currentName, currentCron, currentUrl, currentMethod;
                Integer currentTimeout;
                String currentPayloadJson, currentHeadersJson;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT jd.id, jd.version, jd.name, jd.cron, " +
                        "  c.url, c.method, c.payload::text, c.headers::text, c.timeout_seconds " +
                        "FROM job_definitions jd " +
                        "JOIN job_type_http_configs c ON c.job_definition_id = jd.id " +
                        "WHERE jd.job_family_id = ? AND jd.is_latest = TRUE AND jd.is_deleted = FALSE " +
                        "FOR UPDATE")) {
                    ps.setObject(1, familyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new NotFoundException("No job found for family id: " + familyId);
                        }
                        currentId = (UUID) rs.getObject("id");
                        currentVersion = rs.getInt("version");
                        currentName = rs.getString("name");
                        currentCron = rs.getString("cron");
                        currentUrl = rs.getString("url");
                        currentMethod = rs.getString("method");
                        currentTimeout = rs.getInt("timeout_seconds");
                        currentPayloadJson = rs.getString("payload");
                        currentHeadersJson = rs.getString("headers");
                    }
                }

                // Mark current as not latest
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE job_definitions SET is_latest = FALSE WHERE id = ?")) {
                    ps.setObject(1, currentId);
                    ps.executeUpdate();
                }

                // Resolve new values (carry-forward if not provided in request)
                String newName = req.name() != null ? req.name() : currentName;
                String newCron = req.cron() != null ? req.cron() : currentCron;
                String newUrl = req.url() != null ? req.url() : currentUrl;
                String newMethod = req.method() != null ? req.method() : currentMethod;
                Integer newTimeout = req.timeoutSeconds() != null ? req.timeoutSeconds() : currentTimeout;

                // For payload/headers: if request field is present (even null explicitly), use request value;
                // otherwise fall back to current. Since records can't distinguish omitted vs null, treat null
                // as "keep current" for carry-forward semantics.
                String newPayloadJson = req.payload() != null ? toJson(req.payload()) : currentPayloadJson;
                String newHeadersJson = req.headers() != null ? toJson(req.headers()) : currentHeadersJson;

                // Insert new version
                UUID newDefinitionId;
                int newVersion = currentVersion + 1;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO job_definitions " +
                        "(job_family_id, version, name, cron, job_type, is_latest, is_parked, is_deleted, max_retries, created_by) " +
                        "VALUES (?, ?, ?, ?, 'HTTP'::job_type, TRUE, FALSE, FALSE, 3, 'submitter') " +
                        "RETURNING id")) {
                    ps.setObject(1, familyId);
                    ps.setInt(2, newVersion);
                    ps.setString(3, newName);
                    ps.setString(4, newCron);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        newDefinitionId = (UUID) rs.getObject("id");
                    }
                }

                insertHttpConfigRaw(conn, newDefinitionId, newUrl, newMethod,
                        newPayloadJson, newHeadersJson, newTimeout);

                conn.commit();
                log.info("Edited job family " + familyId + " → new version " + newVersion + " (id=" + newDefinitionId + ")");
                return new CreateJobResponse(familyId, newDefinitionId, newVersion);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void setParkStatus(UUID familyId, boolean park) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean currentParked;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT is_parked FROM job_definitions " +
                        "WHERE job_family_id = ? AND is_latest = TRUE AND is_deleted = FALSE")) {
                    ps.setObject(1, familyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new NotFoundException("No job found for family id: " + familyId);
                        }
                        currentParked = rs.getBoolean("is_parked");
                    }
                }

                if (currentParked == park) {
                    throw new com.github.asadyezdanschool.conductor.submitter.exception.ConflictException(
                            "Job is already " + (park ? "parked" : "unparked"));
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE job_definitions SET is_parked = ? " +
                        "WHERE job_family_id = ? AND is_latest = TRUE AND is_deleted = FALSE")) {
                    ps.setBoolean(1, park);
                    ps.setObject(2, familyId);
                    ps.executeUpdate();
                }

                conn.commit();
                log.info("Set is_parked=" + park + " for family " + familyId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void insertHttpConfig(Connection conn, UUID jobDefinitionId,
            String url, String method,
            java.util.Map<String, Object> payload, java.util.Map<String, String> headers,
            Integer timeoutSeconds) throws SQLException {
        String payloadJson = payload != null ? toJson(payload) : null;
        String headersJson = headers != null ? toJson(headers) : null;
        insertHttpConfigRaw(conn, jobDefinitionId, url, method, payloadJson, headersJson, timeoutSeconds);
    }

    private void insertHttpConfigRaw(Connection conn, UUID jobDefinitionId,
            String url, String method,
            String payloadJson, String headersJson, Integer timeoutSeconds) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO job_type_http_configs " +
                "(job_definition_id, url, method, payload, headers, timeout_seconds) " +
                "VALUES (?, ?, ?::request_type, ?::jsonb, ?::jsonb, ?)")) {
            ps.setObject(1, jobDefinitionId);
            ps.setString(2, url);
            ps.setString(3, method);
            if (payloadJson != null) {
                ps.setObject(4, payloadJson, Types.OTHER);
            } else {
                ps.setNull(4, Types.OTHER);
            }
            if (headersJson != null) {
                ps.setObject(5, headersJson, Types.OTHER);
            } else {
                ps.setNull(5, Types.OTHER);
            }
            if (timeoutSeconds != null) {
                ps.setInt(6, timeoutSeconds);
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}