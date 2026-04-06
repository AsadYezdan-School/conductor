package com.github.asadyezdanschool.conductor.submitter.service;

import com.github.asadyezdanschool.conductor.grpc.execution.JobType;
import com.github.asadyezdanschool.conductor.grpc.management.*;
import com.github.asadyezdanschool.conductor.submitter.exception.ConflictException;
import com.github.asadyezdanschool.conductor.submitter.exception.NotFoundException;
import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.grpc.SchedulerGrpcClient;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationResponse;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import io.grpc.StatusRuntimeException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

/**
 * Delegates all job management operations to the scheduler's {@code JobManagementService}
 * via gRPC. No database access — the scheduler is the sole DB writer.
 *
 * <h3>gRPC → domain exception mapping</h3>
 * <table>
 *   <tr><th>gRPC status</th><th>Exception thrown</th><th>HTTP status</th></tr>
 *   <tr><td>NOT_FOUND</td><td>{@link NotFoundException}</td><td>404</td></tr>
 *   <tr><td>ALREADY_EXISTS</td><td>{@link ConflictException}</td><td>409</td></tr>
 *   <tr><td>INVALID_ARGUMENT</td><td>{@link ValidationException}</td><td>422</td></tr>
 *   <tr><td>INTERNAL / other</td><td>{@link RuntimeException}</td><td>500</td></tr>
 * </table>
 */
@Singleton
public class JobServiceImpl implements JobService {

    private final SchedulerGrpcClient grpcClient;

    @Inject
    public JobServiceImpl(SchedulerGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    @Override
    public JobCreationResponse createJob(JobCreationRequest req) {
        try {
            HttpJobConfig.Builder httpConfig = HttpJobConfig.newBuilder()
                    .setUrl(req.url() != null ? req.url() : "")
                    .setMethod(req.method() != null ? req.method() : "GET");
            if (req.timeoutSeconds() != null) httpConfig.setTimeoutSeconds(req.timeoutSeconds());

            CreateJobRequest proto = CreateJobRequest.newBuilder()
                    .setName(req.name() != null ? req.name() : "")
                    .setCron(req.cron() != null ? req.cron() : "")
                    .setJobType(JobType.HTTP)
                    .setHttpConfig(httpConfig.build())
                    .build();

            CreateJobResponse resp = grpcClient.stub().createJob(proto);
            return new JobCreationResponse(
                    UUID.fromString(resp.getJobFamilyId()),
                    UUID.fromString(resp.getJobDefinitionId()),
                    resp.getVersion()
            );
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }
    }

    @Override
    public JobCreationResponse editJob(UUID familyId, EditJobRequest req) {
        try {
            com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest.Builder protoBuilder =
                    com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest.newBuilder()
                    .setJobFamilyId(familyId.toString());

            if (req.name() != null) protoBuilder.setName(req.name());
            if (req.cron()  != null) protoBuilder.setCron(req.cron());

            if (req.url() != null || req.method() != null || req.timeoutSeconds() != null) {
                HttpEditConfig.Builder httpEdit = HttpEditConfig.newBuilder();
                if (req.url()            != null) httpEdit.setUrl(req.url());
                if (req.method()         != null) httpEdit.setMethod(req.method());
                if (req.timeoutSeconds() != null) httpEdit.setTimeoutSeconds(req.timeoutSeconds());
                protoBuilder.setHttpConfig(httpEdit.build());
            }

            com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse resp =
                    grpcClient.stub().editJob(protoBuilder.build());
            return new JobCreationResponse(
                    UUID.fromString(resp.getJobFamilyId()),
                    UUID.fromString(resp.getJobDefinitionId()),
                    resp.getVersion()
            );
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }
    }

    @Override
    public ParkStatusResponse parkJob(UUID familyId) {
        try {
            ParkJobResponse resp = grpcClient.stub().parkJob(
                    ParkJobRequest.newBuilder().setJobFamilyId(familyId.toString()).build());
            return new ParkStatusResponse(UUID.fromString(resp.getJobFamilyId()), resp.getIsParked());
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }
    }

    @Override
    public ParkStatusResponse unparkJob(UUID familyId) {
        try {
            UnparkJobResponse resp = grpcClient.stub().unparkJob(
                    UnparkJobRequest.newBuilder().setJobFamilyId(familyId.toString()).build());
            return new ParkStatusResponse(UUID.fromString(resp.getJobFamilyId()), resp.getIsParked());
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Map a gRPC {@link StatusRuntimeException} to the appropriate domain exception so the
     * existing {@link com.github.asadyezdanschool.conductor.submitter.exception.ExceptionMappers}
     * can translate it to the correct HTTP response.
     */
    private RuntimeException mapGrpcException(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND       -> new NotFoundException(e.getStatus().getDescription());
            case ALREADY_EXISTS  -> new ConflictException(e.getStatus().getDescription());
            case INVALID_ARGUMENT -> new ValidationException(
                    List.of(e.getStatus().getDescription() != null
                            ? e.getStatus().getDescription() : "Invalid request"));
            default -> new RuntimeException("Scheduler error: " + e.getMessage(), e);
        };
    }
}
