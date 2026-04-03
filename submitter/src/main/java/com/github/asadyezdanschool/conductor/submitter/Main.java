package com.github.asadyezdanschool.conductor.submitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class Main {
    static String greeting() {
        return "Hello World from Java 25 built with Bazel after rebuild and moving bazelrc, we are trying to deploy with AWS";
    }

    public static void main(String[] args) throws Exception {
        String dbUrl  = System.getenv("DB_WRITER_URL");
        String dbUser = System.getenv("DB_USERNAME");
        String dbPass = System.getenv("DB_PASSWORD");

        if (dbUrl == null) throw new IllegalStateException("DB_WRITER_URL not set");

        Connection conn = connectWithRetry(dbUrl, dbUser, dbPass);

        int i = 0;
        while (true) {
            submitHttpJob(conn, "job-" + i++, "* * * * *", "https://example.com", "GET");
            Thread.sleep(1000);
        }
    }

    private static void submitHttpJob(Connection conn, String name, String cron, String url, String method) throws Exception {
        conn.setAutoCommit(false);
        try {
            UUID jobFamilyId = UUID.randomUUID();
            UUID jobDefinitionId;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO job_definitions (job_family_id, name, cron, job_type) VALUES (?, ?, ?, 'HTTP'::job_type) RETURNING id")) {
                ps.setObject(1, jobFamilyId);
                ps.setString(2, name);
                ps.setString(3, cron);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    jobDefinitionId = (UUID) rs.getObject(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO job_type_http_configs (job_definition_id, url, method) VALUES (?, ?, ?::request_type)")) {
                ps.setObject(1, jobDefinitionId);
                ps.setString(2, url);
                ps.setString(3, method);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Submitted job: " + name + " (definition=" + jobDefinitionId + ")");
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static Connection connectWithRetry(String url, String user, String pass) throws InterruptedException {
        int attempts = 0;
        while (true) {
            try {
                Connection conn = DriverManager.getConnection(url, user, pass);
                System.out.println("Connected to database");
                return conn;
            } catch (Exception e) {
                attempts++;
                System.err.println("DB connection attempt " + attempts + " failed: " + e.getMessage() + " — retrying in 5s");
                Thread.sleep(5000);
            }
        }
    }
}
