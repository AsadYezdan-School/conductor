package com.github.asadyezdanschool.conductor.submitter.repository;

import com.github.asadyezdanschool.conductor.submitter.model.AlertConfigRequest;
import com.github.asadyezdanschool.conductor.submitter.model.AlertConfigResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class AlertConfigRepository {

    private static final Logger log = Logger.getLogger(AlertConfigRepository.class.getName());

    private final DataSource dataSource;

    @Inject
    public AlertConfigRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AlertConfigResponse upsertAlertConfig(UUID familyId, AlertConfigRequest req) {
        String sql =
                "INSERT INTO job_family_alert_configs " +
                "  (job_family_id, min_success_rate_pct, max_avg_duration_ms, updated_at) " +
                "VALUES (?, ?, ?, NOW()) " +
                "ON CONFLICT (job_family_id) DO UPDATE SET " +
                "  min_success_rate_pct = EXCLUDED.min_success_rate_pct, " +
                "  max_avg_duration_ms  = EXCLUDED.max_avg_duration_ms, " +
                "  updated_at           = EXCLUDED.updated_at " +
                "RETURNING job_family_id::text, min_success_rate_pct, max_avg_duration_ms, updated_at";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, familyId);
            if (req.minSuccessRatePct() != null) {
                ps.setDouble(2, req.minSuccessRatePct());
            } else {
                ps.setNull(2, Types.NUMERIC);
            }
            if (req.maxAvgDurationMs() != null) {
                ps.setInt(3, req.maxAvgDurationMs());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double rate = rs.getDouble("min_success_rate_pct");
                boolean rateNull = rs.wasNull();
                int dur = rs.getInt("max_avg_duration_ms");
                boolean durNull = rs.wasNull();
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                return new AlertConfigResponse(
                        rs.getString("job_family_id"),
                        rateNull ? null : rate,
                        durNull  ? null : dur,
                        updatedAt != null ? updatedAt.toInstant().toString() : null
                );
            }
        } catch (SQLException e) {
            log.severe("Failed to upsert alert config for family " + familyId + ": " + e.getMessage());
            throw new RuntimeException("Failed to upsert alert config", e);
        }
    }
}
