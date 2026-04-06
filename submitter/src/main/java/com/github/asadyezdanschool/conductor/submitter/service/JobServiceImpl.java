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
import java.util.logging.Logger;

@Singleton
public class JobServiceImpl implements JobService {

    private static final Logger log = Logger.getLogger(JobServiceImpl.class.getName());

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
            UUID familyId = UUID.fromString(resp.getJobFamilyId());
            log.info("Job \"" + req.name() + "\" (" + familyId + ") was created (v" + resp.getVersion() + ", cron=\"" + req.cron() + "\")");
            return new JobCreationResponse(
                    familyId,
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
            log.info("Job (" + familyId + ") was updated"
                    + (req.name() != null ? " name=\"" + req.name() + "\"" : "")
                    + (req.cron()  != null ? " cron=\"" + req.cron() + "\""  : "")
                    + " -> v" + resp.getVersion());
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
            log.info("Job (" + familyId + ") was parked");
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
            log.info("Job (" + familyId + ") was unparked");
            return new ParkStatusResponse(UUID.fromString(resp.getJobFamilyId()), resp.getIsParked());
        } catch (StatusRuntimeException e) {
            throw mapGrpcException(e);
        }
    }

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
