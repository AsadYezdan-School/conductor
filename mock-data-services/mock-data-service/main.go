package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
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

	submitted := 0
	for i := 1; i <= numJobs; i++ {
		if submitJob(submitterURL, mockListenerURL, i) {
			submitted++
		}
	}

	log.Printf("Startup complete — submitted %d/%d jobs. Sleeping indefinitely.", submitted, numJobs)
	select {} // block forever so ECS doesn't restart the container
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

func submitJob(submitterURL, listenerURL string, index int) bool {
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
		return false
	}

	resp, err := http.Post(submitterURL+"/jobs", "application/json", bytes.NewReader(data))
	if err != nil {
		log.Printf("submit error for job %d: %v", index, err)
		return false
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusCreated {
		log.Printf("unexpected status %d for job %d: %s", resp.StatusCode, index, respBody)
		return false
	}

	var result jobResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		log.Printf("submitted job %d (unparseable response): %s", index, respBody)
		return true
	}
	log.Printf("submitted job %d family_id=%s name=%s", index, result.JobFamilyID, body.Name)

	// Small delay between submissions to avoid overwhelming the submitter at startup
	time.Sleep(100 * time.Millisecond)
	return true
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
