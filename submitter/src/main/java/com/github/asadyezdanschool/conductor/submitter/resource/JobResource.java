package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.AddDependencyRequest;
import com.github.asadyezdanschool.conductor.submitter.model.AlertConfigRequest;
import com.github.asadyezdanschool.conductor.submitter.model.EditJobRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationRequest;
import com.github.asadyezdanschool.conductor.submitter.model.JobCreationResponse;
import com.github.asadyezdanschool.conductor.submitter.model.ParkStatusResponse;
import com.github.asadyezdanschool.conductor.submitter.repository.AlertConfigRepository;
import com.github.asadyezdanschool.conductor.submitter.repository.DependencyRepository;
import com.github.asadyezdanschool.conductor.submitter.repository.ReadJobRepository;
import com.github.asadyezdanschool.conductor.submitter.service.JobService;

import javax.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
    private final ReadJobRepository readRepo;
    private final AlertConfigRepository alertConfigRepo;
    private final DependencyRepository depRepo;

    @Inject
    public JobResource(JobService jobService, ReadJobRepository readRepo,
                       AlertConfigRepository alertConfigRepo, DependencyRepository depRepo) {
        this.jobService = jobService;
        this.readRepo = readRepo;
        this.alertConfigRepo = alertConfigRepo;
        this.depRepo = depRepo;
    }

    @GET
    public Response listJobs() {
        log.info("GET /jobs");
        return Response.ok(readRepo.listJobs()).build();
    }

    @GET
    @Path("/{jobFamilyId}")
    public Response getJob(@PathParam("jobFamilyId") String jobFamilyId) {
        log.info("GET /jobs/" + jobFamilyId);
        UUID familyId = parseUuid(jobFamilyId);
        return Response.ok(readRepo.getJob(familyId)).build();
    }

    @GET
    @Path("/{jobFamilyId}/runs")
    public Response listRuns(
            @PathParam("jobFamilyId") String jobFamilyId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        log.info("GET /jobs/" + jobFamilyId + "/runs");
        UUID familyId = parseUuid(jobFamilyId);
        int offset = page * limit;
        return Response.ok(readRepo.listRuns(familyId, limit, offset)).build();
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
        log.info("PUT /jobs/" + jobFamilyId);
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

    @PUT
    @Path("/{jobFamilyId}/alert-config")
    public Response putAlertConfig(@PathParam("jobFamilyId") String jobFamilyId, AlertConfigRequest req) {
        log.info("PUT /jobs/" + jobFamilyId + "/alert-config");
        UUID familyId = parseUuid(jobFamilyId);
        if (req == null) {
            throw new ValidationException(List.of("request body is required"));
        }
        return Response.ok(alertConfigRepo.upsertAlertConfig(familyId, req)).build();
    }

    @GET
    @Path("/{jobFamilyId}/dependencies")
    public Response getDependencies(@PathParam("jobFamilyId") String jobFamilyId) {
        log.info("GET /jobs/" + jobFamilyId + "/dependencies");
        UUID familyId = parseUuid(jobFamilyId);
        return Response.ok(depRepo.getDependencies(familyId)).build();
    }

    @POST
    @Path("/{jobFamilyId}/dependencies")
    public Response addDependency(@PathParam("jobFamilyId") String jobFamilyId, AddDependencyRequest req) {
        log.info("POST /jobs/" + jobFamilyId + "/dependencies");
        UUID familyId = parseUuid(jobFamilyId);
        if (req == null || req.dependsOnFamilyId() == null) {
            throw new ValidationException(List.of("dependsOnFamilyId is required"));
        }
        UUID upstreamId = parseUuid(req.dependsOnFamilyId());
        depRepo.addDependency(familyId, upstreamId);
        return Response.ok(depRepo.getDependencies(familyId)).build();
    }

    @DELETE
    @Path("/{jobFamilyId}/dependencies/{upstreamFamilyId}")
    public Response removeDependency(
            @PathParam("jobFamilyId") String jobFamilyId,
            @PathParam("upstreamFamilyId") String upstreamFamilyId) {
        log.info("DELETE /jobs/" + jobFamilyId + "/dependencies/" + upstreamFamilyId);
        UUID familyId = parseUuid(jobFamilyId);
        UUID upstreamId = parseUuid(upstreamFamilyId);
        depRepo.removeDependency(familyId, upstreamId);
        return Response.ok().build();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of("invalid UUID: " + value));
        }
    }
}
