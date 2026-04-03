package com.github.asadyezdanschool.conductor.submitter.unit;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.CreateJobResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import com.github.asadyezdanschool.conductor.submitter.repository.JobRepository;
import com.github.asadyezdanschool.conductor.submitter.service.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceTest {

    private JobRepository mockRepo;
    private JobServiceImpl service;

    @BeforeEach
    void setUp() {
        mockRepo = mock(JobRepository.class);
        service = new JobServiceImpl(mockRepo);
    }

    // ── createJob ─────────────────────────────────────────────────────────────

    @Test
    void createJob_validRequest_delegatesToRepository() throws SQLException {
        CreateJobRequest req = new CreateJobRequest(
                "my-job", "* * * * *", "https://example.com", "GET",
                null, null, null);
        CreateJobResponse expected = new CreateJobResponse(UUID.randomUUID(), UUID.randomUUID(), 1);
        when(mockRepo.createJob(req)).thenReturn(expected);

        CreateJobResponse actual = service.createJob(req);

        assertEquals(expected, actual);
        verify(mockRepo, times(1)).createJob(req);
    }

    @Test
    void createJob_missingName_throwsValidationException() {
        CreateJobRequest req = new CreateJobRequest(
                null, "* * * * *", "https://example.com", "GET",
                null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createJob(req));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("name")));
    }

    @Test
    void createJob_missingCron_throwsValidationException() {
        CreateJobRequest req = new CreateJobRequest(
                "job", "", "https://example.com", "GET",
                null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createJob(req));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("cron")));
    }

    @Test
    void createJob_invalidUrl_throwsValidationException() {
        CreateJobRequest req = new CreateJobRequest(
                "job", "* * * * *", "ftp://example.com", "GET",
                null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createJob(req));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("url")));
    }

    @Test
    void createJob_invalidMethod_throwsValidationException() {
        CreateJobRequest req = new CreateJobRequest(
                "job", "* * * * *", "https://example.com", "JUMP",
                null, null, null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createJob(req));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("method")));
    }

    @Test
    void createJob_allMethodsAccepted() throws SQLException {
        CreateJobResponse response = new CreateJobResponse(UUID.randomUUID(), UUID.randomUUID(), 1);
        when(mockRepo.createJob(any())).thenReturn(response);

        for (String method : new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"}) {
            CreateJobRequest req = new CreateJobRequest(
                    "job", "* * * * *", "https://example.com", method,
                    null, null, null);
            assertDoesNotThrow(() -> service.createJob(req), "method " + method + " should be valid");
        }
    }

    // ── editJob ───────────────────────────────────────────────────────────────

    @Test
    void editJob_invalidUrl_throwsValidationException() {
        EditJobRequest req = new EditJobRequest(
                null, null, "not-a-url", null, null, null, null);

        assertThrows(ValidationException.class, () -> service.editJob(UUID.randomUUID(), req));
    }

    @Test
    void editJob_validPartialRequest_delegatesToRepository() throws SQLException {
        UUID familyId = UUID.randomUUID();
        EditJobRequest req = new EditJobRequest(
                "new-name", null, null, null, null, null, null);
        CreateJobResponse expected = new CreateJobResponse(familyId, UUID.randomUUID(), 2);
        when(mockRepo.editJob(familyId, req)).thenReturn(expected);

        CreateJobResponse actual = service.editJob(familyId, req);

        assertEquals(expected, actual);
    }

    // ── park/unpark ───────────────────────────────────────────────────────────

    @Test
    void parkJob_delegatesToRepository() throws SQLException {
        UUID familyId = UUID.randomUUID();
        doNothing().when(mockRepo).setParkStatus(familyId, true);

        ParkStatusResponse result = service.parkJob(familyId);

        assertEquals(familyId, result.jobFamilyId());
        assertTrue(result.isParked());
        verify(mockRepo).setParkStatus(familyId, true);
    }

    @Test
    void unparkJob_delegatesToRepository() throws SQLException {
        UUID familyId = UUID.randomUUID();
        doNothing().when(mockRepo).setParkStatus(familyId, false);

        ParkStatusResponse result = service.unparkJob(familyId);

        assertEquals(familyId, result.jobFamilyId());
        assertFalse(result.isParked());
        verify(mockRepo).setParkStatus(familyId, false);
    }
}