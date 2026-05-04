package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.management.*;
import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.cache.JobCache;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpHeader;
import com.github.asadyezdanschool.conductor.scheduler.model.JobType;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import com.github.asadyezdanschool.conductor.scheduler.scheduling.CronEvaluator;
import io.grpc.Status;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link JobManagementService}.
 *
 * <p>Write-through design: every mutating operation:
 * <ol>
 *   <li>Validates the request (cron syntax, required fields)</li>
 *   <li>Writes to the DB via {@link JobRepository}</li>
 *   <li>Updates the in-memory {@link JobCache} so the change is immediately visible
 *       to the scheduler loop — no lag for user actions</li>
 * </ol>
 *
 * <p>Errors map to gRPC status codes as documented on {@link JobManagementService}.
 */
@Singleton
public class JobManagementServiceImpl implements JobManagementService {

    private final JobRepository  repository;
    private final JobCache       cache;
    private final CronEvaluator  cronEvaluator;

    @Inject
    public JobManagementServiceImpl(JobRepository repository,
                                    JobCache cache,
                                    CronEvaluator cronEvaluator) {
        this.repository    = repository;
        this.cache         = cache;
        this.cronEvaluator = cronEvaluator;
    }

    @Override
    public CreateJobResponse createJob(CreateJobRequest request) {
        // Validate
        if (request.getName().isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("name must not be blank")
                    .asRuntimeException();
        }
        try {
            cronEvaluator.validate(request.getCron());
        } catch (IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT.withDescription(e.getMessage())
                    .asRuntimeException();
        }
        if (request.getJobType() != com.github.asadyezdanschool.conductor.grpc.execution.JobType.HTTP) {
            throw Status.INVALID_ARGUMENT.withDescription("only HTTP job type is supported")
                    .asRuntimeException();
        }
        if (!request.hasHttpConfig()) {
            throw Status.INVALID_ARGUMENT.withDescription("http_config is required for HTTP jobs")
                    .asRuntimeException();
        }

        HttpJobConfig httpConfig = request.getHttpConfig();
        List<HttpHeader> headers = httpConfig.getHeadersList().stream()
                .map(h -> new HttpHeader(h.getName(), h.getValue()))
                .collect(Collectors.toList());
        JobRepository.CreateJobParams params = new JobRepository.CreateJobParams(
                request.getName(),
                request.getCron(),
                "HTTP",
                httpConfig.getUrl(),
                httpConfig.getMethod(),
                httpConfig.getPayload().isEmpty() ? null : httpConfig.getPayload(),
                headers,
                httpConfig.getTimeoutSeconds() == 0 ? 30 : httpConfig.getTimeoutSeconds()
        );

        try {
            JobRepository.CreatedJobResult result = repository.createJobDefinition(params);

            Instant nextScheduledAt = cronEvaluator.nextAfter(request.getCron(), Instant.now());
            CachedJob cached = new CachedJob(
                    result.definitionId(),
                    result.familyId(),
                    request.getName(),
                    request.getCron(),
                    JobType.HTTP,
                    3,
                    null,
                    nextScheduledAt
            );
            cache.put(cached);

            return CreateJobResponse.newBuilder()
                    .setJobFamilyId(result.familyId().toString())
                    .setJobDefinitionId(result.definitionId().toString())
                    .setVersion(result.version())
                    .build();
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("Failed to create job: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }

    @Override
    public EditJobResponse editJob(EditJobRequest request) {
        if (request.getJobFamilyId().isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("job_family_id must not be blank")
                    .asRuntimeException();
        }

        // Validate cron if present
        if (!request.getCron().isEmpty()) {
            try {
                cronEvaluator.validate(request.getCron());
            } catch (IllegalArgumentException e) {
                throw Status.INVALID_ARGUMENT.withDescription(e.getMessage())
                        .asRuntimeException();
            }
        }

        UUID familyId = UUID.fromString(request.getJobFamilyId());

        // Extract optional HTTP config fields
        String url = null, method = null, payload = null;
        List<HttpHeader> headers = null;
        Integer timeoutSeconds = null;
        if (request.getConfigCase() == EditJobRequest.ConfigCase.HTTP_CONFIG) {
            HttpEditConfig httpEdit = request.getHttpConfig();
            if (httpEdit.hasUrl())            url            = httpEdit.getUrl();
            if (httpEdit.hasMethod())         method         = httpEdit.getMethod();
            if (httpEdit.hasPayload())        payload        = httpEdit.getPayload();
            if (!httpEdit.getHeadersList().isEmpty()) {
                headers = httpEdit.getHeadersList().stream()
                        .map(h -> new HttpHeader(h.getName(), h.getValue()))
                        .collect(Collectors.toList());
            }
            if (httpEdit.hasTimeoutSeconds()) timeoutSeconds = httpEdit.getTimeoutSeconds();
        }

        JobRepository.EditJobParams params = new JobRepository.EditJobParams(
                request.getName().isEmpty()  ? null : request.getName(),
                request.getCron().isEmpty()  ? null : request.getCron(),
                url, method, payload, headers, timeoutSeconds
        );

        try {
            JobRepository.EditedJobResult result = repository.editJobDefinition(familyId, params);

            cache.remove(familyId);

            // Re-fetch the updated definition to populate cache
            JobRepository.ActiveJobRow row = repository.fetchActiveJob(familyId)
                    .orElseThrow(() -> Status.INTERNAL
                            .withDescription("Could not re-fetch job after edit").asRuntimeException());

            Instant nextScheduledAt = cronEvaluator.nextAfter(row.cron(), Instant.now());
            CachedJob cached = new CachedJob(
                    row.id(),
                    row.familyId(),
                    row.name(),
                    row.cron(),
                    JobType.fromString(row.jobType()),
                    row.maxRetries(),
                    row.lastTriggeredAt(),
                    nextScheduledAt
            );
            cache.put(cached);

            return EditJobResponse.newBuilder()
                    .setJobFamilyId(familyId.toString())
                    .setJobDefinitionId(result.newDefinitionId().toString())
                    .setVersion(result.newVersion())
                    .build();
        } catch (JobRepository.NotFoundException e) {
            throw Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("Failed to edit job: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }

    @Override
    public ParkJobResponse parkJob(ParkJobRequest request) {
        if (request.getJobFamilyId().isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("job_family_id must not be blank")
                    .asRuntimeException();
        }

        UUID familyId = UUID.fromString(request.getJobFamilyId());

        try {
            repository.setParkStatus(familyId, true);
            cache.remove(familyId);

            return ParkJobResponse.newBuilder()
                    .setJobFamilyId(familyId.toString())
                    .setIsParked(true)
                    .build();
        } catch (JobRepository.NotFoundException e) {
            throw Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
        } catch (JobRepository.ConflictException e) {
            throw Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("Failed to park job: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }

    @Override
    public UnparkJobResponse unparkJob(UnparkJobRequest request) {
        if (request.getJobFamilyId().isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("job_family_id must not be blank")
                    .asRuntimeException();
        }

        UUID familyId = UUID.fromString(request.getJobFamilyId());

        try {
            repository.setParkStatus(familyId, false);

            JobRepository.ActiveJobRow row = repository.fetchActiveJob(familyId)
                    .orElseThrow(() -> Status.INTERNAL
                            .withDescription("Could not re-fetch job after unpark").asRuntimeException());

            Instant nextScheduledAt = cronEvaluator.nextAfter(row.cron(), Instant.now());
            CachedJob cached = new CachedJob(
                    row.id(),
                    row.familyId(),
                    row.name(),
                    row.cron(),
                    JobType.fromString(row.jobType()),
                    row.maxRetries(),
                    row.lastTriggeredAt(),
                    nextScheduledAt
            );
            cache.put(cached);

            return UnparkJobResponse.newBuilder()
                    .setJobFamilyId(familyId.toString())
                    .setIsParked(false)
                    .build();
        } catch (JobRepository.NotFoundException e) {
            throw Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
        } catch (JobRepository.ConflictException e) {
            throw Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
        } catch (io.grpc.StatusRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("Failed to unpark job: " + e.getMessage())
                    .withCause(e).asRuntimeException();
        }
    }
}
