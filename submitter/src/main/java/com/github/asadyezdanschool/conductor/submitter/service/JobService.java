package com.github.asadyezdanschool.conductor.submitter.service;

import com.github.asadyezdanschool.conductor.submitter.model.JobCreationRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;

import java.util.UUID;

public interface JobService {
    JobCreationResponse createJob(JobCreationRequest req);
    JobCreationResponse editJob(UUID familyId, EditJobRequest req);
    ParkStatusResponse parkJob(UUID familyId);
    ParkStatusResponse unparkJob(UUID familyId);
}