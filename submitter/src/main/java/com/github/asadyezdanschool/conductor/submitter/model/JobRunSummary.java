package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobRunSummary(
        @JsonProperty("runId")          String runId,
        @JsonProperty("jobFamilyId")    String jobFamilyId,
        @JsonProperty("status")         String status,
        @JsonProperty("attemptNumber")  int attemptNumber,
        @JsonProperty("scheduledAt")    String scheduledAt,
        @JsonProperty("startedAt")      String startedAt,
        @JsonProperty("finishedAt")     String finishedAt,
        @JsonProperty("durationMs")     Long durationMs
) {}
