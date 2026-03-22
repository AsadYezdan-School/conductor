package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"os"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	_ "github.com/lib/pq"
)

func main() {
	queueUrl := os.Getenv("SQS_QUEUE_URL")
	dbHost   := os.Getenv("DB_WRITER_HOST") // https://aws.rds.rds-proxy (dummy)
	dbUser   := os.Getenv("DB_USERNAME")
	dbPass   := os.Getenv("DB_PASSWORD")

	if queueUrl == "" {
		log.Fatal("SQS_QUEUE_URL not set")
	}
	if dbHost == "" {
		log.Fatal("DB_WRITER_HOST not set")
	}

	dsn := fmt.Sprintf(
		"host=%s port=5432 user=%s password=%s dbname=conductor sslmode=disable",
		dbHost, dbUser, dbPass,
	)
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		log.Fatalf("open db: %v", err)
	}
	defer db.Close()

	cfg, err := config.LoadDefaultConfig(context.Background())
	if err != nil {
		log.Fatalf("failed to load AWS config: %v", err)
	}

	client := sqs.NewFromConfig(cfg)

	for {
		out, err := client.ReceiveMessage(context.Background(), &sqs.ReceiveMessageInput{
			QueueUrl:            &queueUrl,
			MaxNumberOfMessages: 10,
			WaitTimeSeconds:     20,
		})
		if err != nil {
			log.Printf("receive error: %v", err)
			continue
		}
		for _, msg := range out.Messages {
			body := *msg.Body
			fmt.Println(body)

			// Write processed job record to DB via RDS Proxy
			_, dbErr := db.Exec(
				`UPDATE http_jobs SET status = 'PROCESSED', updated_at = NOW() WHERE id = $1`,
				body,
			)
			if dbErr != nil {
				log.Printf("db write error: %v", dbErr)
			}

			client.DeleteMessage(context.Background(), &sqs.DeleteMessageInput{ //nolint
				QueueUrl:      &queueUrl,
				ReceiptHandle: msg.ReceiptHandle,
			})
		}
	}
}
