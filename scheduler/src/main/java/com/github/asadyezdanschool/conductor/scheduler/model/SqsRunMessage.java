package com.github.asadyezdanschool.conductor.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON payload placed on SQS by the scheduler when a job run is queued.
 *
 * <p>Format: {@code {"jobRunId":"<uuid>","jobType":"HTTP"}}
 *
 * <p>Workers parse this message to determine which processor handles the run
 * ({@code jobType}) and which gRPC call to make ({@code jobRunId}).
 */
public record SqsRunMessage(
        @JsonProperty("jobRunId") String jobRunId,
        @JsonProperty("jobType")  String jobType
) {}
