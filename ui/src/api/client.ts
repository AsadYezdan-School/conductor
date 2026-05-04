import { fetchAuthSession } from 'aws-amplify/auth';
import type {
  AddDependencyRequest,
  AlertBreach,
  AlertConfigRequest,
  AlertConfigResponse,
  EditJobRequest,
  FailureModeStat,
  JobCreationRequest,
  JobCreationResponse,
  JobDependenciesResponse,
  JobDetail,
  JobFamilyTrendBucket,
  JobHealthStat,
  JobRunSummary,
  JobSummary,
  ParkStatusResponse,
  RunEvent,
  RunTrendBucket,
} from './types';

const BASE = '/api';

async function getToken(): Promise<string | undefined> {
  try {
    const session = await fetchAuthSession();
    return session.tokens?.idToken?.toString();
  } catch {
    return undefined;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await getToken();
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status}: ${text}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  listJobs(): Promise<JobSummary[]> {
    return request('/jobs');
  },

  getJob(jobFamilyId: string): Promise<JobDetail> {
    return request(`/jobs/${jobFamilyId}`);
  },

  listRuns(jobFamilyId: string, page = 0, limit = 20): Promise<JobRunSummary[]> {
    return request(`/jobs/${jobFamilyId}/runs?page=${page}&limit=${limit}`);
  },

  listRunEvents(runId: string): Promise<RunEvent[]> {
    return request(`/runs/${runId}/events`);
  },

  createJob(body: JobCreationRequest): Promise<JobCreationResponse> {
    return request('/jobs', { method: 'POST', body: JSON.stringify(body) });
  },

  editJob(jobFamilyId: string, body: EditJobRequest): Promise<JobCreationResponse> {
    return request(`/jobs/${jobFamilyId}`, { method: 'PUT', body: JSON.stringify(body) });
  },

  parkJob(jobFamilyId: string): Promise<ParkStatusResponse> {
    return request(`/jobs/${jobFamilyId}/park`, { method: 'POST' });
  },

  unparkJob(jobFamilyId: string): Promise<ParkStatusResponse> {
    return request(`/jobs/${jobFamilyId}/unpark`, { method: 'POST' });
  },

  getJobHealth(window: '7d' | '30d' | '90d' = '7d'): Promise<JobHealthStat[]> {
    return request(`/analytics/job-health?window=${window}`);
  },

  getRunTrend(window: '24h' | '7d' | '30d' = '24h'): Promise<RunTrendBucket[]> {
    return request(`/analytics/run-trend?window=${window}`);
  },

  getFailureModes(): Promise<FailureModeStat[]> {
    return request('/analytics/failure-modes');
  },

  getAlerts(): Promise<AlertBreach[]> {
    return request('/analytics/alerts');
  },

  getJobFamilyTrend(jobFamilyId: string, window: '7d' | '30d' = '7d'): Promise<JobFamilyTrendBucket[]> {
    return request(`/analytics/jobs/${jobFamilyId}/trend?window=${window}`);
  },

  putAlertConfig(jobFamilyId: string, body: AlertConfigRequest): Promise<AlertConfigResponse> {
    return request(`/jobs/${jobFamilyId}/alert-config`, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },

  getJobDependencies(jobFamilyId: string): Promise<JobDependenciesResponse> {
    return request(`/jobs/${jobFamilyId}/dependencies`);
  },

  addJobDependency(jobFamilyId: string, body: AddDependencyRequest): Promise<JobDependenciesResponse> {
    return request(`/jobs/${jobFamilyId}/dependencies`, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  removeJobDependency(jobFamilyId: string, upstreamFamilyId: string): Promise<void> {
    return request(`/jobs/${jobFamilyId}/dependencies/${upstreamFamilyId}`, { method: 'DELETE' });
  },
};
