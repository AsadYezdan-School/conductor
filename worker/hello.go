package main

import (
    "fmt"
    "time"
)

func greeting() string {
    return "hello world from Go 1.26.0 built in bazel from rebuild and moving bazel rc we are trying to deploy with AWS"
}

func main() {
    for true {
        fmt.Println(greeting())
        time.Sleep(1 * time.Second)
    }
}
