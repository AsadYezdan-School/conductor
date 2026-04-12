package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.repository.ReadJobRepository;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/runs")
@Produces(MediaType.APPLICATION_JSON)
public class RunResource {

    private static final Logger log = Logger.getLogger(RunResource.class.getName());

    private final ReadJobRepository readRepo;

    @Inject
    public RunResource(ReadJobRepository readRepo) {
        this.readRepo = readRepo;
    }

    @GET
    @Path("/{runId}/events")
    public Response listRunEvents(@PathParam("runId") String runId) {
        log.info("GET /runs/" + runId + "/events");
        UUID id = parseUuid(runId);
        return Response.ok(readRepo.listRunEvents(id)).build();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of("invalid UUID: " + value));
        }
    }
}
