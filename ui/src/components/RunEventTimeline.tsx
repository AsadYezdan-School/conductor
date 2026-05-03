import { useState } from 'react';
import type { RunEvent } from '../api/types';
import { JobStatusBadge } from './JobStatusBadge';

interface Props {
  events: RunEvent[];
}

export function RunEventTimeline({ events }: Props) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  if (events.length === 0) {
    return <p className="text-sm text-gray-500 py-4">No events.</p>;
  }

  function toggle(id: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  return (
    <ol className="relative border-l border-gray-200 space-y-6 ml-3">
      {events.map((ev) => (
        <li key={ev.eventId} className="ml-6">
          <span className="absolute -left-2 flex h-4 w-4 items-center justify-center rounded-full bg-white ring-2 ring-gray-300" />
          <div className="flex items-center gap-2">
            <JobStatusBadge status={ev.status} />
            <span className="text-xs text-gray-400">
              {new Date(ev.occurredAt).toLocaleString()}
            </span>
            <span className="text-xs text-gray-400 italic">{ev.source}</span>
          </div>
          {ev.message && (
            <p className="mt-1 text-sm text-gray-700">{ev.message}</p>
          )}
          {(ev.httpStatusCode !== null || ev.responseBody) && (
            <div className="mt-1">
              <button
                onClick={() => toggle(ev.eventId)}
                className="text-xs text-blue-600 hover:underline"
              >
                {expanded.has(ev.eventId) ? 'Hide details' : 'Show details'}
              </button>
              {expanded.has(ev.eventId) && (
                <div className="mt-2 rounded bg-gray-50 p-3 text-xs font-mono space-y-1">
                  {ev.httpStatusCode !== null && (
                    <p className="text-gray-600">HTTP {ev.httpStatusCode}</p>
                  )}
                  {ev.responseBody && (
                    <pre className="whitespace-pre-wrap break-all text-gray-800">
                      {ev.responseBody}
                    </pre>
                  )}
                </div>
              )}
            </div>
          )}
        </li>
      ))}
    </ol>
  );
}
