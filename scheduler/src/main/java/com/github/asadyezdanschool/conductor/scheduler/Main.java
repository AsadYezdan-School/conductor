package com.github.asadyezdanschool.conductor.scheduler;

import java.util.logging.Logger;

/**
 * Scheduler process entry point.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Build the Dagger component (validates all dependencies are wired)</li>
 *   <li>Initialize the in-memory job cache (full DB load + start background refresh)</li>
 *   <li>Start the Management gRPC server (port {@code SCHEDULER_MANAGEMENT_GRPC_PORT})</li>
 *   <li>Start the Execution gRPC server (port {@code SCHEDULER_EXECUTION_GRPC_PORT})</li>
 *   <li>Run the scheduler loop on the current thread (blocks until interrupted)</li>
 * </ol>
 *
 * <p>Shutdown (SIGTERM / SIGINT via JVM shutdown hook):
 * <ul>
 *   <li>Stop the scheduler loop</li>
 *   <li>Gracefully shut down both gRPC servers</li>
 *   <li>Stop the cache background refresh</li>
 * </ul>
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        AppComponent component = DaggerAppComponent.create();

        component.jobCache().initialize();
        component.managementGrpcServer().start();
        component.executionGrpcServer().start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down scheduler...");
            component.schedulerLoop().stop();
            component.managementGrpcServer().stop();
            component.executionGrpcServer().stop();
            component.jobCache().shutdown();
            log.info("Scheduler shutdown complete");
        }, "shutdown-hook"));

        log.info("Scheduler started — entering loop");
        component.schedulerLoop().start(); // blocks
    }
}
