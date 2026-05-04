package com.github.asadyezdanschool.conductor.submitter.model;

public record JobHealthStat(
        String  name,
        String  jobFamilyId,
        String  cron,
        boolean isParked,
        long    totalRuns,
        long    succeeded,
        long    failed,
        Double  successRatePct,
        Long    avgDurationMs,
        Long    p50DurationMs,
        Long    p95DurationMs,
        Long    p99DurationMs
) {}
