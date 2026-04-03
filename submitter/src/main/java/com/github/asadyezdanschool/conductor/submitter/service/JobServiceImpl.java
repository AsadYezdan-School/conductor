package com.github.asadyezdanschool.conductor.submitter.service;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import com.github.asadyezdanschool.conductor.submitter.repository.JobRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
public class JobServiceImpl implements JobService {

    private static final Set<String> VALID_METHODS =
            Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD");

    private final JobRepository repo;

    @Inject
    public JobServiceImpl(JobRepository repo) {
        this.repo = repo;
    }

    @Override
    public CreateJobResponse createJob(CreateJobRequest req) {
        validateCreate(req);
        try {
            return repo.createJob(req);
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating job", e);
        }
    }

    @Override
    public CreateJobResponse editJob(UUID familyId, EditJobRequest req) {
        validateEdit(req);
        try {
            return repo.editJob(familyId, req);
        } catch (SQLException e) {
            throw new RuntimeException("Database error editing job", e);
        }
    }

    @Override
    public ParkStatusResponse parkJob(UUID familyId) {
        try {
            repo.setParkStatus(familyId, true);
            return new ParkStatusResponse(familyId, true);
        } catch (SQLException e) {
            throw new RuntimeException("Database error parking job", e);
        }
    }

    @Override
    public ParkStatusResponse unparkJob(UUID familyId) {
        try {
            repo.setParkStatus(familyId, false);
            return new ParkStatusResponse(familyId, false);
        } catch (SQLException e) {
            throw new RuntimeException("Database error unparking job", e);
        }
    }

    // ── validation ────────────────────────────────────────────────────────────

    private void validateCreate(CreateJobRequest req) {
        List<String> errors = new ArrayList<>();
        if (req == null) {
            errors.add("request body is required");
            throw new ValidationException(errors);
        }
        if (req.name() == null || req.name().isBlank()) errors.add("name is required");
        if (req.cron() == null || req.cron().isBlank()) errors.add("cron is required");
        if (req.url() == null || req.url().isBlank()) {
            errors.add("url is required");
        } else if (!req.url().matches("https?://.*")) {
            errors.add("url must start with http:// or https://");
        }
        if (req.method() == null || req.method().isBlank()) {
            errors.add("method is required");
        } else if (!VALID_METHODS.contains(req.method().toUpperCase())) {
            errors.add("method must be one of: " + VALID_METHODS);
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private void validateEdit(EditJobRequest req) {
        List<String> errors = new ArrayList<>();
        if (req == null) {
            errors.add("request body is required");
            throw new ValidationException(errors);
        }
        if (req.url() != null && !req.url().isBlank() && !req.url().matches("https?://.*")) {
            errors.add("url must start with http:// or https://");
        }
        if (req.method() != null && !req.method().isBlank()
                && !VALID_METHODS.contains(req.method().toUpperCase())) {
            errors.add("method must be one of: " + VALID_METHODS);
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}