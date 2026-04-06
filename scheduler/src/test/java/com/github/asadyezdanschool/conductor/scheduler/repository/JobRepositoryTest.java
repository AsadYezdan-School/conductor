package com.github.asadyezdanschool.conductor.scheduler.repository;

import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link JobRepository} against a real PostgreSQL instance
 * managed by Testcontainers.
 *
 * <p>Each test method runs in its own transaction that is rolled back at the end,
 * keeping tests independent.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobRepositoryTest {

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.6")
                    .withDatabaseName("conductor")
                    .withUsername("conductor")
                    .withPassword("conductor");

    private JobRepository repository;

    @BeforeAll
    void startAll() throws Exception {
        postgres.start();
        applyMigrations();
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(postgres.getJdbcUrl());
        cfg.setUsername(postgres.getUsername());
        cfg.setPassword(postgres.getPassword());
        repository = new JobRepository(new HikariDataSource(cfg));
    }

    @AfterAll
    void stopAll() {
        postgres.stop();
    }

    // ── loadActiveJobs ────────────────────────────────────────────────────────

    @Test
    void loadActiveJobs_returnsOnlyLatestNonParkedNonDeleted() {
        // TODO: insert 3 job definitions:
        //   - active (is_latest=T, is_parked=F, is_deleted=F) → should be returned
        //   - parked (is_parked=T) → excluded
        //   - deleted (is_deleted=T) → excluded
        //   loadActiveJobs() returns exactly the 1 active job
    }

    @Test
    void loadActiveJobs_jobWithNoScheduleRow_returnsNullLastTriggeredAt() {
        // TODO: insert active job with no job_schedules row
        //       assert returned row has lastTriggeredAt == null
    }

    @Test
    void loadActiveJobs_jobWithScheduleRow_returnsLastTriggeredAt() {
        // TODO: insert job + job_schedules row; assert lastTriggeredAt populated
    }

    // ── createJobDefinition ───────────────────────────────────────────────────

    @Test
    void createJobDefinition_insertsDefinitionAndHttpConfig() {
        // TODO: call createJobDefinition with valid params
        //       assert result.version() == 1
        //       query DB directly and verify both job_definitions and job_type_http_configs rows exist
    }

    @Test
    void createJobDefinition_isLatestTrueByDefault() {
        // TODO: verify is_latest=TRUE on the inserted row
    }

    // ── editJobDefinition ─────────────────────────────────────────────────────

    @Test
    void editJobDefinition_createsNewVersionAndMarksOldNotLatest() {
        // TODO: create job; edit with new name
        //       assert new version = 2, new is_latest=TRUE, old is_latest=FALSE
    }

    @Test
    void editJobDefinition_carryForward_nullFieldsKeepExistingValues() {
        // TODO: create job with url="http://old.com"; edit with null url
        //       assert new version's http_config still has url="http://old.com"
    }

    @Test
    void editJobDefinition_unknownFamily_throwsNotFoundException() {
        // TODO: call editJobDefinition with random UUID → NotFoundException
    }

    // ── setParkStatus ─────────────────────────────────────────────────────────

    @Test
    void setParkStatus_park_setsIsParkTrue() {
        // TODO: create active job; setParkStatus(familyId, true)
        //       query DB and assert is_parked=TRUE
    }

    @Test
    void setParkStatus_unpark_setsIsParkFalse() {
        // TODO: create parked job; setParkStatus(familyId, false) → is_parked=FALSE
    }

    @Test
    void setParkStatus_alreadyParked_throwsConflictException() {
        // TODO: create parked job; setParkStatus(familyId, true) → ConflictException
    }

    @Test
    void setParkStatus_unknownFamily_throwsNotFoundException() {
        // TODO: setParkStatus with random UUID → NotFoundException
    }

    // ── enqueueRun ────────────────────────────────────────────────────────────

    @Test
    void enqueueRun_insertsJobRunEventAndScheduleInOneTransaction() {
        // TODO: create job definition; enqueueRun(...)
        //       assert: job_runs row with status=QUEUED, job_run_events row with source='scheduler',
        //       job_schedules row with last_triggered_at set
    }

    @Test
    void enqueueRun_returnsGeneratedRunId() {
        // TODO: assert returned UUID is non-null and matches the inserted row
    }

    // ── getHttpRunDetails ─────────────────────────────────────────────────────

    @Test
    void getHttpRunDetails_found_returnsAllFields() {
        // TODO: create job + enqueue run; getHttpRunDetails(runId)
        //       assert url, method, timeoutSeconds match what was inserted
    }

    @Test
    void getHttpRunDetails_unknownRunId_returnsEmpty() {
        // TODO: Optional.empty() for unknown UUID
    }

    // ── status updates ────────────────────────────────────────────────────────

    @Test
    void markRunning_updatesStatusAndStartedAt() {
        // TODO: create + enqueue run; markRunning(runId)
        //       assert job_runs.status=RUNNING, started_at IS NOT NULL
        //       assert job_run_events contains RUNNING row with source='worker'
    }

    @Test
    void markSucceeded_updatesStatusFinishedAtAndDuration() {
        // TODO: create + enqueue + markRunning; markSucceeded(runId, 1234L)
        //       assert SUCCEEDED, finished_at, duration_ms=1234
    }

    @Test
    void markFailed_writesMessageAndHttpStatusCode() {
        // TODO: markFailed(runId, 500L, "timeout", 503, "{}")
        //       assert job_run_events row has correct message + http_status_code + response_body
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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
