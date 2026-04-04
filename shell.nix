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
          echo "  local-up             Start postgres + elasticmq"
          echo "  local-run-submitter  Insert jobs into DB  [bazel run]"
          echo "  local-run-scheduler  Poll DB → SQS         [bazel run]"
          echo "  local-run-worker     Poll SQS → execute    [go run]"
          echo "  local-down           Stop containers (keeps DB data)"
          echo "  local-down-clean     Stop + delete DB volume"
          echo "  local-psql           Open psql session"
          echo "  local-watch-jobs     Live view of recent job runs"
          echo "  local-sqs-stats      Queue depth"
          echo "  local-logs           docker-compose log tail"
          echo "  ─────────────────────────────────────────────────────"
          echo ""
    }
    echo "Visit http://localhost:9325/ to access ElasticMQ's web UI and inspect queues + messages."
    echo "WARNING: You must have java 25 installed locally, it is not supported in Nix yet, so you need to install it before running submitter and scheduler"
    echo "Available commands:"
      echo ""
      echo "  Conductor local dev shell"
      echo "  ─────────────────────────────────────────────────────"
      echo "  local-up             Start postgres + elasticmq"
      echo "  local-setup          Run migrations (after local-up)"
      echo "  local-run-submitter  Insert jobs into DB  [bazel run]"
      echo "  local-run-scheduler  Poll DB → SQS         [bazel run]"
      echo "  local-run-worker     Poll SQS → execute    [go run]"
      echo "  local-down           Stop containers (keeps DB data)"
      echo "  local-down-clean     Stop + delete DB volume"
      echo "  local-psql           Open psql session"
      echo "  local-watch-jobs     Live view of recent job runs"
      echo "  local-logs           docker-compose log tail"
      echo "  ─────────────────────────────────────────────────────"
      echo ""


  '';
}
