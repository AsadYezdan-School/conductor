package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

func main() {
	port := getEnv("PORT", "8080")
	delayMs := getEnvInt("RESPONSE_DELAY_MS", 200)
	statusCode := getEnvInt("RESPONSE_STATUS_CODE", 200)

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		log.Printf("received %s %s — sleeping %dms, responding %d", r.Method, r.URL.Path, delayMs, statusCode)
		time.Sleep(time.Duration(delayMs) * time.Millisecond)
		w.WriteHeader(statusCode)
		fmt.Fprintf(w, `{"status":%d}`, statusCode)
	})

	addr := ":" + port
	log.Printf("mock-listener-service listening on %s (delay=%dms status=%d)", addr, delayMs, statusCode)
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
