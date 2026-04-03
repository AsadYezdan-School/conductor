package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record CreateJobRequest(
        @JsonProperty("name") String name,
        @JsonProperty("cron") String cron,
        @JsonProperty("url") String url,
        @JsonProperty("method") String method,
        @JsonProperty("payload") Map<String, Object> payload,
        @JsonProperty("headers") Map<String, String> headers,
        @JsonProperty("timeoutSeconds") Integer timeoutSeconds
) {}