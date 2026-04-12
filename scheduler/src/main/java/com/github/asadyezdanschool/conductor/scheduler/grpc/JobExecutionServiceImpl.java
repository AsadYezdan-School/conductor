package com.github.asadyezdanschool.conductor.scheduler.grpc;

import com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest;
import com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse;
import com.github.asadyezdanschool.conductor.grpc.execution.JobExecutionServiceGrpc;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest;
import com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse;
import com.github.asadyezdanschool.conductor.scheduler.model.HttpRunDetails;
import com.github.asadyezdanschool.conductor.scheduler.service.JobExecutionService;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * gRPC endpoint adapter for {@link JobExecutionService}.
 *
 * <p>Translates proto requests/responses to domain calls and handles
 * StreamObserver lifecycle. All business logic is in the service layer.
 */
@Singleton
public class JobExecutionServiceImpl extends JobExecutionServiceGrpc.JobExecutionServiceImplBase {

    private final JobExecutionService service;

    @Inject
    public JobExecutionServiceImpl(JobExecutionService service) {
        this.service = service;
    }

    @Override
    public void getHttpRunDetails(GetHttpRunDetailsRequest request,
                                  StreamObserver<GetHttpRunDetailsResponse> observer) {
        try {
            UUID runId = UUID.fromString(request.getJobRunId());
            HttpRunDetails details = service.getHttpRunDetails(runId);

            GetHttpRunDetailsResponse response = GetHttpRunDetailsResponse.newBuilder()
                    .setJobRunId(details.jobRunId().toString())
                    .setJobDefinitionId(details.jobDefinitionId().toString())
                    .setUrl(details.url())
                    .setMethod(details.method() != null ? details.method() : "")
                    .setPayload(details.payload() != null ? details.payload() : "")
                    .setHeaders(details.headers() != null ? details.headers() : "")
                    .setTimeoutSeconds(details.timeoutSeconds())
                    .setAttemptNumber(details.attemptNumber())
                    .setMaxRetries(details.maxRetries())
                    .build();

            observer.onNext(response);
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void reportStatus(ReportStatusRequest request,
                             StreamObserver<ReportStatusResponse> observer) {
        try {
            observer.onNext(service.reportStatus(request));
            observer.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        }
    }
}
