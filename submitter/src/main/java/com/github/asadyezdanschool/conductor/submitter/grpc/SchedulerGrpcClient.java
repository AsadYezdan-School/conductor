package com.github.asadyezdanschool.conductor.submitter.grpc;

import com.github.asadyezdanschool.conductor.grpc.management.JobManagementServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages the gRPC channel and blocking stub for the scheduler's {@code JobManagementService}.
 *
 * <p>The channel is created once at startup and reused for all calls. The blocking stub is
 * thread-safe and can be called concurrently from Jersey request threads.
 *
 * <p>Target address is read from env var {@code SCHEDULER_GRPC_ADDRESS}
 * (format: {@code host:port}, default {@code localhost:50051}).
 *
 * <p>Call {@link #shutdown()} during graceful process shutdown to drain in-flight RPCs.
 */
@Singleton
public class SchedulerGrpcClient {

    private static final Logger log = Logger.getLogger(SchedulerGrpcClient.class.getName());
    static final String ENV_ADDRESS      = "SCHEDULER_GRPC_ADDRESS";
    static final String DEFAULT_ADDRESS  = "localhost:50051";

    private final ManagedChannel channel;
    private final JobManagementServiceGrpc.JobManagementServiceBlockingStub stub;

    @Inject
    public SchedulerGrpcClient() {
        String address = System.getenv().getOrDefault(ENV_ADDRESS, DEFAULT_ADDRESS);
        this.channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
        this.stub = JobManagementServiceGrpc.newBlockingStub(channel);
        log.info("Scheduler gRPC client connected to " + address);
    }

    /** Returns the blocking stub to use for all management RPC calls. */
    public JobManagementServiceGrpc.JobManagementServiceBlockingStub stub() {
        return stub;
    }

    /** Gracefully shut down the channel, waiting up to 5 seconds for in-flight calls. */
    public void shutdown() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
