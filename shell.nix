{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  name = "conductor-dev";

  packages = with pkgs; [
    # Runtimes
    go               # 1.24 from nixpkgs; no 1.26-specific language features used in worker
    jdk24            # closest to Java 25 in nixpkgs; source code is compatible
    bazelisk         # auto-downloads Bazel 9.0.0 as declared in .bazelversion

    # Infrastructure
    docker-compose   # manages postgres + elasticmq containers
    awscli2          # inspect SQS queue locally via ElasticMQ
    liquibase        # run DB migrations against local postgres
    postgresql_17    # provides pg_isready + psql client
    curl             # used in setup.sh health checks
  ];

  shellHook = ''
    REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

    if [ -f "$REPO_ROOT/local-dev/env.sh" ]; then
      source "$REPO_ROOT/local-dev/env.sh"
    fi

    export JAVA_HOME="${pkgs.jdk24}"
    alias bazel=bazelisk

    local-up() {
      docker-compose -f "$REPO_ROOT/local-dev/docker-compose.yml" up -d
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
        -c "SELECT id, name, status, updated_at FROM http_jobs ORDER BY created_at DESC LIMIT 20;"'
    }

    local-sqs-stats() {
      aws sqs get-queue-attributes \
        --endpoint-url http://localhost:9324 \
        --queue-url "$SQS_QUEUE_URL" \
        --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible
    }

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
    echo "  local-watch-jobs     Live view of http_jobs table"
    echo "  local-sqs-stats      Queue depth"
    echo "  local-logs           docker-compose log tail"
    echo "  ─────────────────────────────────────────────────────"
    echo ""
  '';
}
