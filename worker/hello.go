package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

func main() {
// 	queueUrl := os.Getenv("SQS_QUEUE_URL")
// 	if queueUrl == "" {
// 		log.Fatal("SQS_QUEUE_URL not set")
// 	}
//
// 	cfg, err := config.LoadDefaultConfig(context.Background())
// 	if err != nil {
// 		log.Fatalf("failed to load AWS config: %v", err)
// 	}
//
// 	client := sqs.NewFromConfig(cfg)
//
// 	for {
// 		out, err := client.ReceiveMessage(context.Background(), &sqs.ReceiveMessageInput{
// 			QueueUrl:            &queueUrl,
// 			MaxNumberOfMessages: 10,
// 			WaitTimeSeconds:     20,
// 		})
// 		if err != nil {
// 			log.Printf("receive error: %v", err)
// 			continue
// 		}
// 		for _, msg := range out.Messages {
// 			fmt.Println(*msg.Body)
// 			client.DeleteMessage(context.Background(), &sqs.DeleteMessageInput{ //nolint
// 				QueueUrl:      &queueUrl,
// 				ReceiptHandle: msg.ReceiptHandle,
// 			})
// 		}
// 	}

}
