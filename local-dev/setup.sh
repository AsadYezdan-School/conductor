#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$SCRIPT_DIR/env.sh"

PG_DRIVER_JAR="$SCRIPT_DIR/.cache/postgresql-42.7.3.jar"
PG_DRIVER_URL="https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"

# ── Wait for PostgreSQL ───────────────────────────────────────────────────────
echo "==> Waiting for PostgreSQL..."
for i in $(seq 1 30); do
    if pg_isready -h localhost -p 5432 -U conductor -d conductor -q 2>/dev/null; then
        echo "    PostgreSQL ready."; break
    fi
    echo "    Attempt $i/30 — sleeping 2s..."; sleep 2
    [ "$i" -eq 30 ] && { echo "ERROR: PostgreSQL not ready." >&2; exit 1; }
done

# ── Wait for ElasticMQ ────────────────────────────────────────────────────────
echo "==> Waiting for ElasticMQ..."
for i in $(seq 1 20); do
    if curl -sf http://localhost:9325 > /dev/null 2>&1; then
        echo "    ElasticMQ ready."; break
    fi
    echo "    Attempt $i/20 — sleeping 2s..."; sleep 2
    [ "$i" -eq 20 ] && { echo "ERROR: ElasticMQ not ready." >&2; exit 1; }
done

# ── Download PostgreSQL JDBC driver for Liquibase ─────────────────────────────
mkdir -p "$SCRIPT_DIR/.cache"
if [ ! -f "$PG_DRIVER_JAR" ]; then
    echo "==> Downloading PostgreSQL JDBC driver..."
    curl -fL "$PG_DRIVER_URL" -o "$PG_DRIVER_JAR"
    echo "    Saved to $PG_DRIVER_JAR"
else
    echo "==> PostgreSQL JDBC driver already cached."
fi

# ── Run Liquibase migrations ──────────────────────────────────────────────────
echo "==> Running Liquibase migrations..."
liquibase \
    --classpath="$PG_DRIVER_JAR" \
    --driver=org.postgresql.Driver \
    --url="$LIQUIBASE_COMMAND_URL" \
    --username="$LIQUIBASE_COMMAND_USERNAME" \
    --password="$LIQUIBASE_COMMAND_PASSWORD" \
    --changelog-file="db.changelog-master.yaml" \
    --search-path="$REPO_ROOT/db-migrations/changelog" \
    update

echo ""
echo "==> Setup complete."
echo "    PostgreSQL: localhost:5432 (db/user/pass = conductor)"
echo "    ElasticMQ:  http://localhost:9324"
echo "    SQS URL:    $SQS_QUEUE_URL"
echo ""
echo "    Start services (each in its own terminal inside nix-shell):"
echo "      local-run-submitter"
echo "      local-run-scheduler"
echo "      local-run-worker"
