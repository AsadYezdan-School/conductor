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
 * Netty gRPC server hosting {@link JobManagementServiceImpl}.
 *
 * <p>Consumed by the Submitter service (job creation / editing / park / unpark).
 * Runs on port specified by env var {@code SCHEDULER_MANAGEMENT_GRPC_PORT} (default 50051).
 */
@Singleton
public class ManagementGrpcServer {

    private static final Logger log = Logger.getLogger(ManagementGrpcServer.class.getName());

    private final int port;
    private final JobManagementServiceImpl serviceImpl;
    private Server server;

    @Inject
    public ManagementGrpcServer(@Named("managementGrpcPort") int port,
                                 JobManagementServiceImpl serviceImpl) {
        this.port        = port;
        this.serviceImpl = serviceImpl;
    }

    public void start() throws IOException {
        server = NettyServerBuilder.forPort(port)
                .addService(serviceImpl)
                .build()
                .start();
        log.info("Management gRPC server started on port " + port);
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
