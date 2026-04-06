package com.github.asadyezdanschool.conductor.grpc.management;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * JobManagementService is consumed exclusively by the Submitter REST service.
 * All job definition writes (create, edit, park, unpark) flow through here so
 * the scheduler remains the sole reader/writer of the database.
 * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
 * Error codes:
 *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
 *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
 *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
 *   INTERNAL          — unexpected server error (→ HTTP 500)
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class JobManagementServiceGrpc {

  private JobManagementServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "conductor.management.JobManagementService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> getCreateJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateJob",
      requestType = com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> getCreateJobMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest, com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> getCreateJobMethod;
    if ((getCreateJobMethod = JobManagementServiceGrpc.getCreateJobMethod) == null) {
      synchronized (JobManagementServiceGrpc.class) {
        if ((getCreateJobMethod = JobManagementServiceGrpc.getCreateJobMethod) == null) {
          JobManagementServiceGrpc.getCreateJobMethod = getCreateJobMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest, com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobManagementServiceMethodDescriptorSupplier("CreateJob"))
              .build();
        }
      }
    }
    return getCreateJobMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> getEditJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EditJob",
      requestType = com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> getEditJobMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest, com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> getEditJobMethod;
    if ((getEditJobMethod = JobManagementServiceGrpc.getEditJobMethod) == null) {
      synchronized (JobManagementServiceGrpc.class) {
        if ((getEditJobMethod = JobManagementServiceGrpc.getEditJobMethod) == null) {
          JobManagementServiceGrpc.getEditJobMethod = getEditJobMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest, com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EditJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobManagementServiceMethodDescriptorSupplier("EditJob"))
              .build();
        }
      }
    }
    return getEditJobMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> getParkJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ParkJob",
      requestType = com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> getParkJobMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest, com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> getParkJobMethod;
    if ((getParkJobMethod = JobManagementServiceGrpc.getParkJobMethod) == null) {
      synchronized (JobManagementServiceGrpc.class) {
        if ((getParkJobMethod = JobManagementServiceGrpc.getParkJobMethod) == null) {
          JobManagementServiceGrpc.getParkJobMethod = getParkJobMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest, com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ParkJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobManagementServiceMethodDescriptorSupplier("ParkJob"))
              .build();
        }
      }
    }
    return getParkJobMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> getUnparkJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnparkJob",
      requestType = com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest.class,
      responseType = com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest,
      com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> getUnparkJobMethod() {
    io.grpc.MethodDescriptor<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest, com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> getUnparkJobMethod;
    if ((getUnparkJobMethod = JobManagementServiceGrpc.getUnparkJobMethod) == null) {
      synchronized (JobManagementServiceGrpc.class) {
        if ((getUnparkJobMethod = JobManagementServiceGrpc.getUnparkJobMethod) == null) {
          JobManagementServiceGrpc.getUnparkJobMethod = getUnparkJobMethod =
              io.grpc.MethodDescriptor.<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest, com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnparkJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse.getDefaultInstance()))
              .setSchemaDescriptor(new JobManagementServiceMethodDescriptorSupplier("UnparkJob"))
              .build();
        }
      }
    }
    return getUnparkJobMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static JobManagementServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceStub>() {
        @java.lang.Override
        public JobManagementServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobManagementServiceStub(channel, callOptions);
        }
      };
    return JobManagementServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static JobManagementServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceBlockingV2Stub>() {
        @java.lang.Override
        public JobManagementServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobManagementServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return JobManagementServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static JobManagementServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceBlockingStub>() {
        @java.lang.Override
        public JobManagementServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobManagementServiceBlockingStub(channel, callOptions);
        }
      };
    return JobManagementServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static JobManagementServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JobManagementServiceFutureStub>() {
        @java.lang.Override
        public JobManagementServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JobManagementServiceFutureStub(channel, callOptions);
        }
      };
    return JobManagementServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Create a new job family at version 1.
     * </pre>
     */
    default void createJob(com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateJobMethod(), responseObserver);
    }

    /**
     * <pre>
     * Edit a job — inserts a new version and marks the previous one not-latest.
     * Only supplied fields are changed; omitted fields carry forward.
     * </pre>
     */
    default void editJob(com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEditJobMethod(), responseObserver);
    }

    /**
     * <pre>
     * Park a job — scheduler stops firing it; existing in-flight runs complete.
     * </pre>
     */
    default void parkJob(com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getParkJobMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unpark a job — scheduler resumes firing it on its cron schedule.
     * </pre>
     */
    default void unparkJob(com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnparkJobMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service JobManagementService.
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public static abstract class JobManagementServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return JobManagementServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service JobManagementService.
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public static final class JobManagementServiceStub
      extends io.grpc.stub.AbstractAsyncStub<JobManagementServiceStub> {
    private JobManagementServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobManagementServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobManagementServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new job family at version 1.
     * </pre>
     */
    public void createJob(com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateJobMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Edit a job — inserts a new version and marks the previous one not-latest.
     * Only supplied fields are changed; omitted fields carry forward.
     * </pre>
     */
    public void editJob(com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEditJobMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Park a job — scheduler stops firing it; existing in-flight runs complete.
     * </pre>
     */
    public void parkJob(com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getParkJobMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unpark a job — scheduler resumes firing it on its cron schedule.
     * </pre>
     */
    public void unparkJob(com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest request,
        io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnparkJobMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service JobManagementService.
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public static final class JobManagementServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<JobManagementServiceBlockingV2Stub> {
    private JobManagementServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobManagementServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobManagementServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new job family at version 1.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse createJob(com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Edit a job — inserts a new version and marks the previous one not-latest.
     * Only supplied fields are changed; omitted fields carry forward.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse editJob(com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getEditJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Park a job — scheduler stops firing it; existing in-flight runs complete.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse parkJob(com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getParkJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unpark a job — scheduler resumes firing it on its cron schedule.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse unparkJob(com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUnparkJobMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service JobManagementService.
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public static final class JobManagementServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<JobManagementServiceBlockingStub> {
    private JobManagementServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobManagementServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobManagementServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new job family at version 1.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse createJob(com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Edit a job — inserts a new version and marks the previous one not-latest.
     * Only supplied fields are changed; omitted fields carry forward.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse editJob(com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEditJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Park a job — scheduler stops firing it; existing in-flight runs complete.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse parkJob(com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getParkJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unpark a job — scheduler resumes firing it on its cron schedule.
     * </pre>
     */
    public com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse unparkJob(com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnparkJobMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service JobManagementService.
   * <pre>
   * JobManagementService is consumed exclusively by the Submitter REST service.
   * All job definition writes (create, edit, park, unpark) flow through here so
   * the scheduler remains the sole reader/writer of the database.
   * Default port: 50051  (env SCHEDULER_MANAGEMENT_GRPC_PORT)
   * Error codes:
   *   INVALID_ARGUMENT  — validation failure (→ HTTP 422)
   *   NOT_FOUND         — unknown job_family_id (→ HTTP 404)
   *   ALREADY_EXISTS    — park/unpark when already in that state (→ HTTP 409)
   *   INTERNAL          — unexpected server error (→ HTTP 500)
   * </pre>
   */
  public static final class JobManagementServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<JobManagementServiceFutureStub> {
    private JobManagementServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JobManagementServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JobManagementServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new job family at version 1.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse> createJob(
        com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateJobMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Edit a job — inserts a new version and marks the previous one not-latest.
     * Only supplied fields are changed; omitted fields carry forward.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse> editJob(
        com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEditJobMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Park a job — scheduler stops firing it; existing in-flight runs complete.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse> parkJob(
        com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getParkJobMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Unpark a job — scheduler resumes firing it on its cron schedule.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse> unparkJob(
        com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnparkJobMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_JOB = 0;
  private static final int METHODID_EDIT_JOB = 1;
  private static final int METHODID_PARK_JOB = 2;
  private static final int METHODID_UNPARK_JOB = 3;

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
        case METHODID_CREATE_JOB:
          serviceImpl.createJob((com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse>) responseObserver);
          break;
        case METHODID_EDIT_JOB:
          serviceImpl.editJob((com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse>) responseObserver);
          break;
        case METHODID_PARK_JOB:
          serviceImpl.parkJob((com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse>) responseObserver);
          break;
        case METHODID_UNPARK_JOB:
          serviceImpl.unparkJob((com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest) request,
              (io.grpc.stub.StreamObserver<com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse>) responseObserver);
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
          getCreateJobMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.management.CreateJobRequest,
              com.github.asadyezdanschool.conductor.grpc.management.CreateJobResponse>(
                service, METHODID_CREATE_JOB)))
        .addMethod(
          getEditJobMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.management.EditJobRequest,
              com.github.asadyezdanschool.conductor.grpc.management.EditJobResponse>(
                service, METHODID_EDIT_JOB)))
        .addMethod(
          getParkJobMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.management.ParkJobRequest,
              com.github.asadyezdanschool.conductor.grpc.management.ParkJobResponse>(
                service, METHODID_PARK_JOB)))
        .addMethod(
          getUnparkJobMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.github.asadyezdanschool.conductor.grpc.management.UnparkJobRequest,
              com.github.asadyezdanschool.conductor.grpc.management.UnparkJobResponse>(
                service, METHODID_UNPARK_JOB)))
        .build();
  }

  private static abstract class JobManagementServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    JobManagementServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.github.asadyezdanschool.conductor.grpc.management.JobManagementProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("JobManagementService");
    }
  }

  private static final class JobManagementServiceFileDescriptorSupplier
      extends JobManagementServiceBaseDescriptorSupplier {
    JobManagementServiceFileDescriptorSupplier() {}
  }

  private static final class JobManagementServiceMethodDescriptorSupplier
      extends JobManagementServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    JobManagementServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (JobManagementServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new JobManagementServiceFileDescriptorSupplier())
              .addMethod(getCreateJobMethod())
              .addMethod(getEditJobMethod())
              .addMethod(getParkJobMethod())
              .addMethod(getUnparkJobMethod())
              .build();
        }
      }
    }
    return result;
  }
}
