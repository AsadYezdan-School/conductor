package main

import (
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"os"
	"strconv"
	"time"
)

func main() {
	port := getEnv("MOCK_LISTENER_PORT", "8081")
	statusCode := getEnvInt("RESPONSE_STATUS_CODE", 200)

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		delaySecs := rand.Intn(10) + 1
		log.Printf("received %s %s — sleeping %ds, responding %d", r.Method, r.URL.Path, delaySecs, statusCode)
		time.Sleep(time.Duration(delaySecs) * time.Second)
		w.WriteHeader(statusCode)
		fmt.Fprintf(w, `{"status":%d}`, statusCode)
	})

	addr := ":" + port
	log.Printf("mock-listener-service listening on %s (random delay=1-10s status=%d)", addr, statusCode)
	if err := http.ListenAndServe(addr, nil); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
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
