package processor

import (
	"context"
	"testing"

	pb "asadyezdanschool/conductor/worker/gen/execution"
	"google.golang.org/grpc"
)

// mockJobExecutionClient is a hand-written mock for pb.JobExecutionServiceClient.
// We avoid external mock frameworks to keep the worker lightweight.
type mockJobExecutionClient struct {
	getHttpRunDetailsFunc func(ctx context.Context, in *pb.GetHttpRunDetailsRequest, opts ...grpc.CallOption) (*pb.GetHttpRunDetailsResponse, error)
	reportStatusFunc      func(ctx context.Context, in *pb.ReportStatusRequest, opts ...grpc.CallOption) (*pb.ReportStatusResponse, error)
}

func (m *mockJobExecutionClient) GetHttpRunDetails(ctx context.Context, in *pb.GetHttpRunDetailsRequest, opts ...grpc.CallOption) (*pb.GetHttpRunDetailsResponse, error) {
	return m.getHttpRunDetailsFunc(ctx, in, opts...)
}

func (m *mockJobExecutionClient) ReportStatus(ctx context.Context, in *pb.ReportStatusRequest, opts ...grpc.CallOption) (*pb.ReportStatusResponse, error) {
	return m.reportStatusFunc(ctx, in, opts...)
}

// TestProcessRun_Success verifies the happy path:
// GetHttpRunDetails → ReportStatus(RUNNING) → HTTP 200 → ReportStatus(SUCCEEDED)
func TestProcessRun_Success(t *testing.T) {
	// TODO:
	//  1. Spin up httptest.NewServer returning 200
	//  2. mockClient.getHttpRunDetailsFunc returns details pointing at the test server
	//  3. mockClient.reportStatusFunc records the statuses it receives
	//  4. proc := NewHTTPProcessor(mockClient, httpTestClient)
	//  5. result := proc.ProcessRun(context.Background(), "run-123")
	//  6. assert result == true
	//  7. assert reportStatus was called with RUNNING then SUCCEEDED
	t.Skip("TODO: implement")
}

// TestProcessRun_Non2xxResponse verifies that a non-2xx HTTP status causes
// a FAILED report and ProcessRun returns false.
func TestProcessRun_Non2xxResponse(t *testing.T) {
	// TODO:
	//  1. httptest.NewServer returning 503
	//  2. assert ProcessRun returns false
	//  3. assert reportStatus called with FAILED + http_status_code=503
	t.Skip("TODO: implement")
}

// TestProcessRun_GetHttpRunDetailsFails verifies that when GetHttpRunDetails returns
// an error, ProcessRun returns false without calling ReportStatus.
func TestProcessRun_GetHttpRunDetailsFails(t *testing.T) {
	// TODO:
	//  1. getHttpRunDetailsFunc returns error
	//  2. assert ProcessRun returns false
	//  3. assert reportStatusFunc never called
	t.Skip("TODO: implement")
}

// TestProcessRun_ReportStatusRunningError verifies that a failure in
// ReportStatus(RUNNING) is logged but does not abort execution.
func TestProcessRun_ReportStatusRunningError(t *testing.T) {
	// TODO:
	//  1. reportStatusFunc returns error on first call (RUNNING), success on second (SUCCEEDED)
	//  2. HTTP server returns 200
	//  3. assert ProcessRun still returns true (execution not aborted by RUNNING report failure)
	t.Skip("TODO: implement")
}

// TestProcessRun_HttpClientError verifies that a network-level error in the HTTP
// call is reported as FAILED.
func TestProcessRun_HttpClientError(t *testing.T) {
	// TODO:
	//  1. details.Url points at a server that immediately closes the connection
	//  2. assert ProcessRun returns false
	//  3. assert FAILED report with non-empty message
	t.Skip("TODO: implement")
}

// TestProcessRun_Timeout verifies that the per-request timeout from details.TimeoutSeconds
// is applied and a deadline exceeded error surfaces as FAILED.
func TestProcessRun_Timeout(t *testing.T) {
	// TODO:
	//  1. httptest.NewServer that sleeps longer than timeoutSeconds
	//  2. details.TimeoutSeconds = 1 (or very small)
	//  3. assert FAILED with message containing "deadline" or "timeout"
	t.Skip("TODO: implement")
}
