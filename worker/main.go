package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"os"
	"time"

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

	for attempts := 1; ; attempts++ {
		if err := db.Ping(); err == nil {
			log.Println("Connected to database")
			break
		} else {
			log.Printf("DB connection attempt %d failed: %v — retrying in 5s", attempts, err)
			time.Sleep(5 * time.Second)
		}
	}

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
			jobID := *msg.Body

			// Look up job definition by ID
			var name, cron, url, method, status string
			err := db.QueryRow(
				`SELECT name, cron, url, method, status FROM http_jobs WHERE id = $1`,
				jobID,
			).Scan(&name, &cron, &url, &method, &status)
			if err != nil {
				log.Printf("job lookup error (id=%s): %v", jobID, err)
				continue
			}
			fmt.Printf("executing job id=%s name=%s cron=%q url=%s method=%s status=%s\n",
				jobID, name, cron, url, method, status)

			// Mark job as EXECUTED
			_, dbErr := db.Exec(
				`UPDATE http_jobs SET status = 'EXECUTED', updated_at = NOW() WHERE id = $1`,
				jobID,
			)
			if dbErr != nil {
				log.Printf("db update error (id=%s): %v", jobID, dbErr)
				continue
			}

			client.DeleteMessage(context.Background(), &sqs.DeleteMessageInput{ //nolint
				QueueUrl:      &queueUrl,
				ReceiptHandle: msg.ReceiptHandle,
			})
		}
	}
}
