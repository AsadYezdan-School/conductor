package com.github.asadyezdanschool.conductor.scheduler;

import com.github.asadyezdanschool.conductor.scheduler.cache.InMemoryJobCache;
import com.github.asadyezdanschool.conductor.scheduler.cache.JobCache;
import com.github.asadyezdanschool.conductor.scheduler.service.JobExecutionService;
import com.github.asadyezdanschool.conductor.scheduler.service.JobExecutionServiceImpl;
import com.github.asadyezdanschool.conductor.scheduler.service.JobManagementService;
import com.github.asadyezdanschool.conductor.scheduler.service.JobManagementServiceImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.sqs.SqsClient;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Dagger module wiring together all scheduler dependencies.
 *
 * <p>Environment variables consumed:
 * <ul>
 *   <li>{@code DB_WRITER_URL}          — JDBC URL for PostgreSQL</li>
 *   <li>{@code DB_USERNAME}            — DB username</li>
 *   <li>{@code DB_PASSWORD}            — DB password</li>
 *   <li>{@code SQS_QUEUE_URL}          — SQS queue URL for job run messages</li>
 *   <li>{@code SCHEDULER_MANAGEMENT_GRPC_PORT} — port for JobManagementService (default 50051)</li>
 *   <li>{@code SCHEDULER_EXECUTION_GRPC_PORT}  — port for JobExecutionService  (default 50052)</li>
 * </ul>
 */
@Module
public abstract class AppModule {

    // ── Interface bindings ────────────────────────────────────────────────────

    @Binds
    @Singleton
    abstract JobCache bindJobCache(InMemoryJobCache impl);

    @Binds
    @Singleton
    abstract JobManagementService bindJobManagementService(JobManagementServiceImpl impl);

    @Binds
    @Singleton
    abstract JobExecutionService bindJobExecutionService(JobExecutionServiceImpl impl);

    // ── Concrete providers ────────────────────────────────────────────────────

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
        return new HikariDataSource(config);
    }

    @Provides
    @Singleton
    static SqsClient provideSqsClient() {
        return SqsClient.create();
    }

    @Provides
    @Named("sqsQueueUrl")
    static String provideSqsQueueUrl() {
        return requireEnv("SQS_QUEUE_URL");
    }

    @Provides
    @Named("managementGrpcPort")
    static int provideManagementGrpcPort() {
        String val = System.getenv("SCHEDULER_MANAGEMENT_GRPC_PORT");
        return val != null ? Integer.parseInt(val) : 50051;
    }

    @Provides
    @Named("executionGrpcPort")
    static int provideExecutionGrpcPort() {
        String val = System.getenv("SCHEDULER_EXECUTION_GRPC_PORT");
        return val != null ? Integer.parseInt(val) : 50052;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String requireEnv(String name) {
        String val = System.getenv(name);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return val;
    }
}
