package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobSummary(
        @JsonProperty("jobFamilyId")     String jobFamilyId,
        @JsonProperty("jobDefinitionId") String jobDefinitionId,
        @JsonProperty("version")         int version,
        @JsonProperty("name")            String name,
        @JsonProperty("cron")            String cron,
        @JsonProperty("jobType")         String jobType,
        @JsonProperty("isParked")        boolean isParked,
        @JsonProperty("nextScheduledAt") String nextScheduledAt,
        @JsonProperty("lastTriggeredAt") String lastTriggeredAt,
        @JsonProperty("latestRunStatus") String latestRunStatus
) {}
