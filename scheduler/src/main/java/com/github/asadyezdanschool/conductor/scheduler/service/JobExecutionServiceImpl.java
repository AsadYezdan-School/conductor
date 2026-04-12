package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.execution.JobStatus;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import io.grpc.Status;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * Default implementation of {@link JobExecutionService}.
 */
@Singleton
public class JobExecutionServiceImpl implements JobExecutionService {

    private final JobRepository repository;

    @Inject
    public JobExecutionServiceImpl(JobRepository repository) {
        this.repository = repository;
    }

    @Override
    public HttpRunDetails getHttpRunDetails(UUID jobRunId) {
        try {
            return repository.getHttpRunDetails(jobRunId)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("run not found: " + jobRunId)
                            .asRuntimeException());
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("DB error: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }

    @Override
    public ReportStatusResponse reportStatus(ReportStatusRequest request) {
        UUID runId = UUID.fromString(request.getJobRunId());
        try {
            switch (request.getStatus()) {
                case RUNNING -> repository.markRunning(runId);
                case SUCCEEDED -> repository.markSucceeded(
                        runId,
                        request.getDurationMs(),
                        request.getHttpStatusCode(),
                        request.getResponseBody().isEmpty() ? null : request.getResponseBody()
                );
                case FAILED -> repository.markFailed(
                        runId,
                        request.getDurationMs(),
                        request.getMessage().isEmpty() ? null : request.getMessage(),
                        request.getHttpStatusCode(),
                        request.getResponseBody().isEmpty() ? null : request.getResponseBody()
                );
                default -> throw Status.INVALID_ARGUMENT
                        .withDescription("Unsupported status: " + request.getStatus())
                        .asRuntimeException();
            }
            return ReportStatusResponse.newBuilder().setAcknowledged(true).build();
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("DB error: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }
}
