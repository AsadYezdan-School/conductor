package com.github.asadyezdanschool.conductor.scheduler.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobDependencyTest {

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.6")
                    .withDatabaseName("conductor")
                    .withUsername("conductor")
                    .withPassword("conductor");

    private DataSource dataSource;

    @BeforeAll
    void startAll() throws Exception {
        postgres.start();
        applyMigrations();
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(postgres.getJdbcUrl());
        cfg.setUsername(postgres.getUsername());
        cfg.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(cfg);
    }

    @AfterAll
    void stopAll() {
        postgres.stop();
    }

    // ── valid DAG insertions ──────────────────────────────────────────────────

    @Test
    void linearChain_succeeds() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        insertDep(a, b);
        insertDep(b, c);
        // A→B→C is a valid linear chain; no assertion needed beyond no exception
    }

    @Test
    void diamondDag_succeeds() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        insertDep(a, c);
        insertDep(b, c);
        // A→C, B→C — two independent upstreams converging on one downstream
    }

    @Test
    void extendingChain_succeeds() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        insertDep(a, b);
        insertDep(b, c);
        insertDep(a, d); // parallel branch from A, no cycle
    }

    // ── cycle detection ───────────────────────────────────────────────────────

    @Test
    void twoNodeCycle_rejected() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        insertDep(a, b);
        assertCycleRejected(b, a); // B→A would give A→B→A
    }

    @Test
    void threeNodeCycle_rejected() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        insertDep(a, b);
        insertDep(b, c);
        assertCycleRejected(c, a); // C→A would give A→B→C→A
    }

    @Test
    void fourNodeCycle_rejected() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        insertDep(a, b);
        insertDep(b, c);
        insertDep(c, d);
        assertCycleRejected(d, a); // D→A would give A→B→C→D→A
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void insertDep(UUID upstream, UUID downstream) throws Exception {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO job_dependencies (upstream_family_id, downstream_family_id) VALUES (?, ?)")) {
            ps.setObject(1, upstream);
            ps.setObject(2, downstream);
            ps.executeUpdate();
        }
    }

    private void assertCycleRejected(UUID upstream, UUID downstream) {
        SQLException ex = assertThrows(SQLException.class, () -> insertDep(upstream, downstream));
        assertTrue(ex.getMessage().contains("dependency cycle"),
                "Expected 'dependency cycle' in exception message, got: " + ex.getMessage());
    }

    // ── migration setup ───────────────────────────────────────────────────────

    private void applyMigrations() throws Exception {
        String srcDir = System.getenv("TEST_SRCDIR");
        Path changelogDir;
        if (srcDir != null) {
            String workspace = System.getenv("TEST_WORKSPACE");
            if (workspace == null) workspace = "_main";
            changelogDir = Paths.get(srcDir, workspace, "db-migrations", "changelog");
        } else {
            changelogDir = Paths.get("db-migrations", "changelog").toAbsolutePath();
        }
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase liquibase = new Liquibase(
                    "db.changelog-master.yaml",
                    new DirectoryResourceAccessor(changelogDir),
                    database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }
}
