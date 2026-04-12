package com.github.asadyezdanschool.conductor.submitter.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}