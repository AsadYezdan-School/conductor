import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../api/client';
import type { AlertBreach } from '../api/types';

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtMs(ms: number | null): string {
  if (ms === null) return '—';
  if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`;
  return `${ms}ms`;
}

function formatBucket(iso: string, window: string): string {
  const d = new Date(iso);
  return window === '24h'
    ? d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function barColor(rate: number): string {
  if (rate < 80) return '#ef4444';
  if (rate < 95) return '#f59e0b';
  return '#22c55e';
}

// ── Sub-components ────────────────────────────────────────────────────────────

function KpiCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border bg-white px-6 py-4">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-gray-900">{value}</p>
    </div>
  );
}

function WindowToggle<T extends string>({
  value,
  options,
  onChange,
}: {
  value: T;
  options: T[];
  onChange: (v: T) => void;
}) {
  return (
    <div className="flex overflow-hidden rounded border border-gray-200 text-xs">
      {options.map((o) => (
        <button
          key={o}
          onClick={() => onChange(o)}
          className={`px-2 py-1 ${
            o === value ? 'bg-gray-900 text-white' : 'text-gray-600 hover:bg-gray-50'
          }`}
        >
          {o}
        </button>
      ))}
    </div>
  );
}

function AlertPanel() {
  const q = useQuery({
    queryKey: ['analytics', 'alerts'],
    queryFn: api.getAlerts,
    refetchInterval: 60_000,
  });
  const breaches: AlertBreach[] = q.data ?? [];

  if (q.isLoading) return null;

  if (breaches.length === 0) {
    return (
      <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
        All monitored jobs are healthy.
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <h3 className="text-sm font-semibold text-gray-700">Active Alerts</h3>
      {breaches.map((b) => {
        const isRed = b.breachedFields.includes('successRate');
        const colorClass = isRed
          ? 'border-red-200 bg-red-50 text-red-800'
          : 'border-amber-200 bg-amber-50 text-amber-800';
        return (
          <div key={b.jobFamilyId} className={`rounded-lg border px-4 py-3 text-sm ${colorClass}`}>
            <span className="font-semibold">{b.name}</span>
            {b.breachedFields.includes('successRate') && (
              <span>
                {' '}— Success rate {b.actualSuccessRatePct?.toFixed(1) ?? '0'}% &lt; threshold{' '}
                {b.thresholdSuccessRatePct}%
              </span>
            )}
            {b.breachedFields.includes('avgDuration') && (
              <span>
                {' '}— Avg duration {fmtMs(b.actualAvgDurationMs)} &gt; threshold{' '}
                {fmtMs(b.thresholdAvgDurationMs)}
              </span>
            )}
            {b.downstreamCount > 0 && (
              <span className="ml-2 font-medium">
                ⚠ {b.downstreamCount} downstream job{b.downstreamCount > 1 ? 's' : ''} at risk
              </span>
            )}
          </div>
        );
      })}
    </div>
  );
}

function JobTrendModal({
  jobFamilyId,
  name,
  onClose,
}: {
  jobFamilyId: string;
  name: string;
  onClose: () => void;
}) {
  const [window, setWindow] = useState<'7d' | '30d'>('7d');
  const q = useQuery({
    queryKey: ['analytics', 'job-trend', jobFamilyId, window],
    queryFn: () => api.getJobFamilyTrend(jobFamilyId, window),
  });
  const data = q.data ?? [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-2xl rounded-lg bg-white shadow-xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">Trend: {name}</h2>
          <div className="flex items-center gap-3">
            <WindowToggle value={window} options={['7d', '30d'] as const} onChange={setWindow} />
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              ✕
            </button>
          </div>
        </div>
        <div className="px-6 py-4">
          {q.isLoading ? (
            <p className="text-sm text-gray-400">Loading…</p>
          ) : data.length === 0 ? (
            <p className="text-sm text-gray-400">No run data for this window.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={data} margin={{ top: 4, right: 24, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="bucket"
                  tickFormatter={(v) => new Date(v).toLocaleDateString([], { month: 'short', day: 'numeric' })}
                  tick={{ fontSize: 11 }}
                />
                <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                <Tooltip
                  labelFormatter={(v) =>
                    new Date(v as string).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
                  }
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="succeeded"
                  name="Succeeded"
                  stroke="#22c55e"
                  strokeWidth={2}
                  dot={false}
                />
                <Line
                  type="monotone"
                  dataKey="failed"
                  name="Failed"
                  stroke="#ef4444"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export function AnalyticsPage() {
  const [trendWindow, setTrendWindow] = useState<'24h' | '7d' | '30d'>('24h');
  const [healthWindow, setHealthWindow] = useState<'7d' | '30d' | '90d'>('7d');
  const [selectedJob, setSelectedJob] = useState<{ id: string; name: string } | null>(null);

  const trendQuery = useQuery({
    queryKey: ['analytics', 'run-trend', trendWindow],
    queryFn: () => api.getRunTrend(trendWindow),
  });
  const healthQuery = useQuery({
    queryKey: ['analytics', 'job-health', healthWindow],
    queryFn: () => api.getJobHealth(healthWindow),
  });
  const failureQuery = useQuery({
    queryKey: ['analytics', 'failure-modes'],
    queryFn: api.getFailureModes,
  });

  const trend = trendQuery.data ?? [];
  const health = healthQuery.data ?? [];
  const failures = failureQuery.data ?? [];

  const totalRuns = trend.reduce((s, b) => s + b.totalRuns, 0);
  const succeeded = trend.reduce((s, b) => s + b.succeeded, 0);
  const successRate = totalRuns > 0 ? `${((succeeded / totalRuns) * 100).toFixed(1)}%` : '—';
  const jobsWithFailures = health.filter((j) => j.failed > 0).length;

  const healthChartData = [...health]
    .sort((a, b) => (a.successRatePct ?? 0) - (b.successRatePct ?? 0))
    .map((j) => ({ name: j.name, successRate: j.successRatePct ?? 0, jobFamilyId: j.jobFamilyId }));

  const loading = trendQuery.isLoading || healthQuery.isLoading || failureQuery.isLoading;
  const error = trendQuery.error || healthQuery.error || failureQuery.error;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Conductor</h1>
        <div className="flex items-center gap-4">
          <Link to="/" className="text-sm text-gray-600 hover:text-gray-900">
            Jobs
          </Link>
          <span className="text-sm font-medium text-blue-600">Analytics</span>
        </div>
      </header>

      <main className="px-8 py-6 space-y-6">
        <h2 className="text-lg font-semibold text-gray-900">Analytics</h2>

        {/* Alert panel */}
        <AlertPanel />

        {loading && <p className="text-gray-500">Loading…</p>}
        {error && <p className="text-red-600">Failed to load analytics data.</p>}

        {/* KPI row */}
        <div className="grid grid-cols-3 gap-4">
          <KpiCard label={`Total Runs (${trendWindow})`} value={totalRuns} />
          <KpiCard label={`Success Rate (${trendWindow})`} value={successRate} />
          <KpiCard label="Jobs with Failures" value={jobsWithFailures} />
        </div>

        {/* Run volume trend */}
        <div className="rounded-lg border bg-white p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-700">Run Volume</h3>
            <WindowToggle
              value={trendWindow}
              options={['24h', '7d', '30d'] as const}
              onChange={setTrendWindow}
            />
          </div>
          {trend.length === 0 && !loading ? (
            <p className="text-sm text-gray-400">No run data for this window.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trend} margin={{ top: 4, right: 24, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="bucket"
                  tickFormatter={(v) => formatBucket(v, trendWindow)}
                  tick={{ fontSize: 12 }}
                />
                <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                <Tooltip
                  labelFormatter={(v) => new Date(v as string).toLocaleString()}
                  formatter={(val: number, key: string) => [
                    val,
                    key === 'succeeded' ? 'Succeeded' : 'Failed',
                  ]}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="succeeded"
                  name="Succeeded"
                  stroke="#22c55e"
                  strokeWidth={2}
                  dot={false}
                />
                <Line
                  type="monotone"
                  dataKey="failed"
                  name="Failed"
                  stroke="#ef4444"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Success rate bar chart — click a bar to open per-job trend */}
        <div className="rounded-lg border bg-white p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-700">
              Success Rate by Job — Last {healthWindow} (worst first)
            </h3>
            <WindowToggle
              value={healthWindow}
              options={['7d', '30d', '90d'] as const}
              onChange={setHealthWindow}
            />
          </div>
          {healthChartData.length === 0 && !loading ? (
            <p className="text-sm text-gray-400">No job data for this window.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={healthChartData}
                margin={{ top: 4, right: 24, left: 0, bottom: 64 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11 }}
                  angle={-30}
                  textAnchor="end"
                  interval={0}
                />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} unit="%" />
                <Tooltip formatter={(v: number) => [`${v}%`, 'Success rate']} />
                <Bar
                  dataKey="successRate"
                  name="Success rate"
                  radius={[3, 3, 0, 0]}
                  style={{ cursor: 'pointer' }}
                  onClick={(data) =>
                    setSelectedJob({ id: data.jobFamilyId as string, name: data.name as string })
                  }
                >
                  {healthChartData.map((entry, i) => (
                    <Cell key={i} fill={barColor(entry.successRate)} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
          <p className="mt-2 text-xs text-gray-400">Click a bar to view the job's run trend.</p>
        </div>

        {/* Job health table with percentile columns */}
        {health.length > 0 && (
          <div className="rounded-lg border bg-white p-6">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">
              Job Health — Last {healthWindow}
            </h3>
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-4 py-2">Job</th>
                  <th className="px-4 py-2">Total Runs</th>
                  <th className="px-4 py-2">Success Rate</th>
                  <th className="px-4 py-2">Avg Duration</th>
                  <th className="px-4 py-2">P50</th>
                  <th className="px-4 py-2">P95</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {[...health]
                  .sort((a, b) => (a.successRatePct ?? 0) - (b.successRatePct ?? 0))
                  .map((j) => (
                    <tr
                      key={j.jobFamilyId}
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => setSelectedJob({ id: j.jobFamilyId, name: j.name })}
                    >
                      <td className="px-4 py-2 font-medium text-gray-900">{j.name}</td>
                      <td className="px-4 py-2 text-gray-700">{j.totalRuns}</td>
                      <td className="px-4 py-2">
                        {j.successRatePct !== null ? (
                          <span
                            className={`font-medium ${
                              j.successRatePct < 80
                                ? 'text-red-600'
                                : j.successRatePct < 95
                                ? 'text-amber-600'
                                : 'text-green-600'
                            }`}
                          >
                            {j.successRatePct.toFixed(1)}%
                          </span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td className="px-4 py-2 text-gray-700">{fmtMs(j.avgDurationMs)}</td>
                      <td className="px-4 py-2 text-gray-700">{fmtMs(j.p50DurationMs)}</td>
                      <td className="px-4 py-2 text-gray-700">{fmtMs(j.p95DurationMs)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Failure modes table */}
        <div className="rounded-lg border bg-white p-6">
          <h3 className="mb-4 text-sm font-semibold text-gray-700">Failure Modes — Last 7 Days</h3>
          {failures.length === 0 && !loading ? (
            <p className="text-sm text-gray-400">No failures recorded in the last 7 days.</p>
          ) : (
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-4 py-2">Job</th>
                  <th className="px-4 py-2">HTTP Status</th>
                  <th className="px-4 py-2">Occurrences</th>
                  <th className="px-4 py-2">Last Seen</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {failures.map((f, i) => (
                  <tr key={i} className="hover:bg-gray-50">
                    <td className="px-4 py-2 font-medium text-gray-900">{f.name}</td>
                    <td className="px-4 py-2">
                      {f.httpStatusCode != null ? (
                        <span className="rounded bg-red-50 px-2 py-0.5 font-mono text-xs text-red-700">
                          {f.httpStatusCode}
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="px-4 py-2 text-gray-700">{f.occurrences}</td>
                    <td className="px-4 py-2 text-gray-500">
                      {f.lastSeenAt ? new Date(f.lastSeenAt).toLocaleString() : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {selectedJob && (
        <JobTrendModal
          jobFamilyId={selectedJob.id}
          name={selectedJob.name}
          onClose={() => setSelectedJob(null)}
        />
      )}
    </div>
  );
}
