package com.github.asadyezdanschool.conductor.scheduler.integration;

import com.github.asadyezdanschool.conductor.grpc.execution.*;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static io.grpc.Status.Code.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for the execution gRPC server.
 *
 * <p>Seeds the DB with real job + job_run rows via {@link JobRepositoryIntegrationBase}
 * (or direct SQL) and exercises the full GetHttpRunDetails → ReportStatus flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExecutionGrpcServerIntegrationTest {

    // TODO: spin up Testcontainers postgres + apply migrations

    private ManagedChannel channel;
    private JobExecutionServiceGrpc.JobExecutionServiceBlockingStub stub;

    @BeforeAll
    void startServer() throws Exception {
        // TODO: same setup pattern as ManagementGrpcServerIntegrationTest but registers
        //       JobExecutionServiceImpl; seed DB with a queued run for test data
    }

    @AfterAll
    void stopServer() throws Exception {
        // TODO
    }

    @Test
    void getHttpRunDetails_seededRun_returnsCorrectConfig() {
        // TODO: pre-seeded runId; stub.getHttpRunDetails(...)
        //       assert url, method, timeoutSeconds match seeded values
    }

    @Test
    void getHttpRunDetails_unknownRun_returnsNotFound() {
        // TODO: StatusRuntimeException with NOT_FOUND
    }

    @Test
    void reportStatus_running_updatesDbRow() {
        // TODO: reportStatus(runId, RUNNING)
        //       query DB; assert job_runs.status=RUNNING, started_at IS NOT NULL
    }

    @Test
    void reportStatus_succeeded_setsFinishedAtAndDuration() {
        // TODO: reportStatus(runId, SUCCEEDED, durationMs=1234)
        //       assert job_runs.status=SUCCEEDED, duration_ms=1234
        //       assert job_run_events row exists with source='worker'
    }

    @Test
    void reportStatus_failed_writesMessageAndHttpStatusCode() {
        // TODO: reportStatus(runId, FAILED, message="timeout", httpStatusCode=503)
        //       assert job_run_events row with correct fields
    }

    @Test
    void reportStatus_acknowledged_returnsTrue() {
        // TODO: assert ReportStatusResponse.getAcknowledged() == true for all status types
    }
}
