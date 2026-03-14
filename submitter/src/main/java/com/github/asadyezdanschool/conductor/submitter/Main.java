package com.github.asadyezdanschool.conductor.submitter;

public class Main {
    static String greeting() {
        return "Hello World from Java 25 built with Bazel after rebuild and moving bazelrc, we are trying to deploy with AWS";
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.out.println(greeting());
            Thread.sleep(1000);
        }
    }
}
