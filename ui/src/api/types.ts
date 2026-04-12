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
  headers: string | null;
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
