package com.github.asadyezdanschool.conductor.submitter.model;

import java.util.List;

public record JobDependenciesResponse(
        List<JobRef> upstreams,
        List<JobRef> downstreams
) {}
