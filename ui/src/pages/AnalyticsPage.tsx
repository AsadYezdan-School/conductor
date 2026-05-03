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
import { useAuth } from '../auth/AuthProvider';

function KpiCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border bg-white px-6 py-4">
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-gray-900">{value}</p>
    </div>
  );
}

function formatBucket(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function barColor(rate: number): string {
  if (rate < 80) return '#ef4444';
  if (rate < 95) return '#f59e0b';
  return '#22c55e';
}

export function AnalyticsPage() {
  const { signOut } = useAuth();

  const trendQuery = useQuery({ queryKey: ['analytics', 'run-trend'], queryFn: api.getRunTrend });
  const healthQuery = useQuery({ queryKey: ['analytics', 'job-health'], queryFn: api.getJobHealth });
  const failureQuery = useQuery({ queryKey: ['analytics', 'failure-modes'], queryFn: api.getFailureModes });

  const trend = trendQuery.data ?? [];
  const health = healthQuery.data ?? [];
  const failures = failureQuery.data ?? [];

  // KPI: aggregate across the 24-hour trend buckets
  const totalRuns24h = trend.reduce((s, b) => s + b.totalRuns, 0);
  const succeeded24h = trend.reduce((s, b) => s + b.succeeded, 0);
  const successRate24h =
    totalRuns24h > 0 ? `${((succeeded24h / totalRuns24h) * 100).toFixed(1)}%` : '—';
  const jobsWithFailures = health.filter((j) => j.failed > 0).length;

  // Bar chart: sorted ascending by success rate (worst performers first)
  const healthChartData = [...health]
    .sort((a, b) => (a.successRatePct ?? 0) - (b.successRatePct ?? 0))
    .map((j) => ({ name: j.name, successRate: j.successRatePct ?? 0 }));

  const loading = trendQuery.isLoading || healthQuery.isLoading || failureQuery.isLoading;
  const error = trendQuery.error || healthQuery.error || failureQuery.error;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Conductor</h1>
        <div className="flex items-center gap-4">
          <Link to="/" className="text-sm text-gray-600 hover:text-gray-900">Jobs</Link>
          <span className="text-sm font-medium text-blue-600">Analytics</span>
          <button onClick={() => signOut()} className="text-sm text-gray-500 hover:text-gray-900">
            Sign out
          </button>
        </div>
      </header>

      <main className="px-8 py-6 space-y-6">
        <h2 className="text-lg font-semibold text-gray-900">Analytics</h2>

        {loading && <p className="text-gray-500">Loading…</p>}
        {error && <p className="text-red-600">Failed to load analytics data.</p>}

        {/* KPI row */}
        <div className="grid grid-cols-3 gap-4">
          <KpiCard label="Total Runs (24 h)" value={totalRuns24h} />
          <KpiCard label="Success Rate (24 h)" value={successRate24h} />
          <KpiCard label="Jobs with Failures" value={jobsWithFailures} />
        </div>

        {/* Visualisation 1: Line chart — run volume over last 24 hours */}
        <div className="rounded-lg border bg-white p-6">
          <h3 className="mb-4 text-sm font-semibold text-gray-700">
            Run Volume — Last 24 Hours
          </h3>
          {trend.length === 0 && !loading ? (
            <p className="text-sm text-gray-400">No run data for the last 24 hours.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trend} margin={{ top: 4, right: 24, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="bucket" tickFormatter={formatBucket} tick={{ fontSize: 12 }} />
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

        {/* Visualisation 2: Bar chart — success rate per job */}
        <div className="rounded-lg border bg-white p-6">
          <h3 className="mb-4 text-sm font-semibold text-gray-700">
            Success Rate by Job — Last 7 Days (worst performers first)
          </h3>
          {healthChartData.length === 0 && !loading ? (
            <p className="text-sm text-gray-400">No job data for the last 7 days.</p>
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
                <Bar dataKey="successRate" name="Success rate" radius={[3, 3, 0, 0]}>
                  {healthChartData.map((entry, i) => (
                    <Cell key={i} fill={barColor(entry.successRate)} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Failure modes table */}
        <div className="rounded-lg border bg-white p-6">
          <h3 className="mb-4 text-sm font-semibold text-gray-700">
            Failure Modes — Last 7 Days
          </h3>
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
    </div>
  );
}
