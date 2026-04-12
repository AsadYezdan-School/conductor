package com.github.asadyezdanschool.conductor.scheduler.scheduling;

import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.cache.JobCache;
import com.github.asadyezdanschool.conductor.scheduler.model.JobType;
import com.github.asadyezdanschool.conductor.scheduler.service.EnqueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class SchedulerLoopTest {

    private JobCache       mockCache;
    private CronEvaluator  mockCronEvaluator;
    private EnqueueService mockEnqueueService;
    private SchedulerLoop  loop;

    @BeforeEach
    void setUp() {
        mockCache          = mock(JobCache.class);
        mockCronEvaluator  = mock(CronEvaluator.class);
        mockEnqueueService = mock(EnqueueService.class);
        loop = new SchedulerLoop(mockCache, mockCronEvaluator, mockEnqueueService);
    }

    @Test
    void tick_dueJob_callsMarkTriggeredBeforeEnqueue() {
        // TODO: stub cache.snapshot() with job whose nextScheduledAt is in the past
        //       stub cronEvaluator.nextAfter to return some future time
        //       loop.tick()
        //       verify: markTriggered called BEFORE enqueueService.enqueueRun
        //       Use InOrder mockito verification
    }

    @Test
    void tick_notYetDueJob_isSkipped() {
        // TODO: job.nextScheduledAt = Instant.now().plusSeconds(300)
        //       loop.tick()
        //       verify(mockEnqueueService, never()).enqueueRun(any(), any())
    }

    @Test
    void tick_emptyCache_doesNothing() {
        // TODO: cache.snapshot() returns empty list; tick should complete without error
    }

    @Test
    void tick_enqueueFailure_doesNotPropagateException() {
        // TODO: stub enqueueService.enqueueRun to throw RuntimeException
        //       loop.tick() should not throw — the virtual thread catches it
    }

    @Test
    void tick_multipleDueJobs_allGetMarkTriggered() {
        // TODO: snapshot returns 3 due jobs; verify markTriggered called 3 times
    }

    @Test
    void tick_jobDueExactlyNow_isFired() {
        // TODO: nextScheduledAt = Instant.now() (exactly); verify it fires
        //       (boundary condition: <= now, not <)
    }

    @Test
    void tick_nextScheduledAt_computedFromCurrentTime_notMissedSlot() {
        // TODO: verify that the `next` passed to markTriggered comes from
        //       cronEvaluator.nextAfter(cron, now) and NOT from an old scheduled time
        //       — this is what prevents catch-up firing
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CachedJob dueJob() {
        return new CachedJob(
                UUID.randomUUID(), UUID.randomUUID(),
                "due-job", "* * * * *", JobType.HTTP, 3,
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(1) // past → due
        );
    }

    private CachedJob futureJob() {
        return new CachedJob(
                UUID.randomUUID(), UUID.randomUUID(),
                "future-job", "0 0 * * *", JobType.HTTP, 3,
                null,
                Instant.now().plusSeconds(3600)
        );
    }
}
