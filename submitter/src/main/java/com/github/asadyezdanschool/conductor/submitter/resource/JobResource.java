package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationResponse;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import com.github.asadyezdanschool.conductor.submitter.service.JobService;

import javax.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource {

    private static final Logger log = Logger.getLogger(JobResource.class.getName());

    private final JobService jobService;

    @Inject
    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @POST
    public Response createJob(JobCreationRequest req) {
        log.info("POST /jobs");
        if (req == null) {
            throw new ValidationException(List.of("request body is required"));
        }
        JobCreationResponse result = jobService.createJob(req);
        return Response.status(201).entity(result).build();
    }

    @PUT
    @Path("/{jobFamilyId}")
    public Response editJob(@PathParam("jobFamilyId") String jobFamilyId, EditJobRequest req) {
        log.info("PUT /jobs/{}" + jobFamilyId);
        UUID familyId = parseUuid(jobFamilyId);
        if (req == null) {
            throw new ValidationException(List.of("request body is required"));
        }
        JobCreationResponse result = jobService.editJob(familyId, req);
        return Response.ok(result).build();
    }

    @POST
    @Path("/{jobFamilyId}/park")
    public Response parkJob(@PathParam("jobFamilyId") String jobFamilyId) {
        log.info("POST /jobs/" + jobFamilyId + "/park");
        UUID familyId = parseUuid(jobFamilyId);
        ParkStatusResponse result = jobService.parkJob(familyId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/{jobFamilyId}/unpark")
    public Response unparkJob(@PathParam("jobFamilyId") String jobFamilyId) {
        log.info("POST /jobs/" + jobFamilyId + "/unpark");
        UUID familyId = parseUuid(jobFamilyId);
        ParkStatusResponse result = jobService.unparkJob(familyId);
        return Response.ok(result).build();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of("invalid UUID: " + value));
        }
    }
}