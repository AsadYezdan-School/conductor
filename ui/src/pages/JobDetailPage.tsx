import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { PencilIcon, PauseIcon, PlayIcon, ChevronLeftIcon } from 'lucide-react';
import { api } from '../api/client';
import { isTerminal } from '../api/status';
import type { JobRunSummary } from '../api/types';
import { JobStatusBadge } from '../components/JobStatusBadge';
import { CronHelper } from '../components/CronHelper';
import { RunHistoryTable } from '../components/RunHistoryTable';
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

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex gap-4 py-2 border-b last:border-0">
      <span className="w-36 flex-shrink-0 text-sm font-medium text-gray-500">{label}</span>
      <span className="text-sm text-gray-900 break-all">{value ?? '—'}</span>
    </div>
  );
}

export function JobDetailPage() {
  const { jobFamilyId } = useParams<{ jobFamilyId: string }>();
  const queryClient = useQueryClient();
  const [showEdit, setShowEdit] = useState(false);
  const [page, setPage] = useState(0);
  const LIMIT = 20;

  const { data: job, isLoading, error } = useQuery({
    queryKey: ['job', jobFamilyId],
    queryFn: () => api.getJob(jobFamilyId!),
    enabled: !!jobFamilyId,
  });

  const { data: runs = [] } = useQuery({
    queryKey: ['runs', jobFamilyId, page],
    queryFn: () => api.listRuns(jobFamilyId!, page, LIMIT),
    enabled: !!jobFamilyId,
    // Fast poll when the most recent run is still in progress.
    refetchInterval: (query) => {
      const data = query.state.data as JobRunSummary[] | undefined;
      const latest = data?.[0];
      return latest && !isTerminal(latest.status) ? 2_000 : 5_000;
    },
  });

  const parkMutation = useMutation({
    mutationFn: () =>
      job!.isParked ? api.unparkJob(jobFamilyId!) : api.parkJob(jobFamilyId!),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['job', jobFamilyId] });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success(result.isParked ? 'Job parked' : 'Job unparked');
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (isLoading) return <div className="p-8 text-gray-500">Loading…</div>;
  if (error || !job) return <div className="p-8 text-red-600">Job not found.</div>;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4">
        <Link to="/" className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-3">
          <ChevronLeftIcon size={14} /> All jobs
        </Link>
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-semibold text-gray-900">{job.name}</h1>
          <JobStatusBadge isParked={job.isParked} />
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={() => parkMutation.mutate()}
              disabled={parkMutation.isPending}
              className="flex items-center gap-1.5 rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {job.isParked ? <PlayIcon size={13} /> : <PauseIcon size={13} />}
              {job.isParked ? 'Unpark' : 'Park'}
            </button>
            <button
              onClick={() => setShowEdit(true)}
              className="flex items-center gap-1.5 rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
            >
              <PencilIcon size={13} /> Edit
            </button>
          </div>
        </div>
      </header>

      <main className="px-8 py-6 space-y-6">
        <div className="rounded-lg border bg-white p-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Definition</h2>
          <InfoRow label="Version" value={`v${job.version}`} />
          <InfoRow label="Job type" value={job.jobType} />
          <InfoRow label="Created" value={new Date(job.createdAt).toLocaleString()} />
          <InfoRow label="Max retries" value={job.maxRetries} />
          <InfoRow
            label="Schedule"
            value={
              <span>
                <code className="font-mono">{job.cron}</code>
                <CronHelper cron={job.cron} />
              </span>
            }
          />
          <InfoRow
            label="Next run"
            value={job.nextScheduledAt ? new Date(job.nextScheduledAt).toLocaleString() : null}
          />
          <InfoRow
            label="Last triggered"
            value={job.lastTriggeredAt ? new Date(job.lastTriggeredAt).toLocaleString() : null}
          />
        </div>

        <div className="rounded-lg border bg-white p-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">HTTP Config</h2>
          <InfoRow label="URL" value={<a href={job.url} className="text-blue-600 hover:underline break-all">{job.url}</a>} />
          <InfoRow label="Method" value={<code>{job.method}</code>} />
          <InfoRow label="Timeout" value={`${job.timeoutSeconds}s`} />
          {job.headers && (
            <InfoRow
              label="Headers"
              value={
                <pre className="text-xs bg-gray-50 rounded p-2 overflow-x-auto">
                  {JSON.stringify(JSON.parse(job.headers), null, 2)}
                </pre>
              }
            />
          )}
          {job.payload && (
            <InfoRow
              label="Payload"
              value={
                <pre className="text-xs bg-gray-50 rounded p-2 overflow-x-auto">
                  {JSON.stringify(JSON.parse(job.payload), null, 2)}
                </pre>
              }
            />
          )}
        </div>

        <div className="rounded-lg border bg-white p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">Run History</h2>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded border px-2 py-1 text-xs disabled:opacity-40"
              >
                ← Prev
              </button>
              <span className="text-xs text-gray-500">Page {page + 1}</span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={runs.length < LIMIT}
                className="rounded border px-2 py-1 text-xs disabled:opacity-40"
              >
                Next →
              </button>
            </div>
          </div>
          <RunHistoryTable runs={runs} />
        </div>
      </main>

      {showEdit && (
        <Modal title="Edit Job" onClose={() => setShowEdit(false)}>
          <JobForm existing={job} onClose={() => setShowEdit(false)} />
        </Modal>
      )}
    </div>
  );
}
