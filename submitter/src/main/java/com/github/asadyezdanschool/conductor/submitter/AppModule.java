package com.github.asadyezdanschool.conductor.submitter;

import com.github.asadyezdanschool.conductor.submitter.exception.ExceptionMappers;
import com.github.asadyezdanschool.conductor.submitter.grpc.SchedulerGrpcClient;
import com.github.asadyezdanschool.conductor.submitter.resource.JobResource;
import com.github.asadyezdanschool.conductor.submitter.service.JobService;
import com.github.asadyezdanschool.conductor.submitter.service.JobServiceImpl;
import dagger.Module;
import dagger.Provides;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

/**
 * Dagger module for the submitter process.
 *
 * <p>The submitter no longer holds a database connection — all writes now flow through
 * the scheduler's {@code JobManagementService} gRPC endpoint. The only infrastructure
 * dependency is the {@link SchedulerGrpcClient} holding a {@code ManagedChannel}.
 *
 * <p>Environment variables consumed:
 * <ul>
 *   <li>{@code SCHEDULER_GRPC_ADDRESS} — scheduler management gRPC address
 *       (format: {@code host:port}, default {@code localhost:50051})</li>
 * </ul>
 */
@Module
public class AppModule {

    @Provides
    @Singleton
    public SchedulerGrpcClient provideSchedulerGrpcClient() {
        return new SchedulerGrpcClient();
    }

    @Provides
    @Singleton
    public JobService provideJobService(SchedulerGrpcClient grpcClient) {
        return new JobServiceImpl(grpcClient);
    }

    @Provides
    @Singleton
    public JobResource provideJobResource(JobService service) {
        return new JobResource(service);
    }

    @Provides
    @Singleton
    public ResourceConfig provideResourceConfig(JobResource resource, ExceptionMappers exceptionMappers) {
        ResourceConfig cfg = new ResourceConfig();
        cfg.register(resource);
        cfg.register(exceptionMappers);
        cfg.register(JacksonFeature.class);
        return cfg;
    }

    @Provides
    @Singleton
    public ExceptionMappers provideExceptionMappers() {
        return new ExceptionMappers();
    }
}
