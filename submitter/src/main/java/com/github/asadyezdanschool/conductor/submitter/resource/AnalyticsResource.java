package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.submitter.model.FailureModeStat;
import com.github.asadyezdanschool.conductor.submitter.model.JobHealthStat;
import com.github.asadyezdanschool.conductor.submitter.model.RunTrendBucket;
import com.github.asadyezdanschool.conductor.submitter.repository.ReadJobRepository;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

@Path("/analytics")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private static final Logger log = Logger.getLogger(AnalyticsResource.class.getName());

    private final ReadJobRepository readRepo;

    @Inject
    public AnalyticsResource(ReadJobRepository readRepo) {
        this.readRepo = readRepo;
    }

    @GET
    @Path("/job-health")
    public Response getJobHealth() {
        log.info("GET /analytics/job-health");
        List<JobHealthStat> stats = readRepo.getJobHealthStats();
        return Response.ok(stats).build();
    }

    @GET
    @Path("/run-trend")
    public Response getRunTrend() {
        log.info("GET /analytics/run-trend");
        List<RunTrendBucket> buckets = readRepo.getRunTrend();
        return Response.ok(buckets).build();
    }

    @GET
    @Path("/failure-modes")
    public Response getFailureModes() {
        log.info("GET /analytics/failure-modes");
        List<FailureModeStat> modes = readRepo.getFailureModes();
        return Response.ok(modes).build();
    }
}
