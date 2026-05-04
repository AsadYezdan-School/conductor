package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record JobDetail(
        @JsonProperty("jobFamilyId")     String jobFamilyId,
        @JsonProperty("jobDefinitionId") String jobDefinitionId,
        @JsonProperty("version")         int version,
        @JsonProperty("name")            String name,
        @JsonProperty("cron")            String cron,
        @JsonProperty("jobType")         String jobType,
        @JsonProperty("isParked")        boolean isParked,
        @JsonProperty("maxRetries")      int maxRetries,
        @JsonProperty("createdAt")       String createdAt,
        @JsonProperty("nextScheduledAt") String nextScheduledAt,
        @JsonProperty("lastTriggeredAt") String lastTriggeredAt,
        @JsonProperty("url")             String url,
        @JsonProperty("method")          String method,
        @JsonProperty("payload")         String payload,
        @JsonProperty("headers")         Map<String, String> headers,
        @JsonProperty("timeoutSeconds")  int timeoutSeconds
) {}
