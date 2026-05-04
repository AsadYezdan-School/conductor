export interface JobSummary {
  jobFamilyId: string;
  jobDefinitionId: string;
  version: number;
  name: string;
  cron: string;
  jobType: string;
  isParked: boolean;
  nextScheduledAt: string | null;
  lastTriggeredAt: string | null;
  latestRunStatus: string | null;
}

export interface JobDetail {
  jobFamilyId: string;
  jobDefinitionId: string;
  version: number;
  name: string;
  cron: string;
  jobType: string;
  isParked: boolean;
  maxRetries: number;
  createdAt: string;
  nextScheduledAt: string | null;
  lastTriggeredAt: string | null;
  url: string;
  method: string;
  payload: string | null;
  headers: Record<string, string> | null;
  timeoutSeconds: number;
}

export interface JobRunSummary {
  runId: string;
  jobFamilyId: string;
  status: string;
  attemptNumber: number;
  scheduledAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

export interface RunEvent {
  eventId: string;
  status: string;
  message: string | null;
  httpStatusCode: number | null;
  responseBody: string | null;
  occurredAt: string;
  source: string;
}

export interface JobCreationRequest {
  name: string;
  cron: string;
  url: string;
  method: string;
  timeoutSeconds: number;
  payload?: Record<string, unknown> | null;
  headers?: Record<string, string> | null;
}

export interface EditJobRequest {
  name: string;
  cron: string;
  url: string;
  method: string;
  timeoutSeconds: number;
  payload?: Record<string, unknown> | null;
  headers?: Record<string, string> | null;
}

export interface JobCreationResponse {
  jobFamilyId: string;
  jobDefinitionId: string;
  version: number;
}

export interface ParkStatusResponse {
  jobFamilyId: string;
  isParked: boolean;
}

export interface JobHealthStat {
  name: string;
  jobFamilyId: string;
  cron: string;
  isParked: boolean;
  totalRuns: number;
  succeeded: number;
  failed: number;
  successRatePct: number | null;
  avgDurationMs: number | null;
  p50DurationMs: number | null;
  p95DurationMs: number | null;
  p99DurationMs: number | null;
}

export interface RunTrendBucket {
  bucket: string;
  totalRuns: number;
  succeeded: number;
  failed: number;
  avgDurationMs: number | null;
}

export interface FailureModeStat {
  name: string;
  jobFamilyId: string;
  httpStatusCode: number | null;
  occurrences: number;
  lastSeenAt: string | null;
}

export interface AlertConfigRequest {
  minSuccessRatePct: number | null;
  maxAvgDurationMs: number | null;
}

export interface AlertConfigResponse {
  jobFamilyId: string;
  minSuccessRatePct: number | null;
  maxAvgDurationMs: number | null;
  updatedAt: string;
}

export interface AlertBreach {
  jobFamilyId: string;
  name: string;
  actualSuccessRatePct: number | null;
  thresholdSuccessRatePct: number | null;
  actualAvgDurationMs: number | null;
  thresholdAvgDurationMs: number | null;
  breachedFields: string[];
  downstreamCount: number;
}

export interface JobFamilyTrendBucket {
  bucket: string;
  totalRuns: number;
  succeeded: number;
  failed: number;
  avgDurationMs: number | null;
}

export interface JobRef {
  jobFamilyId: string;
  name: string;
}

export interface JobDependenciesResponse {
  upstreams: JobRef[];
  downstreams: JobRef[];
}

export interface AddDependencyRequest {
  dependsOnFamilyId: string;
}
