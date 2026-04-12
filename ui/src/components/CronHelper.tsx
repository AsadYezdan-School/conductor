import cronstrue from 'cronstrue';

interface Props {
  cron: string;
}

export function CronHelper({ cron }: Props) {
  if (!cron) return null;
  try {
    const human = cronstrue.toString(cron, { throwExceptionOnParseError: true });
    return <span className="text-xs text-gray-500 ml-1">({human})</span>;
  } catch {
    return <span className="text-xs text-red-400 ml-1">(invalid cron)</span>;
  }
}
