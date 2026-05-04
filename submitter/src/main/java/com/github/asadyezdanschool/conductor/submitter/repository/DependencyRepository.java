package com.github.asadyezdanschool.conductor.submitter.repository;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.AddDependencyRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobDependenciesResponse;
import com.github.asadyezdanschool.conductor.submitter.model.JobRef;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class DependencyRepository {

    private static final Logger log = Logger.getLogger(DependencyRepository.class.getName());

    private final DataSource dataSource;

    @Inject
    public DependencyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JobDependenciesResponse getDependencies(UUID familyId) {
        String upstreamSql =
                "SELECT jd.job_family_id::text, jd.name " +
                "FROM job_dependencies dep " +
                "JOIN job_definitions jd ON jd.job_family_id = dep.upstream_family_id " +
                "  AND jd.is_latest = TRUE AND jd.is_deleted = FALSE " +
                "WHERE dep.downstream_family_id = ? " +
                "ORDER BY jd.name ASC";

        String downstreamSql =
                "SELECT jd.job_family_id::text, jd.name " +
                "FROM job_dependencies dep " +
                "JOIN job_definitions jd ON jd.job_family_id = dep.downstream_family_id " +
                "  AND jd.is_latest = TRUE AND jd.is_deleted = FALSE " +
                "WHERE dep.upstream_family_id = ? " +
                "ORDER BY jd.name ASC";

        try (Connection conn = dataSource.getConnection()) {
            List<JobRef> upstreams = queryRefs(conn, upstreamSql, familyId);
            List<JobRef> downstreams = queryRefs(conn, downstreamSql, familyId);
            return new JobDependenciesResponse(upstreams, downstreams);
        } catch (SQLException e) {
            log.severe("Failed to get dependencies for family " + familyId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get dependencies", e);
        }
    }

    public void addDependency(UUID downstreamFamilyId, UUID upstreamFamilyId) {
        if (wouldCreateCycle(downstreamFamilyId, upstreamFamilyId)) {
            throw new ValidationException(List.of("dependency would create a cycle"));
        }

        String sql =
                "INSERT INTO job_dependencies (upstream_family_id, downstream_family_id) " +
                "VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, upstreamFamilyId);
            ps.setObject(2, downstreamFamilyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to add dependency " + upstreamFamilyId + " → " + downstreamFamilyId + ": " + e.getMessage());
            throw new RuntimeException("Failed to add dependency", e);
        }
    }

    public void removeDependency(UUID downstreamFamilyId, UUID upstreamFamilyId) {
        String sql =
                "DELETE FROM job_dependencies " +
                "WHERE downstream_family_id = ? AND upstream_family_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, downstreamFamilyId);
            ps.setObject(2, upstreamFamilyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to remove dependency: " + e.getMessage());
            throw new RuntimeException("Failed to remove dependency", e);
        }
    }

    // Returns true if adding upstream→downstream would create a cycle (i.e. downstream
    // is already reachable from upstream, meaning upstream already depends on downstream).
    private boolean wouldCreateCycle(UUID downstreamFamilyId, UUID upstreamFamilyId) {
        String sql =
                "WITH RECURSIVE reachable AS ( " +
                "  SELECT downstream_family_id AS id " +
                "  FROM job_dependencies WHERE upstream_family_id = ? " +
                "  UNION ALL " +
                "  SELECT dep.downstream_family_id " +
                "  FROM job_dependencies dep JOIN reachable r ON dep.upstream_family_id = r.id " +
                ") " +
                "SELECT 1 FROM reachable WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, upstreamFamilyId);
            ps.setObject(2, downstreamFamilyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.severe("Failed to check for dependency cycle: " + e.getMessage());
            throw new RuntimeException("Failed to check for cycle", e);
        }
    }

    private List<JobRef> queryRefs(Connection conn, String sql, UUID familyId) throws SQLException {
        List<JobRef> refs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, familyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    refs.add(new JobRef(rs.getString("job_family_id"), rs.getString("name")));
                }
            }
        }
        return refs;
    }
}
