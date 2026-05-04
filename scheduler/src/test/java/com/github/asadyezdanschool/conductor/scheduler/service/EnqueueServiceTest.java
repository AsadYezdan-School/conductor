package com.github.asadyezdanschool.conductor.scheduler.service;

import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.model.JobType;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnqueueServiceTest {

    private JobRepository mockRepo;
    private SqsClient     mockSqs;
    private EnqueueService service;

    @BeforeEach
    void setUp() throws Exception {
        mockRepo = mock(JobRepository.class);
        mockSqs  = mock(SqsClient.class);
        service  = new EnqueueService(mockRepo, mockSqs, "https://sqs.us-east-1.amazonaws.com/test/queue");
        // Default: stub enqueueRun to return a new UUID so the happy path proceeds
        when(mockRepo.enqueueRun(any(), any(), any(), any())).thenReturn(UUID.randomUUID());
    }

    @Test
    void enqueueRun_depsMet_enqueuesToDbAndSqs() throws Exception {
        when(mockRepo.areAllUpstreamDepsSucceeded(any())).thenReturn(true);

        service.enqueueRun(dueJob(), Instant.now(), Instant.now().plusSeconds(60));

        verify(mockRepo).enqueueRun(any(), any(), any(), any());
        verify(mockSqs).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void enqueueRun_depsNotMet_skipsEnqueue() throws Exception {
        when(mockRepo.areAllUpstreamDepsSucceeded(any())).thenReturn(false);

        service.enqueueRun(dueJob(), Instant.now(), Instant.now().plusSeconds(60));

        verify(mockRepo, never()).enqueueRun(any(), any(), any(), any());
        verify(mockSqs, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void enqueueRun_depCheckThrowsSqlException_skipsEnqueueSafely() throws Exception {
        when(mockRepo.areAllUpstreamDepsSucceeded(any())).thenThrow(new SQLException("db error"));

        service.enqueueRun(dueJob(), Instant.now(), Instant.now().plusSeconds(60));

        verify(mockRepo, never()).enqueueRun(any(), any(), any(), any());
        verify(mockSqs, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void enqueueRun_noDependencies_proceeds() throws Exception {
        // A job with no deps still returns true from areAllUpstreamDepsSucceeded
        when(mockRepo.areAllUpstreamDepsSucceeded(any())).thenReturn(true);

        service.enqueueRun(dueJob(), Instant.now(), Instant.now().plusSeconds(60));

        verify(mockRepo).enqueueRun(any(), any(), any(), any());
        verify(mockSqs).sendMessage(any(SendMessageRequest.class));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CachedJob dueJob() {
        return new CachedJob(
                UUID.randomUUID(), UUID.randomUUID(),
                "test-job", "* * * * *", JobType.HTTP, 3,
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(1)
        );
    }
}
