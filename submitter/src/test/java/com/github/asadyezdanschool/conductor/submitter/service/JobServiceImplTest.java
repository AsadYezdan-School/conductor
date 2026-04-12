package com.github.asadyezdanschool.conductor.submitter.service;

import com.github.asadyezdanschool.conductor.grpc.management.*;
import com.github.asadyezdanschool.conductor.submitter.exception.ConflictException;
import com.github.asadyezdanschool.conductor.submitter.exception.NotFoundException;
import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.grpc.SchedulerGrpcClient;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceImplTest {

    private JobManagementServiceGrpc.JobManagementServiceBlockingStub mockStub;
    private SchedulerGrpcClient mockClient;
    private JobServiceImpl service;

    @BeforeEach
    void setUp() {
        mockStub   = mock(JobManagementServiceGrpc.JobManagementServiceBlockingStub.class);
        mockClient = mock(SchedulerGrpcClient.class);
        when(mockClient.stub()).thenReturn(mockStub);
        service = new JobServiceImpl(mockClient);
    }

    // ── createJob ─────────────────────────────────────────────────────────────

    @Test
    void createJob_delegatesToGrpcStub_andReturnsResponse() {
        // TODO: stub mockStub.createJob(any()) to return a CreateJobResponse proto
        //       service.createJob(validCreationRequest())
        //       verify mockStub.createJob called once
        //       assert returned JobCreationResponse has correct familyId/definitionId/version
    }

    @Test
    void createJob_grpcInvalidArgument_throwsValidationException() {
        // TODO: mockStub.createJob throws Status.INVALID_ARGUMENT.asRuntimeException()
        //       assertThrows(ValidationException.class, () -> service.createJob(req))
    }

    @Test
    void createJob_grpcInternal_throwsRuntimeException() {
        // TODO: INTERNAL status → RuntimeException (not a domain exception)
    }

    // ── editJob ───────────────────────────────────────────────────────────────

    @Test
    void editJob_delegatesToGrpcStub() {
        // TODO: stub mockStub.editJob; verify called with correct job_family_id
    }

    @Test
    void editJob_grpcNotFound_throwsNotFoundException() {
        // TODO: NOT_FOUND → NotFoundException
    }

    // ── parkJob ───────────────────────────────────────────────────────────────

    @Test
    void parkJob_delegatesToGrpcStub() {
        // TODO: stub mockStub.parkJob; verify call; assert ParkStatusResponse(familyId, true)
    }

    @Test
    void parkJob_grpcAlreadyExists_throwsConflictException() {
        // TODO: ALREADY_EXISTS → ConflictException
    }

    @Test
    void parkJob_grpcNotFound_throwsNotFoundException() {
        // TODO: NOT_FOUND → NotFoundException
    }

    // ── unparkJob ─────────────────────────────────────────────────────────────

    @Test
    void unparkJob_delegatesToGrpcStub() {
        // TODO: verify UnparkJobRequest has correct familyId; assert ParkStatusResponse(familyId, false)
    }

    @Test
    void unparkJob_grpcAlreadyExists_throwsConflictException() {
        // TODO: ALREADY_EXISTS → ConflictException
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JobCreationRequest validCreationRequest() {
        return new JobCreationRequest("test-job", "*/5 * * * *",
                "http://example.com", "POST", null, null, 30);
    }
}
