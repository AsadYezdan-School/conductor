#!/usr/bin/env bash
# Start all Conductor services and expose the UI publicly via Tailscale Funnel.
#
# Usage:  bash local-dev/funnel-serve.sh
#         bash local-dev/funnel-serve.sh --stop
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SESSION="conductor"
UI_PORT=5173

# ── Helpers ───────────────────────────────────────────────────────────────────

die() { echo "ERROR: $*" >&2; exit 1; }

require() {
  command -v "$1" &>/dev/null || die "'$1' not found — install it or enter nix-shell first."
}

# ── --stop shortcut ───────────────────────────────────────────────────────────

if [[ "${1:-}" == "--stop" ]]; then
  echo "==> Stopping Tailscale Funnel..."
  tailscale funnel --bg off 2>/dev/null || true
  echo "==> Killing tmux session '$SESSION'..."
  tmux kill-session -t "$SESSION" 2>/dev/null && echo "Done." || echo "No session found."
  exit 0
fi

# ── Pre-flight ────────────────────────────────────────────────────────────────

require docker-compose
require tmux
require tailscale

# ── Infrastructure ────────────────────────────────────────────────────────────

echo "==> Starting postgres + elasticmq..."
docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" up -d

echo "==> Running migrations..."
bash "$REPO_ROOT/local-dev/setup.sh"

# ── tmux session ──────────────────────────────────────────────────────────────

tmux kill-session -t "$SESSION" 2>/dev/null || true

# Each window sources env.sh so env vars are available without nix-shell.
_window() {
  local name="$1"; local delay="${2:-0}"; local cmd="$3"
  local full_cmd="source '$REPO_ROOT/local-dev/env.sh'"
  [[ "$delay" -gt 0 ]] && full_cmd+=" && sleep $delay"
  full_cmd+=" && $cmd"
  if tmux has-session -t "$SESSION" 2>/dev/null; then
    tmux new-window -t "$SESSION" -n "$name"
  else
    tmux new-session -d -s "$SESSION" -n "$name"
  fi
  tmux send-keys -t "$SESSION:$name" "$full_cmd" Enter
}

echo "==> Starting services in tmux session '$SESSION'..."

_window "worker"        0  "cd '$REPO_ROOT/worker' && go run ."
_window "mock-listener" 0  "cd '$REPO_ROOT' && bazelisk run //mock-data-services/mock-listener-service:MockListenerService"
_window "scheduler"     5  "cd '$REPO_ROOT' && bazelisk run //scheduler:Scheduler"
_window "submitter"     5  "cd '$REPO_ROOT' && bazelisk run //submitter:Submitter"
_window "ui"            0  "cd '$REPO_ROOT/ui' && npm run dev"

# ── Tailscale Funnel ──────────────────────────────────────────────────────────

echo "==> Enabling Tailscale Funnel on port $UI_PORT..."
sudo tailscale funnel --bg "$UI_PORT"

PUBLIC_URL="$(tailscale serve status 2>/dev/null | grep -oP 'https://\S+\.ts\.net' | head -1 || true)"

echo ""
echo "  All services started."
echo ""
echo "  Local UI  →  http://localhost:$UI_PORT"
if [[ -n "$PUBLIC_URL" ]]; then
  echo "  Public    →  $PUBLIC_URL"
else
  echo "  Public    →  run 'tailscale serve status' to get your URL"
fi
echo ""
echo "  tmux: attach with  tmux attach -t $SESSION"
echo "        windows:     worker | mock-listener | scheduler | submitter | ui"
echo "        stop all:    bash local-dev/funnel-serve.sh --stop"
echo ""
