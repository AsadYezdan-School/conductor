package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;

import java.util.UUID;

/**
 * Handles worker-facing operations: serving job configuration for a queued run and
 * writing status transitions back to the database.
 *
 * <p>The scheduler is the sole DB writer for {@code job_runs} and {@code job_run_events};
 * workers never touch the database directly — all state transitions flow through here.
 */
public interface JobExecutionService {

    /**
     * Fetch the HTTP-specific details needed to execute a queued run.
     * Throws {@link io.grpc.StatusRuntimeException} with {@code NOT_FOUND} if the run ID
     * is unknown.
     */
    HttpRunDetails getHttpRunDetails(UUID jobRunId);

    /**
     * Record a status transition (RUNNING → SUCCEEDED | FAILED) for a run.
     * Writes to both {@code job_runs} and {@code job_run_events} in a single transaction.
     */
    ReportStatusResponse reportStatus(ReportStatusRequest request);
}
