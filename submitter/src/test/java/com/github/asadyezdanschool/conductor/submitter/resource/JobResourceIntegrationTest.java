package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.grpc.management.*;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the submitter's REST API endpoints.
 *
 * <p>Uses an in-process gRPC mock server (grpc-inprocess) so no real scheduler is needed.
 * The full Jersey HTTP stack is started on a random port using the existing
 * {@link com.github.asadyezdanschool.conductor.submitter.AppComponent} Dagger wiring.
 *
 * <p>The mock gRPC server is registered as a {@link JobManagementServiceGrpc.JobManagementServiceImplBase}
 * where each test method can configure the desired response/error.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobResourceIntegrationTest {

    // TODO: start in-process gRPC server + Jersey HTTP server before tests

    @BeforeAll
    void startAll() throws Exception {
        // TODO:
        //  1. serverName = InProcessServerBuilder.generateName()
        //  2. Register a configurable stub service with InProcessServerBuilder.forName(serverName)
        //  3. Build in-process channel: InProcessChannelBuilder.forName(serverName)
        //  4. Wire the channel into a SchedulerGrpcClient override for DI
        //  5. Start Jersey HTTP server on random port; set RestAssured base URI + port
    }

    @AfterAll
    void stopAll() {
        // TODO: shut down HTTP server + in-process gRPC server
    }

    // ── POST /jobs ────────────────────────────────────────────────────────────

    @Test
    void postJobs_validRequest_returns201WithLocation() {
        // TODO: configure mock to return CreateJobResponse with a familyId
        //       given().contentType(ContentType.JSON).body(validCreateBody())
        //       .when().post("/jobs")
        //       .then().statusCode(201).body("jobFamilyId", notNullValue())
    }

    @Test
    void postJobs_missingName_returns422() {
        // TODO: configure mock to raise INVALID_ARGUMENT (or test validation before gRPC call)
        //       body missing 'name' field → 422
    }

    @Test
    void postJobs_invalidCron_returns422() {
        // TODO: mock returns INVALID_ARGUMENT; assert 422
    }

    // ── PUT /jobs/{id} ────────────────────────────────────────────────────────

    @Test
    void putJobs_validEdit_returns200() {
        // TODO: configure mock editJob response; assert 200 with new version
    }

    @Test
    void putJobs_unknownFamily_returns404() {
        // TODO: mock returns NOT_FOUND; assert 404
    }

    // ── POST /jobs/{id}/park ──────────────────────────────────────────────────

    @Test
    void parkJob_returns200WithIsParkTrue() {
        // TODO: configure mock parkJob response; assert 200, is_parked=true
    }

    @Test
    void parkJob_alreadyParked_returns409() {
        // TODO: mock returns ALREADY_EXISTS; assert 409
    }

    @Test
    void parkJob_unknownFamily_returns404() {
        // TODO: mock returns NOT_FOUND; assert 404
    }

    // ── POST /jobs/{id}/unpark ────────────────────────────────────────────────

    @Test
    void unparkJob_returns200WithIsParkFalse() {
        // TODO: configure mock unparkJob response; assert 200, is_parked=false
    }

    @Test
    void unparkJob_alreadyUnparked_returns409() {
        // TODO: mock returns ALREADY_EXISTS; assert 409
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String validCreateBody() {
        return """
                {
                  "name": "integration-test-job",
                  "cron": "*/5 * * * *",
                  "url": "http://example.com/hook",
                  "method": "POST",
                  "timeoutSeconds": 30
                }
                """;
    }
}
