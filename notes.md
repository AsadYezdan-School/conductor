I want to make the ui be as useful as possible from a business intelligence perspecticve. I want to be able to demonstrate its Business                        
Intelligence value through appropriate data queries and visualisations which would support                                                                                                                                       
data-driven decision-making

Develop at least 3 custom SQL queries which draw on data from more than 1                                                                                                                                                        
table and which could be used to support decision-making in the context of the                                                                                                                                                   
chosen real-world scenario   
Generate at least two data visualisations appropriate to the data analysis
techniques being adopted to help support data-driven decision making within the
context of your chosen scenario e.g., a word cloud for text analysis, line graph for
trend / predictive analysis, etc… 

Plan: Business Intelligence Analytics Dashboard

Context

Conductor is a distributed job scheduling platform. Its current UI covers operations only (create/park jobs, view run history). The notes.md assignment requires adding BI/analytics value: 3+ multi-table SQL queries and 2+
data visualisations to support data-driven decision-making (e.g. identifying unreliable jobs, peak load times, failure patterns).

No chart library is currently installed. No analytics endpoints exist in the REST API. The database has rich data across 5 tables that can be joined for analytics.

 ---
Part 1: Three SQL Queries (Backend)

Query 1 — Job Health Overview

Tables: job_definitions + job_runs (2 tables)
BI value: Which jobs are unreliable? What is the success rate per job over the last 7 days? Supports prioritising debugging effort.

SELECT
jd.name,
jd.job_family_id::text,
jd.cron,
jd.is_parked,
COUNT(jr.id) AS total_runs,
COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED') AS succeeded,
COUNT(jr.id) FILTER (WHERE jr.status = 'FAILED') AS failed,
ROUND(100.0 * COUNT(jr.id) FILTER (WHERE jr.status = 'SUCCEEDED')
/ NULLIF(COUNT(jr.id), 0), 1) AS success_rate_pct,
ROUND(AVG(jr.duration_ms) FILTER (WHERE jr.status = 'SUCCEEDED')) AS avg_duration_ms
FROM job_definitions jd
LEFT JOIN job_runs jr ON jr.job_family_id = jd.job_family_id
AND jr.scheduled_at >= NOW() - INTERVAL '7 days'
WHERE jd.is_latest = TRUE AND jd.is_deleted = FALSE
GROUP BY jd.name, jd.job_family_id, jd.cron, jd.is_parked
ORDER BY total_runs DESC

Query 2 — Hourly Run Volume Trend

Tables: job_runs + job_definitions (2 tables)
BI value: How does system throughput change over time? When do failures spike? Supports capacity planning and anomaly detection.

SELECT
DATE_TRUNC('hour', jr.scheduled_at) AS bucket,
COUNT(*) AS total_runs,
COUNT(*) FILTER (WHERE jr.status = 'SUCCEEDED') AS succeeded,
COUNT(*) FILTER (WHERE jr.status = 'FAILED') AS failed,
ROUND(AVG(jr.duration_ms)) AS avg_duration_ms
FROM job_runs jr
JOIN job_definitions jd ON jr.job_definition_id = jd.id
WHERE jr.scheduled_at >= NOW() - INTERVAL '24 hours'
GROUP BY DATE_TRUNC('hour', jr.scheduled_at)
ORDER BY bucket ASC

Query 3 — Failure Mode Analysis

Tables: job_run_events + job_runs + job_definitions (3 tables)
BI value: Which HTTP error codes are most common, and for which jobs? Supports root cause analysis.

SELECT
jd.name,
jd.job_family_id::text,
jre.http_status_code,
COUNT(*) AS occurrences,
MAX(jre.occurred_at) AS last_seen_at
FROM job_run_events jre
JOIN job_runs jr ON jre.job_run_id = jr.id
JOIN job_definitions jd ON jr.job_definition_id = jd.id
WHERE jre.status = 'FAILED'
AND jre.occurred_at >= NOW() - INTERVAL '7 days'
GROUP BY jd.name, jd.job_family_id, jre.http_status_code
ORDER BY occurrences DESC
LIMIT 20

 ---
Part 2: Two Visualisations (Frontend)

1. Line chart — Hourly run volume over the last 24 hours, with separate lines for succeeded and failed. Shows trends, peak times, failure spikes. (Recharts LineChart)
2. Bar chart — Success rate per job (from Query 1), sorted by success rate ascending so the worst performers appear first. (Recharts BarChart)

Additionally, the failure mode analysis (Query 3) is shown as a ranked table — a direct tabular visualisation.

 ---
Part 3: Implementation Steps

Backend: 3 new model records

File: submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/model/JobHealthStat.java
public record JobHealthStat(String name, String jobFamilyId, String cron, boolean isParked,
long totalRuns, long succeeded, long failed, Double successRatePct, Long avgDurationMs) {}

File: submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/model/RunTrendBucket.java
public record RunTrendBucket(String bucket, long totalRuns, long succeeded, long failed, Long avgDurationMs) {}

File: submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/model/FailureModeStat.java
public record FailureModeStat(String name, String jobFamilyId, Integer httpStatusCode, long occurrences, String lastSeenAt) {}

Backend: Add 3 methods to ReadJobRepository.java

submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/repository/ReadJobRepository.java
- getJobHealthStats() → runs Query 1, returns List<JobHealthStat>
- getRunTrend() → runs Query 2, returns List<RunTrendBucket>
- getFailureModes() → runs Query 3, returns List<FailureModeStat>

Backend: New AnalyticsResource

File: submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/resource/AnalyticsResource.java
@Path("/analytics")
@Produces(APPLICATION_JSON)
GET /analytics/job-health     → List<JobHealthStat>
GET /analytics/run-trend      → List<RunTrendBucket>
GET /analytics/failure-modes  → List<FailureModeStat>

Backend: Wire into AppModule.java

submitter/src/main/java/com/github/asadyezdanschool/conductor/submitter/AppModule.java
- Add @Provides @Singleton AnalyticsResource provideAnalyticsResource(ReadJobRepository repo)
- Add cfg.register(analyticsResource) in provideResourceConfig()

▎ Note: BUILD.bazel does NOT need modification — it uses glob(["src/main/java/**/*.java"]) which picks up all new Java files automatically.

Frontend: Add recharts

ui/package.json — add "recharts": "^2.15.3" to dependencies.

Frontend: New TypeScript types

ui/src/api/types.ts — add:
export interface JobHealthStat { name, jobFamilyId, cron, isParked, totalRuns, succeeded, failed, successRatePct, avgDurationMs }
export interface RunTrendBucket { bucket, totalRuns, succeeded, failed, avgDurationMs }
export interface FailureModeStat { name, jobFamilyId, httpStatusCode, occurrences, lastSeenAt }

Frontend: New API calls

ui/src/api/client.ts — add:
- getJobHealth() → GET /api/analytics/job-health
- getRunTrend() → GET /api/analytics/run-trend
- getFailureModes() → GET /api/analytics/failure-modes

Frontend: New AnalyticsPage

File: ui/src/pages/AnalyticsPage.tsx — contains:
- KPI summary row: total runs (24h), success rate (24h), jobs with failures
- Visualisation 1: LineChart — run trend over last 24h (succeeded + failed lines)
- Visualisation 2: BarChart — success rate % per job (sorted worst first)
- Failure modes table: job name, HTTP status code, occurrences, last seen

Frontend: Routing & Navigation

ui/src/App.tsx — add route: <Route path="/analytics" element={<AnalyticsPage />} />
ui/src/pages/JobListPage.tsx — add "Analytics" navigation button/link in the page header

 ---
Files Summary

Create (new):

┌───────────────────────────────────────────────┬────────────────────────────────────────────────┐
│                     File                      │                    Purpose                     │
├───────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ submitter/.../model/JobHealthStat.java        │ DTO for job health query                       │
├───────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ submitter/.../model/RunTrendBucket.java       │ DTO for hourly trend query                     │
├───────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ submitter/.../model/FailureModeStat.java      │ DTO for failure mode query                     │
├───────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ submitter/.../resource/AnalyticsResource.java │ REST resource for 3 analytics endpoints        │
├───────────────────────────────────────────────┼────────────────────────────────────────────────┤
│ ui/src/pages/AnalyticsPage.tsx                │ Analytics dashboard page with 2 charts + table │
└───────────────────────────────────────────────┴────────────────────────────────────────────────┘

Modify (existing):

┌─────────────────────────────────────────────────┬───────────────────────────────────────────────────┐
│                      File                       │                      Change                       │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ submitter/.../repository/ReadJobRepository.java │ Add 3 new SQL query methods                       │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ submitter/.../AppModule.java                    │ Register AnalyticsResource in DI + ResourceConfig │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ ui/src/api/types.ts                             │ Add 3 analytics TypeScript interfaces             │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ ui/src/api/client.ts                            │ Add 3 analytics API fetch functions               │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ ui/src/App.tsx                                  │ Add /analytics route                              │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ ui/src/pages/JobListPage.tsx                    │ Add analytics nav link                            │
├─────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ ui/package.json                                 │ Add recharts dependency                           │
└─────────────────────────────────────────────────┴───────────────────────────────────────────────────┘

 ---
Verification

1. Backend: Run bazel build //submitter:Submitter — should compile cleanly with no changes to BUILD.bazel.
2. REST: curl http://localhost:8080/analytics/job-health | jq . — returns array of job health stats.
3. Frontend: cd ui && npm install && npm run dev — dev server starts; navigate to /analytics and verify both charts render.
4. SQL correctness: Each query should return results when there is run history; empty arrays are valid when there are no runs.