# Conductor Data Model — ER Diagram

```mermaid
classDiagram
    class job_definitions {
        +uuid id PK
        +uuid job_family_id
        +int version
        +text name
        +text cron
        +job_type job_type
        +bool is_latest
        +bool is_parked
        +bool is_deleted
        +int max_retries
        +timestamptz created_at
        +text created_by
    }

    class job_type_http_configs {
        +uuid id PK
        +uuid job_definition_id FK
        +text url
        +request_type method
        +jsonb payload
        +jsonb headers
        +int timeout_seconds
    }

    class job_runs {
        +uuid id PK
        +uuid job_definition_id FK
        +uuid job_family_id
        +job_status status
        +int attempt_number
        +uuid parent_run_id FK
        +timestamptz scheduled_at
        +timestamptz queued_at
        +timestamptz started_at
        +timestamptz finished_at
        +text sqs_message_id
    }

    class job_run_events {
        +uuid id PK
        +uuid job_run_id FK
        +job_status status
        +text message
        +int http_status_code
        +text response_body
        +timestamptz occurred_at
        +text source
    }

    class job_schedules {
        +uuid id PK
        +uuid job_definition_id FK
        +timestamptz last_evaluated_at
        +timestamptz last_triggered_at
        +timestamptz next_scheduled_at
    }

    class job_family_alert_configs {
        +uuid job_family_id PK
        +numeric min_success_rate_pct
        +int max_avg_duration_ms
        +timestamptz updated_at
    }

    class job_dependencies {
        +uuid id PK
        +uuid upstream_family_id
        +uuid downstream_family_id
        +timestamptz created_at
    }

    job_definitions "1" --> "0..*" job_runs : has
    job_definitions "1" --> "0..1" job_type_http_configs : http config
    job_definitions "1" --> "0..1" job_schedules : schedule
    job_runs "1" --> "0..*" job_run_events : emits
    job_runs "0..1" --> "0..*" job_runs : retried as
    job_definitions "1" ..> "0..1" job_family_alert_configs : alerts
    job_definitions "1" ..> "0..*" job_dependencies : dependency edge
```

## Enum Types

| Enum | Values |
|------|--------|
| `job_type` | `HTTP`, `SHELL`, `PYTHON` |
| `job_status` | `WAITING`, `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `RETRYING`, `CANCELLED`, `PARKED` |
| `request_type` | `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`, `HEAD` |

## Implicit Relationships (no enforced FK)

| From | To | Via | Note |
|------|----|-----|------|
| `job_family_alert_configs` | `job_definitions` | `job_family_id` | No FK — orphan rows are acceptable by design |
| `job_dependencies` | `job_definitions` | `upstream_family_id` / `downstream_family_id` | References job families, not individual versions |

## Design Notes

- **Versioning** — `job_definitions` rows are immutable. Every edit creates a new row with an incremented `version`. All versions share a `job_family_id`; only one row per family has `is_latest = true`.
- **Denormalized `job_family_id` on `job_runs`** — copied from `job_definitions` at insert time to avoid joins in high-frequency analytics queries.
- **Append-only events** — `job_run_events` is never updated; every status transition appends a new row.
- **Retry chains** — `job_runs.parent_run_id` is a self-referential FK pointing to the original run, forming a chain for retry tracking.
- **Soft deletes** — `job_definitions` uses `is_deleted` and `is_parked` flags; rows are never physically removed.
- **Computed duration** — run duration is not stored; it is computed as `EXTRACT(EPOCH FROM (finished_at - started_at)) * 1000` at query time.
