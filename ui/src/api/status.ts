/** Statuses that mean a run will never change again. */
const TERMINAL = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED']);

export function isTerminal(status: string | null | undefined): boolean {
  return TERMINAL.has(status ?? '');
}
