import { fetchAuthSession } from 'aws-amplify/auth';
import type {
  EditJobRequest,
  FailureModeStat,
  JobCreationRequest,
  JobCreationResponse,
  JobDetail,
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

  getJobHealth(): Promise<JobHealthStat[]> {
    return request('/analytics/job-health');
  },

  getRunTrend(): Promise<RunTrendBucket[]> {
    return request('/analytics/run-trend');
  },

  getFailureModes(): Promise<FailureModeStat[]> {
    return request('/analytics/failure-modes');
  },
};
