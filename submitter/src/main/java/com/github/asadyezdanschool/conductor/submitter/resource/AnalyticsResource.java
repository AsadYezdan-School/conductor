package com.github.asadyezdanschool.conductor.submitter.resource;

import com.github.asadyezdanschool.conductor.submitter.exception.ValidationException;
import com.github.asadyezdanschool.conductor.submitter.model.AlertBreach;
import com.github.asadyezdanschool.conductor.submitter.model.FailureModeStat;
import com.github.asadyezdanschool.conductor.submitter.model.JobFamilyTrendBucket;
import com.github.asadyezdanschool.conductor.submitter.model.JobHealthStat;
import com.github.asadyezdanschool.conductor.submitter.model.RunTrendBucket;
import com.github.asadyezdanschool.conductor.submitter.repository.ReadJobRepository;

import javax.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
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
    public Response getJobHealth(@QueryParam("window") @DefaultValue("7d") String window) {
        log.info("GET /analytics/job-health?window=" + window);
        String pgInterval = resolveHealthInterval(window);
        List<JobHealthStat> stats = readRepo.getJobHealthStats(pgInterval);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/run-trend")
    public Response getRunTrend(@QueryParam("window") @DefaultValue("24h") String window) {
        log.info("GET /analytics/run-trend?window=" + window);
        String[] resolved = resolveTrendWindow(window);
        List<RunTrendBucket> buckets = readRepo.getRunTrend(resolved[0], resolved[1]);
        return Response.ok(buckets).build();
    }

    @GET
    @Path("/failure-modes")
    public Response getFailureModes() {
        log.info("GET /analytics/failure-modes");
        List<FailureModeStat> modes = readRepo.getFailureModes();
        return Response.ok(modes).build();
    }

    @GET
    @Path("/jobs/{familyId}/trend")
    public Response getJobTrend(
            @PathParam("familyId") String familyId,
            @QueryParam("window") @DefaultValue("7d") String window) {
        log.info("GET /analytics/jobs/" + familyId + "/trend?window=" + window);
        UUID id = parseUuid(familyId);
        String pgInterval = resolveJobTrendInterval(window);
        List<JobFamilyTrendBucket> buckets = readRepo.getJobFamilyTrend(id, pgInterval);
        return Response.ok(buckets).build();
    }

    @GET
    @Path("/alerts")
    public Response getAlerts() {
        log.info("GET /analytics/alerts");
        List<AlertBreach> breaches = readRepo.getAlertBreaches();
        return Response.ok(breaches).build();
    }

    private static String resolveHealthInterval(String window) {
        return switch (window) {
            case "7d"  -> "7 days";
            case "30d" -> "30 days";
            case "90d" -> "90 days";
            default    -> throw new ValidationException(List.of("window must be one of: 7d, 30d, 90d"));
        };
    }

    private static String[] resolveTrendWindow(String window) {
        return switch (window) {
            case "24h" -> new String[]{"24 hours", "hour"};
            case "7d"  -> new String[]{"7 days",   "day"};
            case "30d" -> new String[]{"30 days",  "day"};
            default    -> throw new ValidationException(List.of("window must be one of: 24h, 7d, 30d"));
        };
    }

    private static String resolveJobTrendInterval(String window) {
        return switch (window) {
            case "7d"  -> "7 days";
            case "30d" -> "30 days";
            default    -> throw new ValidationException(List.of("window must be one of: 7d, 30d"));
        };
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of("invalid UUID: " + value));
        }
    }
}
