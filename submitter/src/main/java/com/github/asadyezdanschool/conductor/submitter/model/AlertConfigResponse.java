package com.github.asadyezdanschool.conductor.submitter.model;

public record AlertConfigResponse(
        String  jobFamilyId,
        Double  minSuccessRatePct,
        Integer maxAvgDurationMs,
        String  updatedAt
) {}
