package com.github.asadyezdanschool.conductor.submitter.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Logger;

@Provider
public class ExceptionMappers implements ExceptionMapper<RuntimeException> {

    private static final Logger log = Logger.getLogger(ExceptionMappers.class.getName());

    @Override
    public Response toResponse(RuntimeException ex) {
        if (ex instanceof ValidationException ve) {
            log.warning("Validation failure: " + ve.getMessage());
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("errors", ve.getErrors()))
                    .build();
        }
        if (ex instanceof NotFoundException nfe) {
            return Response.status(404)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", nfe.getMessage()))
                    .build();
        }
        if (ex instanceof ConflictException ce) {
            return Response.status(409)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", ce.getMessage()))
                    .build();
        }
        log.severe("Unhandled exception: " + ex.getMessage());
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "internal server error"))
                .build();
    }
}