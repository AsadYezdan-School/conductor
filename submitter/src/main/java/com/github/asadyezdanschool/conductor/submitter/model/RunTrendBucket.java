package com.github.asadyezdanschool.conductor.submitter.model;

public record RunTrendBucket(
        String bucket,
        long totalRuns,
        long succeeded,
        long failed,
        Long avgDurationMs
) {}
