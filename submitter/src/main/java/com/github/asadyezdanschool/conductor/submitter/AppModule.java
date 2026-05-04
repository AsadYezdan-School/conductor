package com.github.asadyezdanschool.conductor.submitter;

import com.github.asadyezdanschool.conductor.submitter.exception.ExceptionMappers;
import com.github.asadyezdanschool.conductor.submitter.grpc.SchedulerGrpcClient;
import com.github.asadyezdanschool.conductor.submitter.repository.AlertConfigRepository;
import com.github.asadyezdanschool.conductor.submitter.repository.DependencyRepository;
import com.github.asadyezdanschool.conductor.submitter.repository.ReadJobRepository;
import com.github.asadyezdanschool.conductor.submitter.resource.AnalyticsResource;
import com.github.asadyezdanschool.conductor.submitter.resource.CorsFilter;
import com.github.asadyezdanschool.conductor.submitter.resource.HealthResource;
import com.github.asadyezdanschool.conductor.submitter.resource.JobResource;
import com.github.asadyezdanschool.conductor.submitter.resource.RunResource;
import com.github.asadyezdanschool.conductor.submitter.service.JobService;
import com.github.asadyezdanschool.conductor.submitter.service.JobServiceImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Dagger module for the submitter process.
 *
 * <p>Environment variables consumed:
 * <ul>
 *   <li>{@code SCHEDULER_GRPC_ADDRESS} — scheduler management gRPC address
 *       (format: {@code host:port}, default {@code localhost:50051})</li>
 *   <li>{@code DB_WRITER_URL} — JDBC URL for the PostgreSQL database</li>
 *   <li>{@code DB_USERNAME} — database username</li>
 *   <li>{@code DB_PASSWORD} — database password</li>
 * </ul>
 */
@Module
public class AppModule {

    @Provides
    @Singleton
    static DataSource provideDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requireEnv("DB_WRITER_URL"));
        config.setUsername(requireEnv("DB_USERNAME"));
        config.setPassword(requireEnv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        // Prevent queries from hanging indefinitely if the proxy drops the TCP connection
        config.addDataSourceProperty("socketTimeout", "30");
        return new HikariDataSource(config);
    }

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
    public AlertConfigRepository provideAlertConfigRepository(DataSource dataSource) {
        return new AlertConfigRepository(dataSource);
    }

    @Provides
    @Singleton
    public DependencyRepository provideDependencyRepository(DataSource dataSource) {
        return new DependencyRepository(dataSource);
    }

    @Provides
    @Singleton
    public JobResource provideJobResource(JobService service, ReadJobRepository readRepo,
                                          AlertConfigRepository alertConfigRepo,
                                          DependencyRepository depRepo) {
        return new JobResource(service, readRepo, alertConfigRepo, depRepo);
    }

    @Provides
    @Singleton
    public RunResource provideRunResource(ReadJobRepository readRepo) {
        return new RunResource(readRepo);
    }

    @Provides
    @Singleton
    public AnalyticsResource provideAnalyticsResource(ReadJobRepository readRepo) {
        return new AnalyticsResource(readRepo);
    }

    @Provides
    @Singleton
    public ResourceConfig provideResourceConfig(
            JobResource jobResource,
            RunResource runResource,
            AnalyticsResource analyticsResource,
            ExceptionMappers exceptionMappers) {
        ResourceConfig cfg = new ResourceConfig();
        cfg.register(jobResource);
        cfg.register(runResource);
        cfg.register(analyticsResource);
        cfg.register(new HealthResource());
        cfg.register(exceptionMappers);
        cfg.register(CorsFilter.class);
        cfg.register(JacksonFeature.class);
        return cfg;
    }

    @Provides
    @Singleton
    public ExceptionMappers provideExceptionMappers() {
        return new ExceptionMappers();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }
}
