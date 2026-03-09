package main

import (
    "strings"
    "testing"
)

func TestGreetingIncludesGoVersion(t *testing.T) {
    got := greeting()
    if got == "" {
        t.Fatal("greeting should not be empty")
    }
    if want := "Go 1.26.0"; !strings.Contains(got, want) {
        t.Fatalf("greeting %q should contain %q", got, want)
    }
}
