package com.github.asadyezdanschool.conductor.submitter.integration;

import com.github.asadyezdanschool.conductor.submitter.AppComponent;
import com.github.asadyezdanschool.conductor.submitter.DaggerAppComponent;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.6")
                    .withDatabaseName("conductor")
                    .withUsername("conductor")
                    .withPassword("conductor");

    private HttpServer server;

    @BeforeAll
    void startAll() throws Exception {
        postgres.start();
        applyMigrations();
        startServer();
    }

    @AfterAll
    void stopAll() {
        if (server != null) server.stop(0);
        postgres.stop();
    }

    private void applyMigrations() throws Exception {
        // Resolve the changelog directory from Bazel runfiles.
        // Under Bazel test, TEST_SRCDIR points to the runfiles root.
        String srcDir = System.getenv("TEST_SRCDIR");
        Path changelogDir;
        if (srcDir != null) {
            // Bazel runfiles path: <TEST_SRCDIR>/<workspace>/db-migrations/changelog/
            String workspace = System.getenv("TEST_WORKSPACE");
            if (workspace == null) workspace = "_main";
            changelogDir = Paths.get(srcDir, workspace, "db-migrations", "changelog");
        } else {
            // Fallback for running tests outside Bazel (e.g., directly via IDE)
            changelogDir = Paths.get("db-migrations", "changelog").toAbsolutePath();
        }

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase liquibase = new Liquibase(
                    "db.changelog-master.yaml",
                    new DirectoryResourceAccessor(changelogDir),
                    database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private void startServer() throws Exception {
        // Point the Dagger component at the Testcontainers postgres
        System.setProperty("DB_WRITER_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());

        AppComponent component = DaggerAppComponent.create();

        // port 0 → OS assigns a free port
        server = JdkHttpServerFactory.createHttpServer(
                new java.net.URI("http://localhost:0/"),
                component.resourceConfig(),
                false);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        int port = server.getAddress().getPort();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "";
    }
}