package com.github.asadyezdanschool.conductor.submitter.model;

import java.util.List;

public record AlertBreach(
        String       jobFamilyId,
        String       name,
        Double       actualSuccessRatePct,
        Double       thresholdSuccessRatePct,
        Long         actualAvgDurationMs,
        Integer      thresholdAvgDurationMs,
        List<String> breachedFields,
        long         downstreamCount
) {}
