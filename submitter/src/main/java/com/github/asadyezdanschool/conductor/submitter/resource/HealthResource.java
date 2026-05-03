package com.github.asadyezdanschool.conductor.submitter.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.TEXT_PLAIN)
public class HealthResource {

    @GET
    public Response health() {
        return Response.ok("ok").build();
    }
}
