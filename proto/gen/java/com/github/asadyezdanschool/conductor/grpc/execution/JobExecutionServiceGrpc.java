package com.github.asadyezdanschool.conductor.grpc.execution;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * JobExecutionService is consumed exclusively by Worker processes.
 * The scheduler is the sole reader/writer of the database; workers never
 * touch the DB directly — all state transitions flow through this service.
 * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class JobExecutionServiceGrpc {

  private JobExecutionServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "conductor.execution.JobExecutionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest,
      com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> getGetHttpRunDetailsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetHttpRunDetails",
      requestType = com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest,
      com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> getGetHttpRunDetailsMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest, com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> getGetHttpRunDetailsMethod;
    if ((getGetHttpRunDetailsMethod = JobExecutionServiceGrpc.getGetHttpRunDetailsMethod) == null) {
      synchronized (JobExecutionServiceGrpc.class) {
        if ((getGetHttpRunDetailsMethod = JobExecutionServiceGrpc.getGetHttpRunDetailsMethod) == null) {
          JobExecutionServiceGrpc.getGetHttpRunDetailsMethod = getGetHttpRunDetailsMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest, com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetHttpRunDetails"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobExecutionServiceMethodDescriptorSupplier("GetHttpRunDetails"))
              .build();
        }
      }
    }
    return getGetHttpRunDetailsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest,
      com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> getReportStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportStatus",
      requestType = com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest,
      com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> getReportStatusMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest, com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> getReportStatusMethod;
    if ((getReportStatusMethod = JobExecutionServiceGrpc.getReportStatusMethod) == null) {
      synchronized (JobExecutionServiceGrpc.class) {
        if ((getReportStatusMethod = JobExecutionServiceGrpc.getReportStatusMethod) == null) {
          JobExecutionServiceGrpc.getReportStatusMethod = getReportStatusMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest, com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobExecutionServiceMethodDescriptorSupplier("ReportStatus"))
              .build();
        }
      }
    }
    return getReportStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static JobExecutionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceStub>() {
        @java.lang.Override
        public JobExecutionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobExecutionServiceStub(channel, callOptions);
        }
      };
    return JobExecutionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static JobExecutionServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceBlockingV2Stub>() {
        @java.lang.Override
        public JobExecutionServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobExecutionServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return JobExecutionServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static JobExecutionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceBlockingStub>() {
        @java.lang.Override
        public JobExecutionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobExecutionServiceBlockingStub(channel, callOptions);
        }
      };
    return JobExecutionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static JobExecutionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobExecutionServiceFutureStub>() {
        @java.lang.Override
        public JobExecutionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobExecutionServiceFutureStub(channel, callOptions);
        }
      };
    return JobExecutionServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Fetch the HTTP configuration needed to execute a queued run.
     * Called by the worker immediately after dequeuing an SQS message.
     * </pre>
     */
    default void getHttpRunDetails(com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetHttpRunDetailsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Report a status transition for a run (RUNNING → SUCCEEDED | FAILED).
     * The scheduler writes the transition to job_runs and job_run_events.
     * </pre>
     */
    default void reportStatus(com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service JobExecutionService.
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public static abstract class JobExecutionServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return JobExecutionServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service JobExecutionService.
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public static final class JobExecutionServiceStub
      extends io.grpc.stub.AbstractAsyncStub<JobExecutionServiceStub> {
    private JobExecutionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobExecutionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobExecutionServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fetch the HTTP configuration needed to execute a queued run.
     * Called by the worker immediately after dequeuing an SQS message.
     * </pre>
     */
    public void getHttpRunDetails(com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetHttpRunDetailsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Report a status transition for a run (RUNNING → SUCCEEDED | FAILED).
     * The scheduler writes the transition to job_runs and job_run_events.
     * </pre>
     */
    public void reportStatus(com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service JobExecutionService.
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public static final class JobExecutionServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<JobExecutionServiceBlockingV2Stub> {
    private JobExecutionServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobExecutionServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobExecutionServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Fetch the HTTP configuration needed to execute a queued run.
     * Called by the worker immediately after dequeuing an SQS message.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse getHttpRunDetails(com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetHttpRunDetailsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Report a status transition for a run (RUNNING → SUCCEEDED | FAILED).
     * The scheduler writes the transition to job_runs and job_run_events.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse reportStatus(com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getReportStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service JobExecutionService.
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public static final class JobExecutionServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<JobExecutionServiceBlockingStub> {
    private JobExecutionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobExecutionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobExecutionServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fetch the HTTP configuration needed to execute a queued run.
     * Called by the worker immediately after dequeuing an SQS message.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse getHttpRunDetails(com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetHttpRunDetailsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Report a status transition for a run (RUNNING → SUCCEEDED | FAILED).
     * The scheduler writes the transition to job_runs and job_run_events.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse reportStatus(com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service JobExecutionService.
   * <pre>
   * JobExecutionService is consumed exclusively by Worker processes.
   * The scheduler is the sole reader/writer of the database; workers never
   * touch the DB directly — all state transitions flow through this service.
   * Default port: 50052  (env SCHEDULER_EXECUTION_GRPC_PORT)
   * </pre>
   */
  public static final class JobExecutionServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<JobExecutionServiceFutureStub> {
    private JobExecutionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobExecutionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobExecutionServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fetch the HTTP configuration needed to execute a queued run.
     * Called by the worker immediately after dequeuing an SQS message.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse> getHttpRunDetails(
        com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetHttpRunDetailsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Report a status transition for a run (RUNNING → SUCCEEDED | FAILED).
     * The scheduler writes the transition to job_runs and job_run_events.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse> reportStatus(
        com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_HTTP_RUN_DETAILS = 0;
  private static final int METHODID_REPORT_STATUS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_HTTP_RUN_DETAILS:
          serviceImpl.getHttpRunDetails((com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse>) responseObserver);
          break;
        case METHODID_REPORT_STATUS:
          serviceImpl.reportStatus((com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetHttpRunDetailsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsRequest,
              com.github.asadyezdanschool.conductor.grpc.execution.GetHttpRunDetailsResponse>(
                service, METHODID_GET_HTTP_RUN_DETAILS)))
        .addMethod(
          getReportStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusRequest,
              com.github.asadyezdanschool.conductor.grpc.execution.ReportStatusResponse>(
                service, METHODID_REPORT_STATUS)))
        .build();
  }

  private static abstract class JobExecutionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    JobExecutionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.github.asadyezdanschool.conductor.grpc.execution.JobExecutionProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("JobExecutionService");
    }
  }

  private static final class JobExecutionServiceFileDescriptorSupplier
      extends JobExecutionServiceBaseDescriptorSupplier {
    JobExecutionServiceFileDescriptorSupplier() {}
  }

  private static final class JobExecutionServiceMethodDescriptorSupplier
      extends JobExecutionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    JobExecutionServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (JobExecutionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new JobExecutionServiceFileDescriptorSupplier())
              .addMethod(getGetHttpRunDetailsMethod())
              .addMethod(getReportStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
