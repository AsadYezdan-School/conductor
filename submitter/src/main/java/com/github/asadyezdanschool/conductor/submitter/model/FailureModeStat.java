package com.github.asadyezdanschool.conductor.submitter.model;

public record FailureModeStat(
        String name,
        String jobFamilyId,
        Integer httpStatusCode,
        long occurrences,
        String lastSeenAt
) {}
