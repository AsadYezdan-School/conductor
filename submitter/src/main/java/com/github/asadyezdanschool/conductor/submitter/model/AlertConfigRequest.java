package com.github.asadyezdanschool.conductor.submitter.model;

public record AlertConfigRequest(
        Double  minSuccessRatePct,
        Integer maxAvgDurationMs
) {}
