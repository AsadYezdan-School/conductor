package com.github.asadyezdanschool.conductor.scheduler;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class Main {
    public static void main(String[] args) throws InterruptedException {
//        String queueUrl = System.getenv("SQS_QUEUE_URL");
//        if (queueUrl == null) {
//            throw new IllegalStateException("SQS_QUEUE_URL not set");
//        }
//
//        try (SqsClient sqs = SqsClient.create()) {
//            int i = 0;
//            while (true) {
//                String body = "hello sqs from scheduler " + i++;
//                sqs.sendMessage(SendMessageRequest.builder()
//                        .queueUrl(queueUrl)
//                        .messageBody(body)
//                        .build());
//                System.out.println("sent: " + body);
//                Thread.sleep(1000);
//            }
//        }
        while (true) {
            int i = 1;
            System.out.println("hello from scheduler" + i++ );
            Thread.sleep(1000);
        }
    }
}
