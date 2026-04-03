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
	dbHost   := os.Getenv("DB_WRITER_HOST")
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
			jobRunID := *msg.Body
			if processRun(db, jobRunID) {
				client.DeleteMessage(context.Background(), &sqs.DeleteMessageInput{ //nolint
					QueueUrl:      &queueUrl,
					ReceiptHandle: msg.ReceiptHandle,
				})
			}
		}
	}
}

func processRun(db *sql.DB, jobRunID string) bool {
	startedAt := time.Now()

	_, err := db.Exec(
		`UPDATE job_runs SET status = 'RUNNING'::job_status, started_at = NOW() WHERE id = $1`,
		jobRunID,
	)
	if err != nil {
		log.Printf("failed to mark run RUNNING (id=%s): %v", jobRunID, err)
		return false
	}

	_, err = db.Exec(
		`INSERT INTO job_run_events (job_run_id, status, source) VALUES ($1, 'RUNNING'::job_status, 'worker')`,
		jobRunID,
	)
	if err != nil {
		log.Printf("failed to insert RUNNING event (id=%s): %v", jobRunID, err)
	}

	var name, cron, url, method string
	err = db.QueryRow(`
		SELECT jd.name, jd.cron, c.url, c.method::text
		FROM job_runs jr
		JOIN job_definitions jd ON jd.id = jr.job_definition_id
		JOIN job_type_http_configs c ON c.job_definition_id = jd.id
		WHERE jr.id = $1`,
		jobRunID,
	).Scan(&name, &cron, &url, &method)
	if err != nil {
		log.Printf("job lookup error (run=%s): %v", jobRunID, err)
		markFailed(db, jobRunID, startedAt, "job lookup failed: "+err.Error())
		return false
	}

	fmt.Printf("executing run=%s name=%s cron=%q url=%s method=%s\n", jobRunID, name, cron, url, method)

	durationMs := int(time.Since(startedAt).Milliseconds())

	_, err = db.Exec(
		`UPDATE job_runs SET status = 'SUCCEEDED'::job_status, finished_at = NOW(), duration_ms = $2 WHERE id = $1`,
		jobRunID, durationMs,
	)
	if err != nil {
		log.Printf("failed to mark run SUCCEEDED (id=%s): %v", jobRunID, err)
	}

	_, err = db.Exec(
		`INSERT INTO job_run_events (job_run_id, status, source) VALUES ($1, 'SUCCEEDED'::job_status, 'worker')`,
		jobRunID,
	)
	if err != nil {
		log.Printf("failed to insert SUCCEEDED event (id=%s): %v", jobRunID, err)
	}

	return true
}

func markFailed(db *sql.DB, jobRunID string, startedAt time.Time, message string) {
	durationMs := int(time.Since(startedAt).Milliseconds())
	db.Exec( //nolint
		`UPDATE job_runs SET status = 'FAILED'::job_status, finished_at = NOW(), duration_ms = $2 WHERE id = $1`,
		jobRunID, durationMs,
	)
	db.Exec( //nolint
		`INSERT INTO job_run_events (job_run_id, status, message, source) VALUES ($1, 'FAILED'::job_status, $2, 'worker')`,
		jobRunID, message,
	)
}