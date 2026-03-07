package main

import (
    "fmt"
    "time"
)

func main() {
    for true {
    	fmt.Println("hello world from Go 1.26.0 built in bazel")
    	time.Sleep(1 * time.Second)
    }
}
