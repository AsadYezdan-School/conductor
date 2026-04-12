package com.github.asadyezdanschool.conductor.scheduler.integration;

import com.github.asadyezdanschool.conductor.grpc.execution.JobType;
import com.github.asadyezdanschool.conductor.grpc.management.*;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for the management gRPC server.
 *
 * <p>Uses {@link InProcessServerBuilder} — no real network port needed.
 * The full Dagger component graph is instantiated with a Testcontainers PostgreSQL DB.
 *
 * <p>Each test asserts BOTH sides of the write-through design:
 * <ol>
 *   <li>The in-memory cache reflects the operation immediately</li>
 *   <li>The database row exists (or is absent) in the form we expect</li>
 * </ol>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManagementGrpcServerIntegrationTest {

    // TODO: shared DB setup — extract to a base class once both integration tests are written
    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.6")
                    .withDatabaseName("conductor")
                    .withUsername("conductor")
                    .withPassword("conductor");

    private ManagedChannel channel;
    private JobManagementServiceGrpc.JobManagementServiceBlockingStub stub;
    private DataSource dataSource;      // direct DB access for assertion queries
    // TODO: hold reference to component.jobCache() for cache assertions

    @BeforeAll
    void startServer() throws Exception {
        // TODO:
        //  1. postgres.start(); applyMigrations()
        //  2. Set system properties for DB env vars
        //  3. AppComponent component = DaggerAppComponent.create()
        //  4. component.jobCache().initialize()
        //  5. String serverName = InProcessServerBuilder.generateName()
        //  6. InProcessServerBuilder.forName(serverName)
        //       .addService(component.managementGrpcServer().serviceImpl())
        //       .build().start()
        //  7. channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        //  8. stub = JobManagementServiceGrpc.newBlockingStub(channel)
        //  9. dataSource = component.dataSource()  (expose via AppComponent for tests)
    }

    @AfterAll
    void stopServer() throws Exception {
        // TODO: channel.shutdownNow(); in-process server shutdown; postgres.stop()
    }

    // ── createJob ────────────────────────────────────────────────────────────

    @Test
    void createJob_success_rowExistsInDbWithCorrectFields() {
        // TODO: CreateJobResponse resp = stub.createJob(validCreateRequest())
        //       UUID definitionId = UUID.fromString(resp.getJobDefinitionId())
        //
        //       // DB assertion: job_definitions row
        //       try (Connection c = dataSource.getConnection();
        //            PreparedStatement ps = c.prepareStatement(
        //                "SELECT name, cron, is_latest, is_parked, is_deleted FROM job_definitions WHERE id = ?")) {
        //         ps.setObject(1, definitionId)
        //         ResultSet rs = ps.executeQuery(); assertTrue(rs.next())
        //         assertEquals("integration-test-job", rs.getString("name"))
        //         assertEquals("*/10 * * * *",         rs.getString("cron"))
        //         assertTrue(rs.getBoolean("is_latest"))
        //         assertFalse(rs.getBoolean("is_parked"))
        //         assertFalse(rs.getBoolean("is_deleted"))
        //       }
        //
        //       // DB assertion: job_type_http_configs row
        //       try (Connection c = dataSource.getConnection(); ...) {
        //         assertRow with url, method, timeout_seconds
        //       }
        //
        //       // Cache assertion
        //       UUID familyId = UUID.fromString(resp.getJobFamilyId())
        //       assertTrue(cache.get(familyId).isPresent())
    }

    @Test
    void createJob_success_httpConfigRowExistsInDb() {
        // TODO: verify job_type_http_configs row exists for the created definition
        //       assert url = "http://example.com", method = "GET", timeout_seconds = 30
    }

    @Test
    void createJob_success_jobAppearsInCache() {
        // TODO: after createJob, assert cache.get(familyId).isPresent()
        //       assert cached.cron() matches request cron
        //       assert cached.nextScheduledAt() is in the future
    }

    @Test
    void createJob_invalidCron_returnsInvalidArgument_noDbRow() {
        // TODO: stub.createJob with cron="not-a-cron" → StatusRuntimeException INVALID_ARGUMENT
        //       also verify: no job_definitions row was inserted (count = 0 for that name)
    }

    // ── editJob ───────────────────────────────────────────────────────────────

    @Test
    void editJob_success_newVersionRowInDb_oldVersionMarkedNotLatest() {
        // TODO: create job; edit with new name
        //
        //       // DB assertion: new version is_latest=TRUE
        //       SELECT version, is_latest FROM job_definitions WHERE job_family_id = ?
        //       assertEquals(2, newRow.version); assertTrue(newRow.is_latest)
        //
        //       // DB assertion: old version is_latest=FALSE
        //       SELECT is_latest FROM job_definitions WHERE id = oldDefinitionId
        //       assertFalse(oldRow.is_latest)
    }

    @Test
    void editJob_success_newVersionInCache_oldVersionGone() {
        // TODO: after edit:
        //       assertTrue(cache.get(familyId).isPresent())
        //       assertEquals(newDefinitionId, cache.get(familyId).get().definitionId())
    }

    @Test
    void editJob_carryForward_dbRowKeepsExistingUrl() {
        // TODO: create job with url="http://original.com"
        //       edit with only name changed (no url in request)
        //       SELECT url FROM job_type_http_configs WHERE job_definition_id = newDefinitionId
        //       assertEquals("http://original.com", url)   // carried forward
    }

    // ── parkJob ───────────────────────────────────────────────────────────────

    @Test
    void parkJob_success_dbRowHasIsParkTrue() {
        // TODO: create; park
        //       SELECT is_parked FROM job_definitions WHERE job_family_id = ? AND is_latest = TRUE
        //       assertTrue(is_parked)
    }

    @Test
    void parkJob_success_removedFromCache() {
        // TODO: create; park
        //       assertTrue(cache.get(familyId).isEmpty())
    }

    @Test
    void parkJob_alreadyParked_returnsAlreadyExists_dbUnchanged() {
        // TODO: create; park; park again → ALREADY_EXISTS
        //       SELECT COUNT from job_definitions WHERE job_family_id=? AND is_parked=TRUE → still 1, no duplicate
    }

    @Test
    void parkJob_unknownFamily_returnsNotFound_noDbChange() {
        // TODO: StatusRuntimeException with NOT_FOUND for random UUID
    }

    // ── unparkJob ─────────────────────────────────────────────────────────────

    @Test
    void unparkJob_success_dbRowHasIsParkFalse() {
        // TODO: create; park; unpark
        //       SELECT is_parked → assertFalse
    }

    @Test
    void unparkJob_success_reappearsInCache() {
        // TODO: create; park; unpark
        //       assertTrue(cache.get(familyId).isPresent())
        //       assert cached job's nextScheduledAt is in the future
    }

    @Test
    void unparkJob_unknownFamily_returnsNotFound() {
        // TODO: StatusRuntimeException NOT_FOUND for random UUID
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateJobRequest validCreateRequest() {
        return CreateJobRequest.newBuilder()
                .setName("integration-test-job")
                .setCron("*/10 * * * *")
                .setJobType(JobType.HTTP)
                .setHttpConfig(HttpJobConfig.newBuilder()
                        .setUrl("http://example.com")
                        .setMethod("GET")
                        .setTimeoutSeconds(30)
                        .build())
                .build();
    }
}
