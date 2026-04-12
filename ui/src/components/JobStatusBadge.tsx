interface Props {
  status: string | null | undefined;
  isParked?: boolean;
}

const colorMap: Record<string, string> = {
  SUCCEEDED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  RUNNING: 'bg-blue-100 text-blue-800',
  QUEUED: 'bg-yellow-100 text-yellow-800',
  RETRYING: 'bg-orange-100 text-orange-800',
  PARKED: 'bg-gray-100 text-gray-600',
  ACTIVE: 'bg-emerald-100 text-emerald-800',
};

export function JobStatusBadge({ status, isParked }: Props) {
  const label = isParked !== undefined
    ? (isParked ? 'PARKED' : 'ACTIVE')
    : (status ?? '—');
  const color = colorMap[label] ?? 'bg-gray-100 text-gray-700';
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${color}`}>
      {label}
    </span>
  );
}
