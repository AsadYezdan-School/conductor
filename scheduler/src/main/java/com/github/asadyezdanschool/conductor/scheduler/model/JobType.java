package com.github.asadyezdanschool.conductor.scheduler.model;

/** Domain mirror of the JobType proto enum. Keeps proto types out of business logic. */
public enum JobType {
    HTTP,
    SHELL,
    PYTHON;

    public static JobType fromProto(com.github.asadyezdanschool.conductor.grpc.execution.JobType proto) {
        return switch (proto) {
            case HTTP   -> HTTP;
            case SHELL  -> SHELL;
            case PYTHON -> PYTHON;
            default -> throw new IllegalArgumentException("Unmapped proto JobType: " + proto);
        };
    }

    public com.github.asadyezdanschool.conductor.grpc.execution.JobType toProto() {
        return switch (this) {
            case HTTP   -> com.github.asadyezdanschool.conductor.grpc.execution.JobType.HTTP;
            case SHELL  -> com.github.asadyezdanschool.conductor.grpc.execution.JobType.SHELL;
            case PYTHON -> com.github.asadyezdanschool.conductor.grpc.execution.JobType.PYTHON;
        };
    }

    /** Parse from the DB ENUM string value (case-insensitive). */
    public static JobType fromString(String value) {
        return switch (value.toUpperCase()) {
            case "HTTP"   -> HTTP;
            case "SHELL"  -> SHELL;
            case "PYTHON" -> PYTHON;
            default -> throw new IllegalArgumentException("Unknown job type string: " + value);
        };
    }
}
