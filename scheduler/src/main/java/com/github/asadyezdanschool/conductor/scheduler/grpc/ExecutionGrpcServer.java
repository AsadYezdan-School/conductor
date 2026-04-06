package com.github.asadyezdanschool.conductor.scheduler.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Netty gRPC server hosting {@link JobExecutionServiceImpl}.
 *
 * <p>Consumed by Worker processes (GetHttpRunDetails + ReportStatus).
 * Runs on port specified by env var {@code SCHEDULER_EXECUTION_GRPC_PORT} (default 50052).
 */
@Singleton
public class ExecutionGrpcServer {

    private static final Logger log = Logger.getLogger(ExecutionGrpcServer.class.getName());

    private final int port;
    private final JobExecutionServiceImpl serviceImpl;
    private Server server;

    @Inject
    public ExecutionGrpcServer(@Named("executionGrpcPort") int port,
                                JobExecutionServiceImpl serviceImpl) {
        this.port        = port;
        this.serviceImpl = serviceImpl;
    }

    public void start() throws IOException {
        server = NettyServerBuilder.forPort(port)
                .addService(serviceImpl)
                .build()
                .start();
        log.info("Execution gRPC server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void awaitTermination() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
