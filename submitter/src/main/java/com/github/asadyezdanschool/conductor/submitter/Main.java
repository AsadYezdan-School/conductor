package com.github.asadyezdanschool.conductor.submitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Main {
    static String greeting() {
        return "Hello World from Java 25 built with Bazel after rebuild and moving bazelrc, we are trying to deploy with AWS";
    }

    public static void main(String[] args) throws Exception {
        String dbUrl  = System.getenv("DB_WRITER_URL");  // jdbc:postgresql://https://aws.rds.rds-proxy:5432/conductor
        String dbUser = System.getenv("DB_USERNAME");
        String dbPass = System.getenv("DB_PASSWORD");

        if (dbUrl == null) throw new IllegalStateException("DB_WRITER_URL not set");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            int i = 0;
            while (true) {
                System.out.println(greeting());

                // Insert a new job via RDS Proxy (writer endpoint)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO http_jobs (name, cron, url, method) VALUES (?, ?, ?, 'GET'::request_type)")) {
                    ps.setString(1, "job-" + i++);
                    ps.setString(2, "* * * * *");
                    ps.setString(3, "https://example.com");
                    ps.executeUpdate();
                }

                Thread.sleep(1000);
            }
        }
    }
}
