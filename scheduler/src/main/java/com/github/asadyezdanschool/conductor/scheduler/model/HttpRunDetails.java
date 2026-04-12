package com.github.asadyezdanschool.conductor.scheduler.model;

import java.util.UUID;

/**
 * The HTTP-specific details a worker needs to execute a queued job run.
 * Returned by {@link com.github.asadyezdanschool.conductor.scheduler.service.JobExecutionService#getHttpRunDetails}.
 */
public record HttpRunDetails(
        UUID   jobRunId,
        UUID   jobDefinitionId,
        UUID   jobFamilyId,
        String url,
        String method,
        /** JSON string; null if not set on the job definition. */
        String payload,
        /** JSON string; null if not set on the job definition. */
        String headers,
        int    timeoutSeconds,
        int    attemptNumber,
        int    maxRetries
) {}
