package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RunEvent(
        @JsonProperty("eventId")        String eventId,
        @JsonProperty("status")         String status,
        @JsonProperty("message")        String message,
        @JsonProperty("httpStatusCode") Integer httpStatusCode,
        @JsonProperty("responseBody")   String responseBody,
        @JsonProperty("occurredAt")     String occurredAt,
        @JsonProperty("source")         String source
) {}
