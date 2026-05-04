package com.github.asadyezdanschool.conductor.submitter.model;

public record JobFamilyTrendBucket(
        String bucket,
        long   totalRuns,
        long   succeeded,
        long   failed,
        Long   avgDurationMs
) {}
