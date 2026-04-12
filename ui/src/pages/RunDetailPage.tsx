import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeftIcon } from 'lucide-react';
import { api } from '../api/client';
import { isTerminal } from '../api/status';
import type { RunEvent } from '../api/types';
import { JobStatusBadge } from '../components/JobStatusBadge';
import { RunEventTimeline } from '../components/RunEventTimeline';


export function RunDetailPage() {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();

  const { data: events = [], isLoading, error } = useQuery({
    queryKey: ['events', runId],
    queryFn: () => api.listRunEvents(runId!),
    enabled: !!runId,
    // Poll every 500ms while the run is in progress; stop once it reaches a terminal state.
    refetchInterval: (query) => {
      const data = query.state.data as RunEvent[] | undefined;
      const lastStatus = data?.at(-1)?.status;
      return isTerminal(lastStatus) ? false : 500;
    },
  });

  if (isLoading) return <div className="p-8 text-gray-500">Loading…</div>;
  if (error) return <div className="p-8 text-red-600">Failed to load run events.</div>;

  const finalEvent = events.at(-1) ?? null;
  const firstEvent = events.at(0) ?? null;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-3"
        >
          <ChevronLeftIcon size={14} /> Back
        </button>
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-semibold text-gray-900">
            Run <code className="text-base font-mono text-gray-600">{runId}</code>
          </h1>
          {finalEvent && <JobStatusBadge status={finalEvent.status} />}
        </div>
        {firstEvent && (
          <p className="mt-1 text-sm text-gray-500">
            Started {new Date(firstEvent.occurredAt).toLocaleString()}
          </p>
        )}
      </header>

      <main className="px-8 py-6">
        <div className="rounded-lg border bg-white p-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-6">Event Timeline</h2>
          <RunEventTimeline events={events} />
        </div>
      </main>
    </div>
  );
}
