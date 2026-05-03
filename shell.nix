{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  name = "conductor-dev";

  packages = with pkgs; [
    # Runtimes
    go               # 1.24 from nixpkgs; no 1.26-specific language features used in worker
    bazelisk         # auto-downloads Bazel 9.0.0 as declared in .bazelversion

    # Infrastructure
    docker-compose   # manages postgres + elasticmq containers
    liquibase        # run DB migrations against local postgres
    postgresql_17    # provides pg_isready + psql client
    curl             # used in setup.sh health checks
    tmux             # local-start-all multi-service session
    nodejs           # npm run dev for UI
  ];

  shellHook = ''
    REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

    if [ -f "$REPO_ROOT/local-dev/env.sh" ]; then
      source "$REPO_ROOT/local-dev/env.sh"
    fi

    alias bazel=bazelisk

    local-setup() {
      bash "$REPO_ROOT/local-dev/setup.sh"
    }

    local-up() {
      docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" up -d
      local-setup
    }

    local-down() {
      docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" down
    }

    local-down-clean() {
      # Also removes the named volume — destroys all DB data
      docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" down -v
    }

    local-setup() {
      bash "$REPO_ROOT/local-dev/setup.sh"
    }

    local-run-worker() {
      # go run from worker/ because go.mod lives there
      # module path: asadyezdanschool/conductor/worker
      cd "$REPO_ROOT/worker" && go run .
    }

    local-run-scheduler() {
      cd "$REPO_ROOT" && bazelisk run //scheduler:Scheduler
    }

    local-run-submitter() {
      cd "$REPO_ROOT" && bazelisk run //submitter:Submitter
    }

    local-run-mock-listener() {
      cd "$REPO_ROOT" && bazelisk run //mock-data-services/mock-listener-service:MockListenerService
    }

    local-run-mock-data-service() {
      cd "$REPO_ROOT" && bazelisk run //mock-data-services/mock-data-service:MockDataService
    }

    local-run-ui() {
      cd "$REPO_ROOT/ui" && npm run dev
    }

    local-start-all() {
      SESSION="conductor"

      echo "==> Starting infrastructure (postgres + elasticmq)..."
      local-up || { echo "Infrastructure failed, aborting."; return 1; }

      tmux kill-session -t "$SESSION" 2>/dev/null || true

      tmux new-session -d -s "$SESSION" -n "worker" \
        -e "PATH=$PATH" -e "REPO_ROOT=$REPO_ROOT"
      tmux send-keys -t "$SESSION:worker" \
        "source '$REPO_ROOT/local-dev/env.sh' && cd '$REPO_ROOT/worker' && go run ." Enter

      tmux new-window -t "$SESSION" -n "mock-listener" \
        -e "PATH=$PATH" -e "REPO_ROOT=$REPO_ROOT"
      tmux send-keys -t "$SESSION:mock-listener" \
        "source '$REPO_ROOT/local-dev/env.sh' && cd '$REPO_ROOT' && bazelisk run //mock-data-services/mock-listener-service:MockListenerService" Enter

      tmux new-window -t "$SESSION" -n "scheduler" \
        -e "PATH=$PATH" -e "REPO_ROOT=$REPO_ROOT"
      tmux send-keys -t "$SESSION:scheduler" \
        "sleep 5 && source '$REPO_ROOT/local-dev/env.sh' && cd '$REPO_ROOT' && bazelisk run //scheduler:Scheduler" Enter

      tmux new-window -t "$SESSION" -n "submitter" \
        -e "PATH=$PATH" -e "REPO_ROOT=$REPO_ROOT"
      tmux send-keys -t "$SESSION:submitter" \
        "sleep 5 && source '$REPO_ROOT/local-dev/env.sh' && cd '$REPO_ROOT' && bazelisk run //submitter:Submitter" Enter

      tmux new-window -t "$SESSION" -n "ui" \
        -e "PATH=$PATH" -e "REPO_ROOT=$REPO_ROOT"
      tmux send-keys -t "$SESSION:ui" \
        "cd '$REPO_ROOT/ui' && npm run dev" Enter

      tmux select-window -t "$SESSION:worker"
      echo "==> Attaching to tmux session '$SESSION'. Use Ctrl+b n/p to switch windows. Ctrl+b d to detach."
      tmux attach-session -t "$SESSION"
    }

    local-stop-all() {
      tmux kill-session -t "conductor" 2>/dev/null && echo "Stopped all services." || echo "No running session found."
    }

    local-logs() {
      docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" logs -f
    }

    local-psql() {
      psql "postgresql://conductor:conductor@localhost:5432/conductor"
    }

    local-watch-jobs() {
      watch -n 2 'psql "postgresql://conductor:conductor@localhost:5432/conductor" \
        -c "SELECT jr.id AS run_id, jd.name, c.url, c.method, jr.status, jr.attempt_number, jr.scheduled_at, jr.duration_ms \
            FROM job_runs jr \
            JOIN job_definitions jd ON jd.id = jr.job_definition_id \
            JOIN job_type_http_configs c ON c.job_definition_id = jd.id \
            ORDER BY jr.scheduled_at DESC LIMIT 20;"'
    }

    help() {
      echo "Visit http://localhost:9325/ to access ElasticMQ's web UI and inspect queues + messages."
      echo "WARNING: You must have java 25 installed locally, it is not supported in Nix yet, so you need to install it before running submitter and scheduler"
      echo "Available commands:"
          echo ""
          echo "  Conductor local dev shell"
          echo "  ─────────────────────────────────────────────────────"
          echo "  local-start-all             Start ALL services in tmux (one command!)"
          echo "  local-stop-all              Kill the tmux session + all services"
          echo "  ─────────────────────────────────────────────────────"
          echo "  local-up                    Start postgres + elasticmq"
          echo "  local-run-worker            Poll SQS → execute    [go run]"
          echo "  local-run-mock-listener     HTTP listener (port 8081) [bazel run]"
          echo "  local-run-scheduler         Poll DB → SQS         [bazel run]"
          echo "  local-run-submitter         Insert jobs into DB  [bazel run]"
          echo "  local-run-ui                Vite dev server (port 9080) [npm run dev]"
          echo "  local-run-mock-data-service Periodic job submitter    [bazel run]"
          echo "  local-down                  Stop containers (keeps DB data)"
          echo "  local-down-clean            Stop + delete DB volume"
          echo "  local-psql                  Open psql session"
          echo "  local-watch-jobs            Live view of recent job runs"
          echo "  local-sqs-stats             Queue depth"
          echo "  local-logs                  docker-compose log tail"
          echo "  ─────────────────────────────────────────────────────"
          echo ""
    }
    echo "Visit http://localhost:9325/ to access ElasticMQ's web UI and inspect queues + messages."
    echo "WARNING: You must have java 25 installed locally, it is not supported in Nix yet, so you need to install it before running submitter and scheduler"
    echo "Available commands:"
      echo ""
      echo "  Conductor local dev shell"
      echo "  ─────────────────────────────────────────────────────"
      echo "  local-start-all             Start ALL services in tmux (one command!)"
      echo "  local-stop-all              Kill the tmux session + all services"
      echo "  ─────────────────────────────────────────────────────"
      echo "  local-up                    Start postgres + elasticmq"
      echo "  local-setup                 Run migrations (after local-up)"
      echo "  local-run-worker            Poll SQS → execute    [go run]"
      echo "  local-run-mock-listener     HTTP listener (port 8081) [bazel run]"
      echo "  local-run-scheduler         Poll DB → SQS         [bazel run]"
      echo "  local-run-submitter         Insert jobs into DB  [bazel run]"
      echo "  local-run-ui                Vite dev server (port 9080) [npm run dev]"
      echo "  local-run-mock-data-service Periodic job submitter    [bazel run]"
      echo "  local-down                  Stop containers (keeps DB data)"
      echo "  local-down-clean            Stop + delete DB volume"
      echo "  local-psql                  Open psql session"
      echo "  local-watch-jobs            Live view of recent job runs"
      echo "  local-logs                  docker-compose log tail"
      echo "  ─────────────────────────────────────────────────────"
      echo ""


  '';
}
