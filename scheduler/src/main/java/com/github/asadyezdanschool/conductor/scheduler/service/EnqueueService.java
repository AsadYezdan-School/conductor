package com.github.asadyezdanschool.conductor.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.asadyezdanschool.conductor.scheduler.cache.CachedJob;
import com.github.asadyezdanschool.conductor.scheduler.model.SqsRunMessage;
import com.github.asadyezdanschool.conductor.scheduler.repository.JobRepository;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the atomic act of persisting a new job run and dispatching it to SQS.
 *
 * <p>Called by {@link com.github.asadyezdanschool.conductor.scheduler.scheduling.SchedulerLoop}
 * on a fresh virtual thread per invocation. Each call:
 * <ol>
 *   <li>Inserts a row in {@code job_runs} (status=QUEUED)</li>
 *   <li>Inserts a row in {@code job_run_events} (status=QUEUED, source='scheduler')</li>
 *   <li>Upserts {@code job_schedules} with the new {@code last_triggered_at} and
 *       {@code next_scheduled_at}</li>
 *   <li>Sends a JSON SQS message: {@code {"jobRunId":"<uuid>","jobType":"HTTP"}}</li>
 * </ol>
 * Steps 1–3 are wrapped in a single JDBC transaction; SQS is sent after the commit.
 *
 * <p>Failures are logged but do not crash the scheduler loop — the cache entry already
 * has an updated {@code nextScheduledAt}, so the job will fire again on its next due time.
 */
@Singleton
public class EnqueueService {

    private static final Logger log = Logger.getLogger(EnqueueService.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Limits how many virtual threads can concurrently hold a DB connection for enqueue.
     *  Keeps the pool available for gRPC handlers even under burst conditions. */
    private static final int MAX_CONCURRENT_ENQUEUES = 10;
    private final Semaphore enqueueSemaphore = new Semaphore(MAX_CONCURRENT_ENQUEUES);

    private final JobRepository repository;
    private final SqsClient sqsClient;
    private final String queueUrl;

    @Inject
    public EnqueueService(
            JobRepository repository,
            SqsClient sqsClient,
            @Named("sqsQueueUrl") String queueUrl) {
        this.repository = repository;
        this.sqsClient  = sqsClient;
        this.queueUrl   = queueUrl;
    }

    private static final int MAX_ATTEMPTS = 3;

    /**
     * Persist and dispatch a single job run.
     *
     * @param job               the cached job definition to fire
     * @param firedAt           the instant at which the scheduler decided to fire this job
     * @param nextScheduledAt   the next future fire time (already computed by the loop)
     */
    public void enqueueRun(CachedJob job, Instant firedAt, Instant nextScheduledAt) {
        try {
            enqueueSemaphore.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warning("Enqueue interrupted waiting for semaphore: " + job.definitionId());
            return;
        }
        try {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                try {
                    if (attempt > 1) {
                        log.warning("Retrying enqueue for definition " + job.definitionId()
                                + " (attempt " + attempt + "/" + MAX_ATTEMPTS + ")");
                        Thread.sleep(1_000L * (attempt - 1));
                    }

                    UUID runId = repository.enqueueRun(
                            job.definitionId(), job.familyId(), firedAt, nextScheduledAt);

                    SqsRunMessage message = new SqsRunMessage(runId.toString(), job.jobType().name());
                    String jsonBody = mapper.writeValueAsString(message);

                    sqsClient.sendMessage(SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(jsonBody)
                            .build());

                    log.info("Queued run: " + runId + " for definition: " + job.definitionId()
                            + " (next=" + nextScheduledAt + ")");
                    return;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warning("Enqueue interrupted for definition " + job.definitionId());
                    return;
                } catch (Exception e) {
                    log.log(Level.SEVERE,
                            "Failed to enqueue run for definition " + job.definitionId()
                                    + " (attempt " + attempt + "/" + MAX_ATTEMPTS + ")", e);
                }
            }
            log.severe("Giving up enqueue for definition " + job.definitionId()
                    + " after " + MAX_ATTEMPTS + " attempts");
        } finally {
            enqueueSemaphore.release();
        }
    }
}
