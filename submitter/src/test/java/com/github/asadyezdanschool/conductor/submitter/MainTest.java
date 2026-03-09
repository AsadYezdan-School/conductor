package com.github.asadyezdanschool.conductor.submitter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void greetingMentionsJava25() {
        assertTrue(Main.greeting().contains("Java 25"));
    }
}
