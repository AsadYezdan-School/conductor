package com.github.asadyezdanschool.conductor.scheduler;

import com.github.asadyezdanschool.conductor.scheduler.cache.InMemoryJobCache;
import com.github.asadyezdanschool.conductor.scheduler.grpc.ExecutionGrpcServer;
import com.github.asadyezdanschool.conductor.scheduler.grpc.ManagementGrpcServer;
import com.github.asadyezdanschool.conductor.scheduler.scheduling.SchedulerLoop;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Dagger component — the object graph root for the scheduler process.
 *
 * <p>Main instantiates this component and retrieves the top-level objects it needs
 * to start the gRPC servers and the scheduler loop.
 */
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {

    ManagementGrpcServer  managementGrpcServer();
    ExecutionGrpcServer   executionGrpcServer();
    SchedulerLoop         schedulerLoop();
    InMemoryJobCache      jobCache();
}
