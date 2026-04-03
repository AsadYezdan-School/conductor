package com.github.asadyezdanschool.conductor.submitter.service;

import com.github.asadyezdanschool.conductor.submitter.model.CreateJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;

import java.util.UUID;

public interface JobService {
    CreateJobResponse createJob(CreateJobRequest req);
    CreateJobResponse editJob(UUID familyId, EditJobRequest req);
    ParkStatusResponse parkJob(UUID familyId);
    ParkStatusResponse unparkJob(UUID familyId);
}