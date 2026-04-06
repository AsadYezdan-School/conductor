package com.github.asadyezdanschool.conductor.scheduler.scheduling;

import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.cache.JobCache;
import com.github.asadyezdanschool.conductor.scheduler.service.EnqueueService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main scheduling loop. Ticks every {@value #TICK_MS} ms, evaluates all cached jobs,
 * and fires virtual threads to enqueue jobs that are due.
 *
 * <h3>Double-fire prevention</h3>
 * Before spawning the enqueue thread, {@link JobCache#markTriggered} is called to advance
 * {@code nextScheduledAt} in-memory. This means even if the DB write takes longer than one
 * tick, the job will not be double-fired within the same scheduler process.
 *
 * <h3>Missed fires</h3>
 * If a fire time is missed (e.g. scheduler was down), it is skipped — the next scheduled
 * time is computed from the current wall clock, not the missed slot.
 */
@Singleton
public class SchedulerLoop {

    private static final Logger log = Logger.getLogger(SchedulerLoop.class.getName());
    static final long TICK_MS = 100;

    private final JobCache       cache;
    private final CronEvaluator  cronEvaluator;
    private final EnqueueService enqueueService;

    private volatile boolean running = false;

    @Inject
    public SchedulerLoop(JobCache cache, CronEvaluator cronEvaluator, EnqueueService enqueueService) {
        this.cache          = cache;
        this.cronEvaluator  = cronEvaluator;
        this.enqueueService = enqueueService;
    }

    /**
     * Start the loop on the calling thread (blocks until {@link #stop()} is called).
     * Intended to be called from a dedicated thread (e.g. a virtual thread in Main).
     */
    public void start() {
        running = true;
        log.info("Scheduler loop started (tick=" + TICK_MS + "ms)");
        while (running) {
            try {
                tick();
                Thread.sleep(TICK_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unexpected error in scheduler tick", e);
            }
        }
        log.info("Scheduler loop stopped");
    }

    public void stop() {
        running = false;
    }

    // ── package-private for testing ──────────────────────────────────────────

    /**
     * Execute a single evaluation pass over the cached jobs.
     * Exposed package-private so unit tests can drive individual ticks without
     * running the full blocking loop.
     */
    void tick() {
        Instant now = Instant.now();
        for (CachedJob job : cache.snapshot()) {
            if (job.nextScheduledAt() == null || !job.nextScheduledAt().isAfter(now)) {
                Instant next = cronEvaluator.nextAfter(job.cron(), now);
                cache.markTriggered(job.familyId(), now, next); // MUST happen before spawn
                Thread.ofVirtual().start(() -> enqueueService.enqueueRun(job, now, next));
            }
        }
    }
}
