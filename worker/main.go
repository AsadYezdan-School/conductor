package main

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"time"

	"asadyezdanschool/conductor/worker/client"
	"asadyezdanschool/conductor/worker/processor"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	sqstypes "github.com/aws/aws-sdk-go-v2/service/sqs/types"
)

// sqsMessage is the JSON structure placed on the queue by the scheduler.
// Keeping it extensible: jobType determines which processor handles the run.
type sqsMessage struct {
	JobRunID string `json:"jobRunId"`
	JobType  string `json:"jobType"` // "HTTP" | "SHELL" | "PYTHON"
}

func main() {
	queueUrl := os.Getenv("SQS_QUEUE_URL")
	if queueUrl == "" {
		log.Fatal("SQS_QUEUE_URL not set")
	}

	// Create scheduler gRPC client
	schedulerClient, err := client.NewSchedulerClient()
	if err != nil {
		log.Fatalf("scheduler grpc: %v", err)
	}
	defer schedulerClient.Close()

	// Create HTTP processor
	httpProc := processor.NewHTTPProcessor(schedulerClient.Stub, nil)

	// Load AWS config + create SQS client
	cfg, err := config.LoadDefaultConfig(context.Background())
	if err != nil {
		log.Fatalf("load aws config: %v", err)
	}
	sqsClient := sqs.NewFromConfig(cfg)

	log.Printf("Worker started — polling %s", queueUrl)

	for {
		out, err := sqsClient.ReceiveMessage(context.Background(), &sqs.ReceiveMessageInput{
			QueueUrl:            &queueUrl,
			MaxNumberOfMessages: 10,
			WaitTimeSeconds:     20,
		})
		if err != nil {
			log.Printf("ReceiveMessage error: %v — retrying in 5s", err)
			time.Sleep(5 * time.Second)
			continue
		}

		for _, msg := range out.Messages {
			go processMessage(context.Background(), msg, sqsClient, queueUrl, httpProc)
		}
	}
}

func processMessage(ctx context.Context, msg sqstypes.Message,
	sqsClient *sqs.Client, queueUrl string, httpProc *processor.HTTPProcessor) {

	var m sqsMessage
	if err := json.Unmarshal([]byte(*msg.Body), &m); err != nil {
		log.Printf("Malformed SQS message (deleting): %v", err)
		deleteMessage(ctx, sqsClient, queueUrl, msg.ReceiptHandle)
		return
	}

	var ok bool
	switch m.JobType {
	case "HTTP":
		ok = httpProc.ProcessRun(ctx, m.JobRunID)
	default:
		log.Printf("Unknown job type %q for run %s — deleting message", m.JobType, m.JobRunID)
		ok = true // don't retry unknown types
	}

	if ok {
		deleteMessage(ctx, sqsClient, queueUrl, msg.ReceiptHandle)
	}
}

func deleteMessage(ctx context.Context, sqsClient *sqs.Client, queueUrl string, receiptHandle *string) {
	_, err := sqsClient.DeleteMessage(ctx, &sqs.DeleteMessageInput{
		QueueUrl:      &queueUrl,
		ReceiptHandle: receiptHandle,
	})
	if err != nil {
		log.Printf("DeleteMessage error: %v", err)
	}
}
