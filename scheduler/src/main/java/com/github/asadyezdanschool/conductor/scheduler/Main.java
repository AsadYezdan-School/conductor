package com.github.asadyezdanschool.conductor.scheduler;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {
        String queueUrl = System.getenv("SQS_QUEUE_URL");
        String dbUrl    = System.getenv("DB_WRITER_URL");
        String dbUser   = System.getenv("DB_USERNAME");
        String dbPass   = System.getenv("DB_PASSWORD");

        if (queueUrl == null) throw new IllegalStateException("SQS_QUEUE_URL not set");
        if (dbUrl    == null) throw new IllegalStateException("DB_WRITER_URL not set");

        Connection conn = connectWithRetry(dbUrl, dbUser, dbPass);

        try (SqsClient sqs = SqsClient.create()) {
            while (true) {
                // Find active job definitions that have never been triggered
                List<UUID[]> jobs = new ArrayList<>();
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id, job_family_id FROM job_definitions " +
                             "WHERE is_latest = TRUE AND is_parked = FALSE AND is_deleted = FALSE " +
                             "AND NOT EXISTS (SELECT 1 FROM job_runs jr WHERE jr.job_definition_id = job_definitions.id) " +
                             "LIMIT 10")) {
                    while (rs.next()) {
                        jobs.add(new UUID[]{
                            (UUID) rs.getObject("id"),
                            (UUID) rs.getObject("job_family_id")
                        });
                    }
                }

                for (UUID[] job : jobs) {
                    UUID jobDefinitionId = job[0];
                    UUID jobFamilyId     = job[1];
                    String jobRunId = enqueueRun(conn, jobDefinitionId, jobFamilyId);
                    sqs.sendMessage(SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(jobRunId)
                            .build());
                    System.out.println("queued run: " + jobRunId + " for definition: " + jobDefinitionId);
                }

                Thread.sleep(1000);
            }
        }
    }

    private static String enqueueRun(Connection conn, UUID jobDefinitionId, UUID jobFamilyId) throws Exception {
        conn.setAutoCommit(false);
        try {
            UUID jobRunId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO job_runs (job_definition_id, job_family_id, status, scheduled_at) " +
                    "VALUES (?, ?, 'QUEUED'::job_status, NOW()) RETURNING id")) {
                ps.setObject(1, jobDefinitionId);
                ps.setObject(2, jobFamilyId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    jobRunId = (UUID) rs.getObject(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO job_run_events (job_run_id, status, source) " +
                    "VALUES (?, 'QUEUED'::job_status, 'scheduler')")) {
                ps.setObject(1, jobRunId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO job_schedules (job_definition_id, last_triggered_at) VALUES (?, NOW()) " +
                    "ON CONFLICT (job_definition_id) DO UPDATE SET last_triggered_at = NOW(), last_evaluated_at = NOW()")) {
                ps.setObject(1, jobDefinitionId);
                ps.executeUpdate();
            }

            conn.commit();
            return jobRunId.toString();
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