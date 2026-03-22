package com.github.asadyezdanschool.conductor.scheduler;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) throws Exception {
        String queueUrl = System.getenv("SQS_QUEUE_URL");
        String dbUrl    = System.getenv("DB_READER_URL");
        String dbUser   = System.getenv("DB_USERNAME");
        String dbPass   = System.getenv("DB_PASSWORD");

        if (queueUrl == null) throw new IllegalStateException("SQS_QUEUE_URL not set");
        if (dbUrl    == null) throw new IllegalStateException("DB_READER_URL not set");

        Connection conn = connectWithRetry(dbUrl, dbUser, dbPass);

        try (SqsClient sqs = SqsClient.create()) {
            while (true) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id FROM http_jobs WHERE status = 'CREATED' LIMIT 10")) {
                    while (rs.next()) {
                        String jobId = rs.getString("id");
                        sqs.sendMessage(SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .messageBody(jobId)
                                .build());
                        System.out.println("queued job: " + jobId);
                    }
                }
                Thread.sleep(1000);
            }
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
