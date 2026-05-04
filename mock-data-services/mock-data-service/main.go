package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"
)

type jobRequest struct {
	Name           string `json:"name"`
	Cron           string `json:"cron"`
	URL            string `json:"url"`
	Method         string `json:"method"`
	TimeoutSeconds int    `json:"timeoutSeconds"`
}

type jobResponse struct {
	JobFamilyID string `json:"jobFamilyId"`
}

type addDepRequest struct {
	DependsOnFamilyID string `json:"dependsOnFamilyId"`
}

// depEdges defines the dependency graph as [downstream, upstream] pairs (1-indexed job numbers).
//
// Layout:
//   Layer 1 — roots (01-05): no upstream deps
//   Layer 2 — (06-10): fan-in from layer 1
//   Layer 3 — (11-15): fan-in from layer 2
//   Layer 4 — (16-20): fan-in from layer 3
//
// This produces chains, diamonds, and shared upstream nodes to exercise
// the dependency gating and analytics features with realistic data.
var depEdges = [][2]int{
	// layer 2
	{6, 1}, {6, 2},
	{7, 2}, {7, 3},
	{8, 4}, {8, 5},
	{9, 3},
	{10, 5},
	// layer 3
	{11, 6},
	{12, 6}, {12, 7},
	{13, 7}, {13, 8},
	{14, 9},
	{15, 10},
	// layer 4
	{16, 11},
	{17, 12}, {17, 13},
	{18, 14},
	{19, 15},
	{20, 16}, {20, 17},
}

func main() {
	submitterURL := os.Getenv("SUBMITTER_URL")
	if submitterURL == "" {
		log.Fatal("SUBMITTER_URL not set")
	}
	mockListenerURL := os.Getenv("MOCK_LISTENER_URL")
	if mockListenerURL == "" {
		log.Fatal("MOCK_LISTENER_URL not set")
	}
	numJobs := getEnvInt("NUM_JOBS", 20)

	log.Printf("mock-data-service starting — submitter=%s listener=%s numJobs=%d",
		submitterURL, mockListenerURL, numJobs)

	waitForSubmitter(submitterURL)

	// familyIDs is 1-indexed: familyIDs[i] holds the family ID for job i.
	familyIDs := make([]string, numJobs+1)
	submitted := 0
	for i := 1; i <= numJobs; i++ {
		id, ok := submitJob(submitterURL, mockListenerURL, i)
		if ok {
			familyIDs[i] = id
			submitted++
		}
	}
	log.Printf("submitted %d/%d jobs", submitted, numJobs)

	wired := 0
	for _, edge := range depEdges {
		downstream, upstream := edge[0], edge[1]
		if downstream > numJobs || upstream > numJobs {
			continue
		}
		downstreamID, upstreamID := familyIDs[downstream], familyIDs[upstream]
		if downstreamID == "" || upstreamID == "" {
			log.Printf("skipping dep %d→%d: job was not submitted", downstream, upstream)
			continue
		}
		if err := addDependency(submitterURL, downstreamID, upstreamID); err != nil {
			log.Printf("dep %d→%d failed: %v", downstream, upstream, err)
		} else {
			log.Printf("dep mock-job-%02d depends on mock-job-%02d", downstream, upstream)
			wired++
		}
	}
	log.Printf("Startup complete — submitted %d/%d jobs, wired %d dependency edges. Sleeping indefinitely.",
		submitted, numJobs, wired)
	// Block until SIGTERM/SIGINT so the container stays running (ECS / Docker).
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGTERM, os.Interrupt)
	<-quit
}

// waitForSubmitter polls GET /jobs until the submitter responds, retrying with
// linear backoff. The JVM submitter takes several seconds to start up and the
// docker-compose health check only waits for the container to be running.
func waitForSubmitter(submitterURL string) {
	client := &http.Client{Timeout: 2 * time.Second}
	for attempt := 1; ; attempt++ {
		resp, err := client.Get(submitterURL + "/jobs")
		if err == nil {
			resp.Body.Close()
			log.Printf("submitter ready after %d probe(s)", attempt)
			return
		}
		delay := time.Duration(attempt) * time.Second
		if delay > 10*time.Second {
			delay = 10 * time.Second
		}
		log.Printf("submitter not ready (attempt %d): %v — retrying in %s", attempt, err, delay)
		time.Sleep(delay)
	}
}

// submitJob creates a job and returns its family ID.
func submitJob(submitterURL, listenerURL string, index int) (string, bool) {
	body := jobRequest{
		Name:           fmt.Sprintf("mock-job-%02d", index),
		Cron:           "* * * * *",
		URL:            listenerURL + "/ping",
		Method:         "GET",
		TimeoutSeconds: 30,
	}

	data, err := json.Marshal(body)
	if err != nil {
		log.Printf("marshal error: %v", err)
		return "", false
	}

	resp, err := http.Post(submitterURL+"/jobs", "application/json", bytes.NewReader(data))
	if err != nil {
		log.Printf("submit error for job %d: %v", index, err)
		return "", false
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusCreated {
		log.Printf("unexpected status %d for job %d: %s", resp.StatusCode, index, respBody)
		return "", false
	}

	var result jobResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		log.Printf("submitted job %d (unparseable response): %s", index, respBody)
		return "", true
	}
	log.Printf("submitted job %d family_id=%s name=%s", index, result.JobFamilyID, body.Name)

	// Small delay between submissions to avoid overwhelming the submitter at startup
	time.Sleep(100 * time.Millisecond)
	return result.JobFamilyID, true
}

func addDependency(submitterURL, downstreamFamilyID, upstreamFamilyID string) error {
	body, _ := json.Marshal(addDepRequest{DependsOnFamilyID: upstreamFamilyID})
	url := fmt.Sprintf("%s/jobs/%s/dependencies", submitterURL, downstreamFamilyID)
	resp, err := http.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		b, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("status %d: %s", resp.StatusCode, b)
	}
	return nil
}

func getEnvInt(key string, def int) int {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		log.Printf("invalid %s=%q, using default %d", key, v, def)
		return def
	}
	return n
}
