package processor

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	pb "asadyezdanschool/conductor/worker/gen/execution"
)

// maxResponseBodyBytes is the maximum number of response body bytes stored in the event log.
const maxResponseBodyBytes = 4096

// HTTPProcessor executes HTTP job runs. It communicates exclusively with the
// scheduler via gRPC — no direct database access.
//
// Lifecycle for a single run:
//  1. GetHttpRunDetails   — fetch URL, method, headers, timeout from the scheduler
//  2. ReportStatus(RUNNING) — tell scheduler the run has started
//  3. Execute the HTTP request
//  4. ReportStatus(SUCCEEDED | FAILED) — report outcome with duration_ms
type HTTPProcessor struct {
	stub       pb.JobExecutionServiceClient
	httpClient *http.Client
}

// NewHTTPProcessor creates a processor backed by the given gRPC stub.
// A custom httpClient can be injected for testing; pass nil to use a default.
func NewHTTPProcessor(stub pb.JobExecutionServiceClient, httpClient *http.Client) *HTTPProcessor {
	if httpClient == nil {
		httpClient = &http.Client{Timeout: 60 * time.Second}
	}
	return &HTTPProcessor{stub: stub, httpClient: httpClient}
}

// ProcessRun executes a single job run identified by jobRunID.
// Returns true if the run completed (successfully or failed) and the SQS message
// should be deleted; returns false if a transient error occurred and the message
// should be retried.
func (p *HTTPProcessor) ProcessRun(ctx context.Context, jobRunID string) bool {
	startedAt := time.Now()

	// 1. Fetch run details
	details, err := p.stub.GetHttpRunDetails(ctx, &pb.GetHttpRunDetailsRequest{JobRunId: jobRunID})
	if err != nil {
		log.Printf("GetHttpRunDetails failed for run %s: %v", jobRunID, err)
		return false // transient — don't delete SQS message
	}

	// 2. Report RUNNING
	log.Printf("Job run %s: running (%s %s) — attempt %d/%d",
		jobRunID, details.Method, details.Url, details.AttemptNumber, details.MaxRetries)
	_, err = p.stub.ReportStatus(ctx, &pb.ReportStatusRequest{
		JobRunId: jobRunID,
		Status:   pb.JobStatus_RUNNING,
	})
	if err != nil {
		log.Printf("ReportStatus(RUNNING) failed for run %s: %v", jobRunID, err)
		// Continue — the run is in progress; logging the error is sufficient
	}

	// 3. Execute HTTP request
	statusCode, responseBody, execErr := p.execute(ctx, details)
	durationMs := time.Since(startedAt).Milliseconds()

	if execErr != nil || statusCode < 200 || statusCode >= 300 {
		// 4a. Failed
		message := ""
		if execErr != nil {
			message = execErr.Error()
		} else {
			message = fmt.Sprintf("non-2xx status: %d", statusCode)
		}
		log.Printf("Job run %s: executed with status FAILED (%s) in %dms — response: %s", jobRunID, message, durationMs, responseBody)
		reportResp, reportErr := p.stub.ReportStatus(ctx, &pb.ReportStatusRequest{
			JobRunId:       jobRunID,
			Status:         pb.JobStatus_FAILED,
			Message:        message,
			HttpStatusCode: int32(statusCode),
			ResponseBody:   responseBody,
			DurationMs:     durationMs,
		})
		if reportErr != nil {
			log.Printf("ReportStatus(FAILED) failed for run %s: %v", jobRunID, reportErr)
		} else if reportResp.ShouldRetry {
			log.Printf("Job run %s: scheduler has enqueued a retry", jobRunID)
		} else {
			log.Printf("Job run %s: no retries remaining", jobRunID)
		}
		return true
	}

	// 4b. Succeeded
	log.Printf("Job run %s: executed with status SUCCEEDED (HTTP %d) in %dms — response: %s", jobRunID, statusCode, durationMs, responseBody)
	_, reportErr := p.stub.ReportStatus(ctx, &pb.ReportStatusRequest{
		JobRunId:       jobRunID,
		Status:         pb.JobStatus_SUCCEEDED,
		HttpStatusCode: int32(statusCode),
		ResponseBody:   responseBody,
		DurationMs:     durationMs,
	})
	if reportErr != nil {
		log.Printf("ReportStatus(SUCCEEDED) failed for run %s: %v", jobRunID, reportErr)
	}
	return true
}

// execute performs the HTTP call described by details and returns
// (statusCode, trimmedResponseBody, error).
func (p *HTTPProcessor) execute(ctx context.Context, details *pb.GetHttpRunDetailsResponse) (int, string, error) {
	method := details.Method
	if method == "" {
		method = "GET"
	}

	var bodyReader io.Reader
	if details.Payload != "" {
		bodyReader = strings.NewReader(details.Payload)
	}

	// Per-request timeout from job config
	timeout := time.Duration(details.TimeoutSeconds) * time.Second
	if timeout <= 0 {
		timeout = 30 * time.Second
	}
	reqCtx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	req, err := http.NewRequestWithContext(reqCtx, method, details.Url, bodyReader)
	if err != nil {
		return 0, "", fmt.Errorf("build request: %w", err)
	}

	if details.Payload != "" {
		req.Header.Set("Content-Type", "application/json")
	}

	// Parse and apply custom headers
	if details.Headers != "" {
		var headers map[string]string
		if jsonErr := json.Unmarshal([]byte(details.Headers), &headers); jsonErr == nil {
			for k, v := range headers {
				req.Header.Set(k, v)
			}
		}
	}

	resp, err := p.httpClient.Do(req)
	if err != nil {
		return 0, "", fmt.Errorf("http request: %w", err)
	}
	defer resp.Body.Close()

	limited := io.LimitReader(resp.Body, maxResponseBodyBytes)
	bodyBytes, _ := io.ReadAll(limited)
	return resp.StatusCode, strings.TrimSpace(string(bodyBytes)), nil
}
