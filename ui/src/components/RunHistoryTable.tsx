import { useNavigate } from 'react-router-dom';
import type { JobRunSummary } from '../api/types';
import { JobStatusBadge } from './JobStatusBadge';

interface Props {
  runs: JobRunSummary[];
}

function fmt(ts: string | null) {
  if (!ts) return '—';
  return new Date(ts).toLocaleString();
}

function fmtDuration(ms: number | null) {
  if (ms === null) return '—';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

export function RunHistoryTable({ runs }: Props) {
  const navigate = useNavigate();

  if (runs.length === 0) {
    return <p className="text-sm text-gray-500 py-4">No runs yet.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-sm">
        <thead>
          <tr className="border-b text-left text-gray-500">
            <th className="pb-2 pr-4 font-medium">Status</th>
            <th className="pb-2 pr-4 font-medium">Scheduled</th>
            <th className="pb-2 pr-4 font-medium">Started</th>
            <th className="pb-2 pr-4 font-medium">Duration</th>
            <th className="pb-2 pr-4 font-medium">Attempt</th>
            <th className="pb-2 font-medium"></th>
          </tr>
        </thead>
        <tbody>
          {runs.map((run) => (
            <tr
              key={run.runId}
              className="border-b hover:bg-gray-50 cursor-pointer"
              onClick={() => navigate(`/runs/${run.runId}`)}
            >
              <td className="py-2 pr-4">
                <JobStatusBadge status={run.status} />
              </td>
              <td className="py-2 pr-4 text-gray-700">{fmt(run.scheduledAt)}</td>
              <td className="py-2 pr-4 text-gray-700">{fmt(run.startedAt)}</td>
              <td className="py-2 pr-4 text-gray-700">{fmtDuration(run.durationMs)}</td>
              <td className="py-2 pr-4 text-gray-700">#{run.attemptNumber}</td>
              <td className="py-2 text-blue-600 text-xs">View →</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
