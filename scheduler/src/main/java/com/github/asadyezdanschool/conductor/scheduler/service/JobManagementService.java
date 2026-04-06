package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse;

/**
 * Business logic for job definition lifecycle operations.
 * Implementations write to the DB via {@link com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository}
 * and update the in-memory {@link com.github.asadyezdanschool.conductor.scheduler.cache.JobCache} so that
 * changes take effect immediately without waiting for the next background refresh.
 *
 * <p>All validation (cron syntax, required fields, idempotency guards) is performed here
 * before any DB write. Errors are surfaced as {@link io.grpc.StatusRuntimeException}.
 */
public interface JobManagementService {

    CreateJobResponse  createJob(CreateJobRequest  request);
    EditJobResponse    editJob(EditJobRequest    request);
    ParkJobResponse    parkJob(ParkJobRequest    request);
    UnparkJobResponse  unparkJob(UnparkJobRequest  request);
}
