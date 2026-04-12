package com.github.asadyezdanschool.conductor.scheduler.cache;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory store of active job definitions keyed by {@code job_family_id}.
 *
 * <p>Implementations must be thread-safe: the scheduler loop reads the cache on every tick
 * while gRPC handlers and the background refresh thread write to it concurrently.
 */
public interface JobCache {

    /** Store or replace a job definition. Keyed by {@code job.familyId()}. */
    void put(CachedJob job);

    /** Remove a job family from the cache (e.g. after park or delete). No-op if absent. */
    void remove(UUID familyId);

    /** Look up the current cached state for a family, if present. */
    Optional<CachedJob> get(UUID familyId);

    /**
     * Atomically update a job's trigger timestamps after it has been fired.
     * Replaces the in-memory record; no-op if the family is no longer cached.
     */
    void markTriggered(UUID familyId, Instant firedAt, Instant nextScheduledAt);

    /**
     * Return a point-in-time snapshot of all cached jobs.
     * The returned collection is independent of the live map and safe to iterate
     * without holding any lock.
     */
    Collection<CachedJob> snapshot();

    /** Number of jobs currently held in the cache. */
    int size();
}
