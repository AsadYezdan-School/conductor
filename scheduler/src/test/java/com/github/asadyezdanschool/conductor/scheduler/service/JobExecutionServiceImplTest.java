package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.execution.JobStatus;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static io.grpc.Status.Code.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobExecutionServiceImplTest {

    private JobRepository              mockRepo;
    private EnqueueService             mockEnqueue;
    private JobExecutionServiceImpl    service;

    @BeforeEach
    void setUp() {
        mockRepo    = mock(JobRepository.class);
        mockEnqueue = mock(EnqueueService.class);
        service     = new JobExecutionServiceImpl(mockRepo, mockEnqueue);
    }

    // ── getHttpRunDetails ─────────────────────────────────────────────────────

    @Test
    void getHttpRunDetails_found_returnsDetails() {
        // TODO: stub repo.getHttpRunDetails(uuid) to return Optional.of(sampleDetails())
        //       HttpRunDetails result = service.getHttpRunDetails(uuid)
        //       assertNotNull(result); assertEquals("http://example.com", result.url())
    }

    @Test
    void getHttpRunDetails_notFound_throwsNotFound() {
        // TODO: stub repo.getHttpRunDetails to return Optional.empty()
        //       StatusRuntimeException ex = assertThrows(...)
        //       assertEquals(NOT_FOUND, ex.getStatus().getCode())
    }

    @Test
    void getHttpRunDetails_repositoryThrows_propagatesAsInternalError() {
        // TODO: stub repo to throw SQLException (wrapped in RuntimeException)
        //       assert StatusRuntimeException with INTERNAL code
    }

    // ── reportStatus ─────────────────────────────────────────────────────────

    @Test
    void reportStatus_running_callsMarkRunning() {
        // TODO: service.reportStatus(runningRequest())
        //       verify(mockRepo).markRunning(uuid)
        //       assert response.getAcknowledged() == true
    }

    @Test
    void reportStatus_succeeded_callsMarkSucceededWithDuration() {
        // TODO: ReportStatusRequest with SUCCEEDED + duration_ms=1500
        //       verify(mockRepo).markSucceeded(uuid, 1500L)
    }

    @Test
    void reportStatus_failed_callsMarkFailedWithMessageAndHttpStatus() {
        // TODO: FAILED request with message, http_status_code=503, response_body
        //       verify(mockRepo).markFailed(uuid, durationMs, message, 503, responseBody)
    }

    @Test
    void reportStatus_unknownStatus_throwsInvalidArgument() {
        // TODO: JobStatus.JOB_STATUS_UNSPECIFIED or QUEUED (not a valid worker-reported status)
        //       assert INVALID_ARGUMENT
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private HttpRunDetails sampleDetails() {
        return new HttpRunDetails(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "http://example.com/hook", "POST",
                null, null, 30, 1, 3
        );
    }

    private ReportStatusRequest runningRequest() {
        return ReportStatusRequest.newBuilder()
                .setJobRunId(UUID.randomUUID().toString())
                .setStatus(JobStatus.RUNNING)
                .build();
    }
}
