#!/usr/bin/env bash
# Bootstrap the full Conductor stack as Docker containers and expose the UI
# publicly via Tailscale Funnel.
#
# Usage:
#   bash conductor_bootstrap.sh          # build images, start stack, open funnel
#   bash conductor_bootstrap.sh --stop   # tear down stack and close funnel
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/local-dev/docker-compose.yml"
UI_PORT=3000

# ── Helpers ───────────────────────────────────────────────────────────────────

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "==> $*"; }

require() {
  command -v "$1" &>/dev/null || die "'$1' not found — install it or enter nix-shell first."
}

# ── --stop shortcut ───────────────────────────────────────────────────────────

if [[ "${1:-}" == "--stop" ]]; then
  info "Closing Tailscale Funnel..."
  sudo tailscale funnel --bg off 2>/dev/null || true
  info "Stopping all containers..."
  docker-compose -f "$COMPOSE_FILE" down
  echo "Done."
  exit 0
fi

# ── Pre-flight checks ─────────────────────────────────────────────────────────

require bazelisk
require docker
require docker-compose
require tailscale

# ── Build images ──────────────────────────────────────────────────────────────

info "Building service images with Bazel..."
cd "$SCRIPT_DIR"

bazelisk run //submitter:submitter_load
bazelisk run //scheduler:scheduler_load
bazelisk run //worker:worker_load
bazelisk run //mock-data-services/mock-listener-service:mock_listener_service_load
bazelisk run //mock-data-services/mock-data-service:mock_data_service_load
bazelisk run //db-migrations:migrations_load

info "Building UI image with Docker..."
docker build -t conductor/ui:local "$SCRIPT_DIR/ui"

# ── Start the stack ───────────────────────────────────────────────────────────

info "Starting full stack via docker-compose..."
docker-compose -f "$COMPOSE_FILE" up -d

# ── Tailscale Funnel ──────────────────────────────────────────────────────────

info "Enabling Tailscale Funnel on port $UI_PORT..."
sudo tailscale funnel --bg "$UI_PORT"

PUBLIC_URL="$(tailscale serve status 2>/dev/null | grep -oP 'https://\S+\.ts\.net' | head -1 || true)"

echo ""
echo "  Stack is up."
echo ""
echo "  Local UI   →  http://localhost:$UI_PORT"
echo "  Local API  →  http://localhost:8080/jobs"
if [[ -n "$PUBLIC_URL" ]]; then
  echo "  Public     →  $PUBLIC_URL"
else
  echo "  Public     →  run 'tailscale serve status' to get your URL"
fi
echo ""
echo "  Logs:  docker-compose -f local-dev/docker-compose.yml logs -f"
echo "  Stop:  bash conductor_bootstrap.sh --stop"
echo ""
