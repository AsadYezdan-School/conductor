package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.grpc.execution.JobType;
import com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.HttpEditConfig;
import com.github.asadyezdanschool.conductor.grpc.management.HttpJobConfig;
import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.cache.JobCache;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import com.github.asadyezdanschool.conductor.scheduler.scheduling.CronEvaluator;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static io.grpc.Status.Code.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JobManagementServiceImpl}.
 *
 * <p>The repository is mocked — "assert things exist in the DB" is expressed here as
 * verifying that the repo was called with the exact parameters we expect (argument capture).
 * Full DB-state assertions live in {@link com.github.asadyezdanschool.conductor.scheduler.integration.ManagementGrpcServerIntegrationTest}.
 */
class JobManagementServiceImplTest {

    private JobRepository            mockRepo;
    private JobCache                 mockCache;
    private CronEvaluator            mockCronEvaluator;
    private JobManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        mockRepo          = mock(JobRepository.class);
        mockCache         = mock(JobCache.class);
        mockCronEvaluator = mock(CronEvaluator.class);
        service = new JobManagementServiceImpl(mockRepo, mockCache, mockCronEvaluator);
    }

    // ── createJob ─────────────────────────────────────────────────────────────

    @Test
    void createJob_validRequest_callsRepoWithCorrectParams() {
        // TODO: stub repo.createJobDefinition to return a CreatedJobResult
        //       stub cronEvaluator.nextAfter to return a future Instant
        //       service.createJob(validRequest())
        //
        //       ArgumentCaptor<JobRepository.CreateJobParams> captor = ArgumentCaptor.forClass(...)
        //       verify(mockRepo).createJobDefinition(captor.capture())
        //       JobRepository.CreateJobParams params = captor.getValue()
        //       assertEquals("my-job",                params.name())
        //       assertEquals("*/5 * * * *",           params.cron())
        //       assertEquals("HTTP",                  params.jobType())
        //       assertEquals("http://example.com/hook", params.url())
        //       assertEquals("POST",                  params.method())
        //       assertEquals(30,                      params.timeoutSeconds())
    }

    @Test
    void createJob_validRequest_putsCachedJobWithCorrectFields() {
        // TODO: after createJob succeeds:
        //       ArgumentCaptor<CachedJob> captor = ArgumentCaptor.forClass(CachedJob.class)
        //       verify(mockCache).put(captor.capture())
        //       CachedJob cached = captor.getValue()
        //       assertEquals(createdDefinitionId, cached.definitionId())
        //       assertEquals(createdFamilyId,     cached.familyId())
        //       assertEquals("*/5 * * * *",       cached.cron())
        //       assertEquals(JobType.HTTP,         cached.jobType())
        //       assertNotNull(cached.nextScheduledAt())
        //       assertNull(cached.lastTriggeredAt())   // new job, never run
    }

    @Test
    void createJob_emptyName_throwsInvalidArgument_noDbWrite() {
        // TODO: CreateJobRequest with blank name
        //       StatusRuntimeException ex = assertThrows(...)
        //       assertEquals(INVALID_ARGUMENT, ex.getStatus().getCode())
        //       verify(mockRepo, never()).createJobDefinition(any())   // no DB write
        //       verify(mockCache, never()).put(any())                  // no cache mutation
    }

    @Test
    void createJob_invalidCron_throwsInvalidArgument_noDbWrite() {
        // TODO: cronEvaluator.validate throws IllegalArgumentException
        //       assert INVALID_ARGUMENT status
        //       verify(mockRepo, never()).createJobDefinition(any())
        //       verify(mockCache, never()).put(any())
    }

    @Test
    void createJob_missingHttpConfig_throwsInvalidArgument_noDbWrite() {
        // TODO: request with job_type=HTTP but no http_config set
        //       assert INVALID_ARGUMENT
        //       verify(mockRepo, never()).createJobDefinition(any())
    }

    // ── editJob ───────────────────────────────────────────────────────────────

    @Test
    void editJob_validRequest_callsRepoWithCorrectParams() {
        // TODO: stub repo.editJobDefinition, repo.fetchActiveJob
        //       service.editJob(editRequestWithNewUrl())
        //
        //       ArgumentCaptor<JobRepository.EditJobParams> captor = ArgumentCaptor.forClass(...)
        //       verify(mockRepo).editJobDefinition(eq(familyId), captor.capture())
        //       assertEquals("http://new.com", captor.getValue().url())
        //       // name was omitted → carry-forward → repo receives null name
        //       assertNull(captor.getValue().name())
    }

    @Test
    void editJob_validRequest_replacesExistingCacheEntry() {
        // TODO: stub repo; service.editJob(...)
        //       InOrder order = inOrder(mockCache)
        //       order.verify(mockCache).remove(familyId)
        //       order.verify(mockCache).put(any(CachedJob.class))
        //       (remove must happen before put to avoid a brief window with stale data)
    }

    @Test
    void editJob_updatedCachedJob_hasCorrectDefinitionIdAndCron() {
        // TODO: after editJob, capture the CachedJob passed to cache.put
        //       assert it has the NEW definitionId (not the old one)
        //       assert cron matches the (possibly updated) cron from the response
    }

    @Test
    void editJob_carryForwardOmittedFields_passesNullsToRepo() {
        // TODO: EditJobRequest with only name set (url, cron, method omitted)
        //       capture EditJobParams; assert url==null, cron==null, method==null
    }

    @Test
    void editJob_unknownFamily_throwsNotFound_noCacheMutation() {
        // TODO: repo.editJobDefinition throws NotFoundException
        //       assert NOT_FOUND status
        //       verify(mockCache, never()).remove(any())
        //       verify(mockCache, never()).put(any())
    }

    @Test
    void editJob_emptyFamilyId_throwsInvalidArgument_noDbWrite() {
        // TODO: EditJobRequest with blank job_family_id
        //       verify(mockRepo, never()).editJobDefinition(any(), any())
    }

    // ── parkJob ───────────────────────────────────────────────────────────────

    @Test
    void parkJob_success_callsRepoWithParkTrueAndRemovesFromCache() {
        // TODO: service.parkJob(parkRequest(familyId))
        //       verify(mockRepo).setParkStatus(eq(familyId), eq(true))
        //       verify(mockCache).remove(eq(familyId))
        //       InOrder: repo write must happen before cache remove
    }

    @Test
    void parkJob_alreadyParked_throwsAlreadyExists_noCacheMutation() {
        // TODO: repo.setParkStatus throws ConflictException
        //       assert ALREADY_EXISTS
        //       verify(mockCache, never()).remove(any())
    }

    @Test
    void parkJob_unknownFamily_throwsNotFound_noCacheMutation() {
        // TODO: repo.setParkStatus throws NotFoundException → NOT_FOUND
        //       verify(mockCache, never()).remove(any())
    }

    // ── unparkJob ─────────────────────────────────────────────────────────────

    @Test
    void unparkJob_success_callsRepoWithParkFalseAndAddsToCache() {
        // TODO: stub repo.setParkStatus (no throw), repo.fetchActiveJob returns a row
        //       service.unparkJob(unparkRequest(familyId))
        //       verify(mockRepo).setParkStatus(eq(familyId), eq(false))
        //       verify(mockCache).put(any(CachedJob.class))
    }

    @Test
    void unparkJob_success_cachedJobHasCorrectFields() {
        // TODO: capture the CachedJob passed to cache.put
        //       assert definitionId, familyId, cron match what fetchActiveJob returned
        //       assert nextScheduledAt is in the future (computed from cron + now)
    }

    @Test
    void unparkJob_alreadyUnparked_throwsAlreadyExists_noCacheMutation() {
        // TODO: repo.setParkStatus throws ConflictException → ALREADY_EXISTS
        //       verify(mockCache, never()).put(any())
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateJobRequest validRequest() {
        return CreateJobRequest.newBuilder()
                .setName("my-job")
                .setCron("*/5 * * * *")
                .setJobType(JobType.HTTP)
                .setHttpConfig(HttpJobConfig.newBuilder()
                        .setUrl("http://example.com/hook")
                        .setMethod("POST")
                        .setTimeoutSeconds(30)
                        .build())
                .build();
    }

    private EditJobRequest editRequestWithNewUrl(UUID familyId) {
        return EditJobRequest.newBuilder()
                .setJobFamilyId(familyId.toString())
                .setHttpConfig(HttpEditConfig.newBuilder()
                        .setUrl("http://new.com")
                        .build())
                .build();
    }
}
