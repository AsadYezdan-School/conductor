package com.github.asadyezdanschool.conductor.scheduler.cache;

import com.github.asadyezdanschool.conductor.scheduler.model.JobType;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight in-memory representation of an active (non-parked, non-deleted, latest-version)
 * job definition held in {@link InMemoryJobCache}.
 *
 * <p>{@code lastTriggeredAt} is {@code null} for jobs that have never been triggered.
 * {@code nextScheduledAt} is always non-null once a job is in the cache — it is computed
 * from the cron expression at insertion time (or loaded from {@code job_schedules} on startup).
 */
public record CachedJob(
        UUID    definitionId,
        UUID    familyId,
        String  name,
        String  cron,
        JobType jobType,
        int     maxRetries,
        /** Null if the job has never been triggered. */
        Instant lastTriggeredAt,
        /** Next scheduled fire time — never null once in cache. */
        Instant nextScheduledAt
) {
    /** Returns a copy of this record with an updated {@code nextScheduledAt}. */
    public CachedJob withNextScheduledAt(Instant next) {
        return new CachedJob(definitionId, familyId, name, cron, jobType, maxRetries, lastTriggeredAt, next);
    }

    /** Returns a copy with both trigger timestamps updated after a successful fire. */
    public CachedJob withTriggered(Instant firedAt, Instant next) {
        return new CachedJob(definitionId, familyId, name, cron, jobType, maxRetries, firedAt, next);
    }
}
