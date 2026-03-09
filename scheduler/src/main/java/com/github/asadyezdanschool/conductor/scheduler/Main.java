package com.github.asadyezdanschool.conductor.scheduler;

public class Main {
    static String greeting() {
        return "Hello World from Java 25 in the scheduler built with Bazel";
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.out.println(greeting());
            Thread.sleep(1000);
        }
    }
}
