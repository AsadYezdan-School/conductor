package com.github.asadyezdanschool.conductor.scheduler.grpc;

import com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.JobManagementServiceGrpc;
import com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse;
import com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest;
import com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse;
import com.github.asadyezdanschool.conductor.scheduler.service.JobManagementService;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * gRPC endpoint adapter for {@link JobManagementService}.
 *
 * <p>This class is intentionally thin — it translates gRPC mechanics (StreamObserver,
 * StatusRuntimeException propagation) and delegates all logic to the service layer.
 * Unhandled exceptions bubble up as {@code INTERNAL} gRPC errors.
 */
@Singleton
public class JobManagementServiceImpl extends JobManagementServiceGrpc.JobManagementServiceImplBase {

    private final JobManagementService service;

    @Inject
    public JobManagementServiceImpl(JobManagementService service) {
        this.service = service;
    }

    @Override
    public void createJob(CreateJobRequest request, StreamObserver<CreateJobResponse> observer) {
        try {
            observer.onNext(service.createJob(request));
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void editJob(EditJobRequest request, StreamObserver<EditJobResponse> observer) {
        try {
            observer.onNext(service.editJob(request));
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void parkJob(ParkJobRequest request, StreamObserver<ParkJobResponse> observer) {
        try {
            observer.onNext(service.parkJob(request));
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void unparkJob(UnparkJobRequest request, StreamObserver<UnparkJobResponse> observer) {
        try {
            observer.onNext(service.unparkJob(request));
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }
}
