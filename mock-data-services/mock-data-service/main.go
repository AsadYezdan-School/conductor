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
	intervalSecs := getEnvInt("SUBMIT_INTERVAL_SECONDS", 1)

	log.Printf("mock-data-service starting — submitter=%s listener=%s interval=%ds",
		submitterURL, mockListenerURL, intervalSecs)

	submitJob(submitterURL, mockListenerURL)

	ticker := time.NewTicker(time.Duration(intervalSecs) * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		submitJob(submitterURL, mockListenerURL)
	}
}

func submitJob(submitterURL, listenerURL string) {
	body := jobRequest{
		Name:           fmt.Sprintf("mock-job-%d", time.Now().Unix()),
		Cron:           "* * * * *",
		URL:            listenerURL,
		Method:         "GET",
		TimeoutSeconds: 30,
	}

	data, err := json.Marshal(body)
	if err != nil {
		log.Printf("marshal error: %v", err)
		return
	}

	resp, err := http.Post(submitterURL+"/jobs", "application/json", bytes.NewReader(data))
	if err != nil {
		log.Printf("submit error: %v", err)
		return
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusCreated {
		log.Printf("unexpected status %d: %s", resp.StatusCode, respBody)
		return
	}

	var result jobResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		log.Printf("submitted job (unparseable response): %s", respBody)
		return
	}
	log.Printf("submitted job family_id=%s url=%s", result.JobFamilyID, listenerURL)
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
