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
        String dbUrl    = System.getenv("DB_READER_URL");  // jdbc:postgresql://https://aws.rds.reader:5432/conductor
        String dbUser   = System.getenv("DB_USERNAME");
        String dbPass   = System.getenv("DB_PASSWORD");

        if (queueUrl == null) throw new IllegalStateException("SQS_QUEUE_URL not set");
        if (dbUrl    == null) throw new IllegalStateException("DB_READER_URL not set");

        try (SqsClient sqs = SqsClient.create();
             Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {

            while (true) {
                // Read jobs from DB via reader endpoint
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id, name, cron FROM http_jobs WHERE status = 'CREATED' LIMIT 10")) {
                    while (rs.next()) {
                        String jobId = rs.getString("id");
                        String name  = rs.getString("name");
                        String cron  = rs.getString("cron");
                        String body  = "job:" + jobId + " name:" + name + " cron:" + cron;
                        sqs.sendMessage(SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .messageBody(body)
                                .build());
                        System.out.println("sent: " + body);
                    }
                }
                Thread.sleep(1000);
            }
        }
    }
}
