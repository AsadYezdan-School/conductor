# Source this file: source local-dev/env.sh

# ── PostgreSQL ────────────────────────────────────────────────────────────────
export DB_USERNAME="conductor"
export DB_PASSWORD="conductor"

# Worker uses host-style DSN (not JDBC) — must be bare hostname only
export DB_WRITER_HOST="localhost"

# Scheduler and Submitter use JDBC
export DB_WRITER_URL="jdbc:postgresql://localhost:5432/conductor?sslmode=disable"
export DB_READER_URL="jdbc:postgresql://localhost:5432/conductor?sslmode=disable"

# Liquibase
export LIQUIBASE_COMMAND_URL="jdbc:postgresql://localhost:5432/conductor?sslmode=disable"
export LIQUIBASE_COMMAND_USERNAME="conductor"
export LIQUIBASE_COMMAND_PASSWORD="conductor"

# ── SQS / ElasticMQ ──────────────────────────────────────────────────────────
export SQS_QUEUE_URL="http://localhost:9324/000000000000/conductor-jobs"

# Dummy AWS credentials — ElasticMQ does not validate them but the SDKs require
# a region and non-empty key/secret to initialise their config chain
export AWS_ACCESS_KEY_ID="local"
export AWS_SECRET_ACCESS_KEY="local"
export AWS_REGION="us-east-1"

# Go aws-sdk-go-v2 (config v1.32.12): per-service endpoint override
export AWS_ENDPOINT_URL_SQS="http://localhost:9324"

# Java AWS SDK v2: global endpoint override (supported since 2.21.0)
# SqsClient.create() picks this up — no source code changes needed
export AWS_ENDPOINT_URL="http://localhost:9324"

# ── mock-data-services ────────────────────────────────────────────────────────
export SUBMITTER_URL="http://localhost:8080"
export MOCK_LISTENER_PORT="8081"
export MOCK_LISTENER_URL="http://localhost:8081"
export SUBMIT_INTERVAL_SECONDS="1"
export RESPONSE_DELAY_MS="200"
export RESPONSE_STATUS_CODE="200"
