import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PlusIcon, PencilIcon, PauseIcon, PlayIcon, ArrowRightIcon } from 'lucide-react';
import { api } from '../api/client';
import { isTerminal } from '../api/status';
import type { JobSummary } from '../api/types';
import { JobStatusBadge } from '../components/JobStatusBadge';
import { CronHelper } from '../components/CronHelper';
import { JobForm } from '../components/JobForm';

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-xl rounded-lg bg-white shadow-xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>
        <div className="px-6 py-4">{children}</div>
      </div>
    </div>
  );
}

export function JobListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);

  const { data: jobs = [], isLoading, error } = useQuery({
    queryKey: ['jobs'],
    queryFn: api.listJobs,
    // Jobs fire at most once per minute, so there's no value polling faster than 15s.
    // Speed up slightly when a run is actively in progress to catch status transitions.
    refetchInterval: (query) => {
      const data = query.state.data as JobSummary[] | undefined;
      const hasActiveRun = data?.some((j) => !isTerminal(j.latestRunStatus) && j.latestRunStatus !== null);
      return hasActiveRun ? 15_000 : 30_000;
    },
  });

  const parkMutation = useMutation({
    mutationFn: (job: JobSummary) =>
      job.isParked ? api.unparkJob(job.jobFamilyId) : api.parkJob(job.jobFamilyId),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success(result.isParked ? 'Job parked' : 'Job unparked');
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (isLoading) return <div className="p-8 text-gray-500">Loading…</div>;
  if (error) return <div className="p-8 text-red-600">Failed to load jobs.</div>;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Conductor</h1>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1.5 rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
        >
          <PlusIcon size={14} />
          New Job
        </button>
      </header>

      <main className="px-8 py-6">
        {jobs.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 bg-white p-12 text-center">
            <p className="text-gray-500">No jobs yet.</p>
            <button
              onClick={() => setShowCreate(true)}
              className="mt-3 text-sm text-blue-600 hover:underline"
            >
              Create your first job →
            </button>
          </div>
        ) : (
          <div className="overflow-hidden rounded-lg border bg-white">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-6 py-3">Name</th>
                  <th className="px-6 py-3">Schedule</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Next Run</th>
                  <th className="px-6 py-3">Last Run</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {jobs.map((job) => (
                  <tr key={job.jobFamilyId} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-medium text-gray-900">{job.name}</td>
                    <td className="px-6 py-3 font-mono text-gray-600">
                      {job.cron}
                      <CronHelper cron={job.cron} />
                    </td>
                    <td className="px-6 py-3">
                      <JobStatusBadge isParked={job.isParked} />
                    </td>
                    <td className="px-6 py-3 text-gray-500">
                      {job.nextScheduledAt ? new Date(job.nextScheduledAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-3">
                      {job.latestRunStatus ? (
                        <JobStatusBadge status={job.latestRunStatus} />
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="px-6 py-3">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => parkMutation.mutate(job)}
                          title={job.isParked ? 'Unpark' : 'Park'}
                          className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                        >
                          {job.isParked ? <PlayIcon size={14} /> : <PauseIcon size={14} />}
                        </button>
                        <button
                          onClick={() => navigate(`/jobs/${job.jobFamilyId}`)}
                          title="View"
                          className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                        >
                          <ArrowRightIcon size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {showCreate && (
        <Modal title="New Job" onClose={() => setShowCreate(false)}>
          <JobForm onClose={() => setShowCreate(false)} />
        </Modal>
      )}
    </div>
  );
}
