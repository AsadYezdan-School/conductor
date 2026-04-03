package com.github.asadyezdanschool.conductor.submitter;

import com.github.asadyezdanschool.conductor.submitter.exception.ExceptionMappers;
import com.github.asadyezdanschool.conductor.submitter.repository.JobRepository;
import com.github.asadyezdanschool.conductor.submitter.resource.JobResource;
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

@Module
public class AppModule {

    @Provides
    @Singleton
    public DataSource provideDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(requireEnv("DB_WRITER_URL"));
        cfg.setUsername(requireEnv("DB_USERNAME"));
        cfg.setPassword(requireEnv("DB_PASSWORD"));
        cfg.setMaximumPoolSize(20);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(3000);
        cfg.addDataSourceProperty("ApplicationName", "conductor-submitter");
        return new HikariDataSource(cfg);
    }

    @Provides
    @Singleton
    public JobRepository provideJobRepository(DataSource ds) {
        return new JobRepository(ds);
    }

    @Provides
    @Singleton
    public JobService provideJobService(JobRepository repo) {
        return new JobServiceImpl(repo);
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

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            // Allow system property override (useful in integration tests)
            value = System.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }
}