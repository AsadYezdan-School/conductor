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


	failureCodes := []int{400, 403, 404, 500, 502, 503}

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		delaySecs := rand.Intn(10) + 1

		code := statusCode
		intentional := rand.Intn(3) == 0
		if intentional {
			code = failureCodes[rand.Intn(len(failureCodes))]
		}

		var msg string
		if intentional {
			msg = fmt.Sprintf("intentional failure: received %s %s — sleeping %ds, responding %d", r.Method, r.URL.Path, delaySecs, code)
		} else {
			msg = fmt.Sprintf("received %s %s — sleeping %ds, responding %d", r.Method, r.URL.Path, delaySecs, code)
		}

		log.Print(msg)
		time.Sleep(time.Duration(delaySecs) * time.Second)
		w.WriteHeader(code)
		fmt.Fprintf(w, `{"message":%q}`, msg)
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
