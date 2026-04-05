package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ParkStatusResponse(
        @JsonProperty("jobFamilyId") UUID jobFamilyId,
        @JsonProperty("isParked") boolean isParked
) {}