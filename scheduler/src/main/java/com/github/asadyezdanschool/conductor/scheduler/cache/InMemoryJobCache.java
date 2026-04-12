package com.github.asadyezdanschool.conductor.scheduler.cache;

import com.github.asadyezdanschool.conductor.scheduler.model.JobType;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import com.github.asadyezdanschool.conductor.scheduler.scheduling.CronEvaluator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe in-memory job cache backed by a {@link ConcurrentHashMap}.
 *
 * <h3>Startup</h3>
 * {@link #initialize()} must be called once before the scheduler loop starts. It loads all
 * active job definitions from the DB using the {@code idx_job_definitions_scheduler} index.
 *
 * <h3>Background safety-net refresh</h3>
 * A single-threaded executor refreshes the full cache from the DB every
 * {@value #REFRESH_INTERVAL_SECONDS} seconds. This catches edge cases where the cache
 * diverges from the DB (e.g. a crash mid-write). The refresh is additive for new jobs and
 * removes entries that are no longer active (parked / deleted / no longer latest).
 *
 * <h3>Immediate updates</h3>
 * gRPC handlers call {@link #put}/{@link #remove} directly after each DB write so the cache
 * reflects user actions with zero lag.
 */
@Singleton
public class InMemoryJobCache implements JobCache {

    private static final Logger log = Logger.getLogger(InMemoryJobCache.class.getName());
    static final int REFRESH_INTERVAL_SECONDS = 30;

    private final ConcurrentHashMap<UUID, CachedJob> map = new ConcurrentHashMap<>();
    private final JobRepository repository;
    private final CronEvaluator cronEvaluator;
    private final ScheduledExecutorService refreshExecutor;

    @Inject
    public InMemoryJobCache(JobRepository repository, CronEvaluator cronEvaluator) {
        this.repository    = repository;
        this.cronEvaluator = cronEvaluator;
        this.refreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-refresh");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Load all active jobs from the DB and start the background refresh.
     * Must be called before {@link #snapshot()} is used by the scheduler loop.
     */
    public void initialize() {
        try {
            List<JobRepository.ActiveJobRow> rows = repository.loadActiveJobs();
            for (JobRepository.ActiveJobRow row : rows) {
                map.put(row.familyId(), buildCachedJob(row));
            }
            log.info("Cache initialized with " + map.size() + " active jobs");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize job cache", e);
        }
        refreshExecutor.scheduleAtFixedRate(this::refresh,
                REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void shutdown() {
        refreshExecutor.shutdownNow();
    }

    // ── JobCache ─────────────────────────────────────────────────────────────

    @Override
    public void put(CachedJob job) {
        map.put(job.familyId(), job);
    }

    @Override
    public void remove(UUID familyId) {
        map.remove(familyId);
    }

    @Override
    public Optional<CachedJob> get(UUID familyId) {
        return Optional.ofNullable(map.get(familyId));
    }

    @Override
    public void markTriggered(UUID familyId, Instant firedAt, Instant nextScheduledAt) {
        map.compute(familyId, (id, existing) -> {
            if (existing == null) return null;
            return existing.withTriggered(firedAt, nextScheduledAt);
        });
    }

    @Override
    public Collection<CachedJob> snapshot() {
        return List.copyOf(map.values());
    }

    @Override
    public int size() {
        return map.size();
    }

    // ── private ──────────────────────────────────────────────────────────────

    /** Reconcile the cache with the current DB state. Called by the background refresh. */
    private void refresh() {
        try {
            List<JobRepository.ActiveJobRow> rows = repository.loadActiveJobs();
            Set<UUID> activeFamilyIds = ConcurrentHashMap.newKeySet();

            for (JobRepository.ActiveJobRow row : rows) {
                activeFamilyIds.add(row.familyId());
                // Only add if not already present — avoids overwriting in-memory markTriggered state
                map.computeIfAbsent(row.familyId(), id -> buildCachedJob(row));
            }

            // Remove entries no longer active in DB
            map.keySet().removeIf(id -> !activeFamilyIds.contains(id));

            log.fine("Cache refreshed: " + map.size() + " active jobs");
        } catch (Exception e) {
            log.log(Level.WARNING, "Background cache refresh failed", e);
        }
    }

    private CachedJob buildCachedJob(JobRepository.ActiveJobRow row) {
        Instant nextScheduledAt;
        if (row.nextScheduledAt() != null) {
            // Use DB-persisted next scheduled time if available
            nextScheduledAt = row.nextScheduledAt();
        } else {
            // Compute from cron + now for jobs that have never been evaluated
            nextScheduledAt = cronEvaluator.nextAfter(row.cron(), Instant.now());
        }
        return new CachedJob(
                row.id(),
                row.familyId(),
                row.name(),
                row.cron(),
                JobType.fromString(row.jobType()),
                row.maxRetries(),
                row.lastTriggeredAt(),
                nextScheduledAt
        );
    }
}
