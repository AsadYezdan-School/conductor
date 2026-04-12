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
import java.util.logging.Logger;

/**
 * Default implementation of {@link JobExecutionService}.
 */
@Singleton
public class JobExecutionServiceImpl implements JobExecutionService {

    private static final Logger log = Logger.getLogger(JobExecutionServiceImpl.class.getName());

    private final JobRepository repository;
    private final EnqueueService enqueueService;

    @Inject
    public JobExecutionServiceImpl(JobRepository repository, EnqueueService enqueueService) {
        this.repository = repository;
        this.enqueueService = enqueueService;
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
            boolean shouldRetry = false;
            switch (request.getStatus()) {
                case RUNNING -> repository.markRunning(runId);
                case SUCCEEDED -> repository.markSucceeded(
                        runId,
                        request.getDurationMs(),
                        request.getHttpStatusCode(),
                        request.getResponseBody().isEmpty() ? null : request.getResponseBody()
                );
                case FAILED -> {
                    repository.markFailed(
                            runId,
                            request.getDurationMs(),
                            request.getMessage().isEmpty() ? null : request.getMessage(),
                            request.getHttpStatusCode(),
                            request.getResponseBody().isEmpty() ? null : request.getResponseBody()
                    );
                    shouldRetry = maybeEnqueueRetry(runId);
                }
                default -> throw Status.INVALID_ARGUMENT
                        .withDescription("Unsupported status: " + request.getStatus())
                        .asRuntimeException();
            }
            return ReportStatusResponse.newBuilder()
                    .setAcknowledged(true)
                    .setShouldRetry(shouldRetry)
                    .build();
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("DB error: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }

    /**
     * Checks whether the failed run has retries remaining. If so, enqueues a new retry run
     * and returns true. Returns false if the job has exhausted its attempts.
     */
    private boolean maybeEnqueueRetry(UUID failedRunId) {
        try {
            HttpRunDetails details = repository.getHttpRunDetails(failedRunId)
                    .orElse(null);
            if (details == null) {
                log.warning("Could not load run details for retry check: " + failedRunId);
                return false;
            }
            int nextAttempt = details.attemptNumber() + 1;
            if (details.attemptNumber() >= details.maxRetries()) {
                log.info("Run " + failedRunId + " exhausted retries ("
                        + details.attemptNumber() + "/" + details.maxRetries() + ") — not retrying");
                return false;
            }
            log.info("Run " + failedRunId + " failed on attempt " + details.attemptNumber()
                    + "/" + details.maxRetries() + " — enqueuing retry (attempt " + nextAttempt + ")");
            enqueueService.enqueueRetry(failedRunId, details, nextAttempt);
            return true;
        } catch (Exception e) {
            log.severe("Failed to check/enqueue retry for run " + failedRunId + ": " + e.getMessage());
            return false;
        }
    }
}
