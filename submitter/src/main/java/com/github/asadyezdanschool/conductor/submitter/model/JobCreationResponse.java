package com.github.asadyezdanschool.conductor.submitter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record JobCreationResponse(
        @JsonProperty("jobFamilyId") UUID jobFamilyId,
        @JsonProperty("jobDefinitionId") UUID jobDefinitionId,
        @JsonProperty("version") int version
) {}