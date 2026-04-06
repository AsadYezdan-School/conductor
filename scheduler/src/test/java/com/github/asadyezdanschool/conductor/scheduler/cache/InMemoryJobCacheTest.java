package com.github.asadyezdanschool.conductor.scheduler.cache;

import com.github.asadyezdanschool.conductor.scheduler.model.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryJobCacheTest {

    private InMemoryJobCache cache;

    @BeforeEach
    void setUp() {
        // TODO: inject a mock JobRepository so initialize() can be called
        //       cache = new InMemoryJobCache(mockRepo);
    }

    @Test
    void put_addsJobToCache() {
        // TODO: cache.put(buildJob()); assertEquals(1, cache.size())
    }

    @Test
    void put_replacesExistingJobForSameFamily() {
        // TODO: put job v1, put job v2 same familyId, assert size still 1 and get returns v2
    }

    @Test
    void remove_deletesJobByFamilyId() {
        // TODO: put then remove; assert size 0 and get returns empty
    }

    @Test
    void remove_noOpWhenAbsent() {
        // TODO: remove on empty cache should not throw
    }

    @Test
    void get_returnsEmptyForUnknownFamily() {
        // TODO: assertTrue(cache.get(UUID.randomUUID()).isEmpty())
    }

    @Test
    void markTriggered_updatesNextScheduledAt() {
        // TODO: put job; markTriggered(familyId, now, nextInstant);
        //       assert get(familyId).get().nextScheduledAt() == nextInstant
    }

    @Test
    void markTriggered_noOpWhenFamilyNotCached() {
        // TODO: markTriggered on unknown family should not throw and not alter cache
    }

    @Test
    void snapshot_returnsPointInTimeCopy() {
        // TODO: put 2 jobs; snapshot = cache.snapshot();
        //       remove one; assert snapshot still contains 2 (not affected by later mutation)
    }

    @Test
    void snapshot_isEmptyWhenCacheIsEmpty() {
        // TODO: assertTrue(cache.snapshot().isEmpty())
    }

    @Test
    void concurrentPutAndRemove_doesNotThrow() throws InterruptedException {
        // TODO: spawn 50 virtual threads alternating put/remove on same familyId, join all
        //       assert no exception thrown
    }

    @Test
    void backgroundRefresh_addsNewJobsFromDb() {
        // TODO: start cache, stub repo to return additional job on second call to loadActiveJobs,
        //       wait > REFRESH_INTERVAL_SECONDS, assert new job appears in cache
        //       (or trigger refresh directly via package-private method)
    }

    @Test
    void backgroundRefresh_removesParkedJobsFromCache() {
        // TODO: start with job in cache; stub repo so job no longer returned (simulating park);
        //       trigger refresh; assert job removed from cache
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CachedJob buildJob() {
        return new CachedJob(
                UUID.randomUUID(), UUID.randomUUID(),
                "test-job", "* * * * *", JobType.HTTP, 3,
                null, Instant.now().plusSeconds(60)
        );
    }
}
