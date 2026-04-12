package client

import (
	"os"

	pb "asadyezdanschool/conductor/worker/gen/execution"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

const (
	envSchedulerAddr     = "SCHEDULER_GRPC_ADDRESS"
	defaultSchedulerAddr = "localhost:50052"
)

// SchedulerClient holds the gRPC connection and the JobExecutionService stub.
// Create it once at startup with NewSchedulerClient and reuse across goroutines —
// gRPC stubs are safe for concurrent use.
type SchedulerClient struct {
	conn *grpc.ClientConn
	Stub pb.JobExecutionServiceClient
}

// NewSchedulerClient dials the scheduler's execution gRPC server.
// Address is read from SCHEDULER_GRPC_ADDRESS (default localhost:50052).
func NewSchedulerClient() (*SchedulerClient, error) {
	addr := os.Getenv(envSchedulerAddr)
	if addr == "" {
		addr = defaultSchedulerAddr
	}
	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, err
	}
	stub := pb.NewJobExecutionServiceClient(conn)
	return &SchedulerClient{conn: conn, Stub: stub}, nil
}

// Close drains and closes the underlying gRPC connection.
// Call this during graceful shutdown.
func (c *SchedulerClient) Close() error {
	return c.conn.Close()
}
