# Engineering Observability, Metrics & Workflow Tracing Architecture

This document describes the architectural design, metrics definitions, telemetry endpoints, correlation ID tracing, security controls, Prometheus scraping configuration, and frontend monitoring dashboard for **Engineering Observability** in the AI Code Review Bot (Commit 21, V2 Platform).

---

## 1. Overview & Objectives

As the AI Code Review Bot evolves across asynchronous execution, quality gates, and automated GitHub webhooks, production operability requires deep real-time visibility into application health, review pipeline performance, webhook delivery reliability, external service resilience, and end-to-end request tracing.

Commit 21 delivers:
1. **Application Health & Probes**: Standardized Spring Boot Actuator liveness, readiness, and PostgreSQL connectivity probes.
2. **Review Workflow Instrumentation**: Micrometer counters, timers, and gauges tracking review submissions, completions, failures, execution duration, findings by source, and quality gate outcomes.
3. **Webhook Reliability Telemetry**: Operational metrics capturing received deliveries, HMAC verification outcomes, duplicate detection, retry executions, and self-healing stale recovery.
4. **External Dependency Telemetry**: Latency tracking, failure recording, HTTP 429 rate-limit counters, and circuit breaker status for Gemini AI and GitHub API integrations.
5. **Correlation ID & MDC Tracing**: Deterministic request tracing using `X-Correlation-ID`, propagating across HTTP requests, webhook handlers, and asynchronous review worker threads with strict cleanup.
6. **Observability Operations API & Dashboard**: Authenticated REST endpoint returning structured operational telemetry and an interactive React dashboard with dark/light theme support.

---

## 2. Telemetry Endpoints & Health Probes

### Endpoints Summary

| Endpoint | Method | Format | Security Policy | Description |
| :--- | :--- | :--- | :--- | :--- |
| `/actuator/health` | `GET` | JSON | Public | High-level status (`UP` / `DOWN`). |
| `/actuator/health/liveness` | `GET` | JSON | Public | Kubernetes liveness probe (internal process health). |
| `/actuator/health/readiness` | `GET` | JSON | Public | Kubernetes readiness probe (database and component readiness). |
| `/actuator/prometheus` | `GET` | Prometheus Text | Authenticated (`USER`, `ADMIN`, `DEVELOPER`) | Prometheus scrape endpoint with all Micrometer metrics. |
| `/actuator/metrics` | `GET` | JSON | Authenticated (`USER`, `ADMIN`, `DEVELOPER`) | List of registered Micrometer metric names. |
| `/actuator/metrics/{metricName}` | `GET` | JSON | Authenticated (`USER`, `ADMIN`, `DEVELOPER`) | Detailed dimensions and sample values for a specific metric. |
| `/api/v1/observability/metrics` | `GET` | JSON | Authenticated (`USER`, `ADMIN`, `DEVELOPER`) | Consolidated, bounded DTO of runtime operational telemetry for dashboard. |

### Health Probes Architecture

```mermaid
graph TD
    Client[HTTP Client / K8s Probes] --> Actuator[/actuator/health]
    Actuator --> Liveness[/actuator/health/liveness]
    Actuator --> Readiness[/actuator/health/readiness]
    Readiness --> DBHealthCheck[Database Connection Health]
    
    Liveness -.->|Reflects process state| AppProcess[Spring Boot Runtime]
    Readiness -.->|Reflects DB connection| PostgreSQL[(PostgreSQL Database)]
```

- **Liveness Probe**: Does not depend on external third-party services (GitHub/Gemini) so temporary network blips never cause orchestrators to terminate the container.
- **Readiness Probe**: Validates database connectivity before routing traffic to the instance.
- **Actuator Details**: `management.endpoint.health.show-details: when_authorized` prevents leaking internal environment configuration to unauthenticated callers.

---

## 3. Micrometer Metrics Registry & Definitions

All metrics are recorded using **bounded labels** (`status`, `source`, `outcome`, `result`, `dependency`). High-cardinality identifiers (such as repository names, PR numbers, commit SHAs, delivery IDs, or user IDs) are strictly excluded from metric tags to prevent metric registry memory leaks.

### 3.1 Code Review Workflow Metrics

| Metric Name | Type | Labels / Tags | Description |
| :--- | :--- | :--- | :--- |
| `codereview.reviews.submitted` | Counter | None | Total code review workflows initiated. |
| `codereview.reviews.completed` | Counter | None | Reviews successfully completed. |
| `codereview.reviews.failed` | Counter | None | Reviews terminating in failure. |
| `codereview.reviews.in_progress` | Gauge | None | Number of reviews actively processing in the async worker pool. |
| `codereview.reviews.duration` | Timer | None | End-to-end execution duration of code review workflows (in seconds). |
| `codereview.reviews.outcome` | Counter | `status={COMPLETED, FAILED}` | Bounded terminal review lifecycle outcomes. |
| `codereview.findings.by_source` | Counter | `source={ai, deterministic_rules}` | Number of review findings generated by detection source. |
| `codereview.quality_gate.outcome` | Counter | `outcome={PASS, FAIL, NOT_EVALUATED}` | Evaluation result distribution across repository quality gates. |

### 3.2 Webhook Reliability Metrics

| Metric Name | Type | Labels / Tags | Description |
| :--- | :--- | :--- | :--- |
| `webhook.deliveries.received` | Counter | None | Total webhook deliveries ingested by the webhook controller. |
| `webhook.signatures.valid` | Counter | None | Webhook deliveries with verified HMAC-SHA256 signatures. |
| `webhook.signatures.invalid` | Counter | None | Deliveries rejected due to signature mismatch or missing signature. |
| `webhook.deliveries.duplicate` | Counter | None | Inbound deliveries matching an already processed delivery GUID. |
| `webhook.deliveries.status` | Counter | `status={ACCEPTED, COMPLETED, FAILED, IGNORED}` | Final lifecycle status assigned to webhook deliveries. |
| `webhook.processing.duration` | Timer | None | Duration of webhook ingestion and synchronous dispatch. |
| `webhook.retries.attempted` | Counter | None | Retry attempts executed across scheduled or manual retries. |
| `webhook.retries.exhausted` | Counter | None | Deliveries reaching terminal maximum retry limits. |
| `webhook.recoveries.stale` | Counter | None | Stale `PROCESSING` deliveries recovered by the lease-stealing worker. |
| `webhook.retries.scheduled` | Counter | None | Failed deliveries picked up and retried by the background scheduler. |

### 3.3 External Dependency Resilience Metrics

| Metric Name | Type | Labels / Tags | Description |
| :--- | :--- | :--- | :--- |
| `external.service.ratelimit` | Counter | `dependency={github, gemini}` | Count of HTTP 429 Too Many Requests received from external APIs. |
| `external.call.duration` | Timer | `service={gemini, github}`, `operation=*` | Latency and call count for external client invocations. |
| `circuitbreaker.calls` | Counter | `name={gemini, github}`, `kind={successful, failed, not_permitted}` | Resilience4j circuit breaker call counts. |
| `circuitbreaker.state` | Gauge | `name={gemini, github}`, `state={closed, open, half_open}` | Resilience4j circuit breaker state indicator. |

---

## 4. Correlation IDs and Structured MDC Logging

### Architecture & Propagation Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client as User / GitHub Webhook
    participant Filter as CorrelationIdFilter
    participant Controller as REST / Webhook Controller
    participant Async as AsyncCodeReviewRunner (Worker Thread)
    
    Client->>Filter: HTTP Request (Optional X-Correlation-ID)
    Note over Filter: Validate format (UUID/Safe String) or generate new UUID
    Filter->>Filter: MDC.put("correlationId", id)
    Filter->>Controller: Process Request
    Controller->>Async: executeReviewAsync(reviewId, correlationId)
    Note over Async: async task starts in ForkJoin/ThreadPool
    Async->>Async: MDC.put("correlationId", correlationId)
    Async->>Async: Execute pipeline & emit structured logs
    Async-->>Async: finally { MDC.remove("correlationId"); }
    Controller-->>Filter: Response Complete
    Filter-->>Client: HTTP Response (Header: X-Correlation-ID)
    Note over Filter: finally { MDC.clear(); }
```

### Key Safety Guarantees

1. **Validation of Inbound Headers**: Arbitrary inbound `X-Correlation-ID` values are sanitized to alphanumeric characters, dashes, and underscores (max 64 chars). Untrusted or malformed values are replaced with a newly generated UUIDv4.
2. **Context Propagation into Async Pool**: When reviews are submitted asynchronously, the caller's correlation ID is passed as a parameter into `AsyncCodeReviewRunner.executeReviewAsync(..., correlationId)`.
3. **Context Cleanup (`MDC.clear()`)**: Every asynchronous task and HTTP filter executes within a `try ... finally` block ensuring MDC entries are purged before threads return to the thread pool. This prevents context leakage across unrelated requests.
4. **No Sensitive Data**: Correlation IDs and logs strictly omit API tokens, HMAC signatures, raw webhook payloads, and repository secrets.

---

## 5. Observability Summary API (`/api/v1/observability/metrics`)

### Response Schema

```json
{
  "timestamp": "2026-10-12T13:20:00Z",
  "environment": "development",
  "health": {
    "status": "UP",
    "liveness": "UP",
    "readiness": "UP",
    "database": "UP"
  },
  "reviewMetrics": {
    "submitted": 42,
    "completed": 38,
    "failed": 4,
    "inProgress": 1,
    "averageDurationMs": 1420.5,
    "outcomes": {
      "COMPLETED": 38,
      "FAILED": 4
    },
    "findingsBySource": {
      "ai": 85,
      "deterministic_rules": 47
    },
    "qualityGates": {
      "PASS": 32,
      "FAIL": 6,
      "NOT_EVALUATED": 4
    }
  },
  "webhookMetrics": {
    "received": 56,
    "validSignatures": 54,
    "invalidSignatures": 2,
    "duplicates": 3,
    "statusCounts": {
      "ACCEPTED": 12,
      "COMPLETED": 36,
      "FAILED": 3,
      "IGNORED": 5
    },
    "retryAttempts": 8,
    "exhaustedRetries": 1,
    "staleRecoveries": 0,
    "pendingRetriesScheduled": 2
  },
  "externalServices": {
    "gemini": {
      "calls": 42,
      "failures": 1,
      "rateLimits": 0,
      "averageLatencyMs": 920.4,
      "circuitBreakerState": "CLOSED"
    },
    "github": {
      "calls": 110,
      "failures": 0,
      "rateLimits": 0,
      "averageLatencyMs": 185.0,
      "circuitBreakerState": "CLOSED"
    }
  }
}
```

---

## 6. Prometheus & Grafana Configuration

### Prometheus Scrape Configuration (`prometheus.yml`)

To configure an external Prometheus server to scrape the application:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-code-review-bot'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['localhost:8080'] # or bot-backend:8080 in docker network
```

### Useful Prometheus PromQL Queries

- **Review Success Rate**:
  ```promql
  sum(rate(codereview_reviews_completed_total[5m])) / 
  (sum(rate(codereview_reviews_completed_total[5m])) + sum(rate(codereview_reviews_failed_total[5m]))) * 100
  ```

- **Average Review Execution Duration**:
  ```promql
  rate(codereview_reviews_duration_seconds_sum[5m]) / 
  rate(codereview_reviews_duration_seconds_count[5m])
  ```

- **Webhook Signature Failure Rate**:
  ```promql
  sum(rate(webhook_signatures_invalid_total[5m])) / 
  (sum(rate(webhook_signatures_valid_total[5m])) + sum(rate(webhook_signatures_invalid_total[5m]))) * 100
  ```

- **External API Rate Limit Warnings**:
  ```promql
  sum by (dependency) (increase(external_service_ratelimit_total[1h]))
  ```

---

## 7. Frontend Observability Dashboard

The React frontend includes an **Observability Dashboard** accessible at `/observability`:

1. **System Health Banner**: Visual status badges for overall health, liveness, readiness, and PostgreSQL connectivity.
2. **Review Pipeline Diagnostics**:
   - Completed vs failed reviews with live success percentage.
   - Active async processing gauge.
   - Findings by source: AI engine vs deterministic rules with proportional visualization.
   - Quality gate outcome distribution (`PASS`, `FAIL`, `NOT_EVALUATED`).
3. **Webhook Reliability Diagnostics**:
   - Received deliveries, valid vs invalid HMAC verification counts, duplicate deliveries detected.
   - Terminal delivery status distribution (`ACCEPTED`, `COMPLETED`, `FAILED`, `IGNORED`).
   - Retries executed, terminal exhausted retries, and background scheduler recoveries.
4. **Dependency Resilience**:
   - Gemini AI: API invocations, latency, failure rate, rate limits, and circuit breaker status.
   - GitHub App API: API invocations, latency, failure rate, rate limits, and circuit breaker status.
5. **Real-time UX Controls**:
   - Auto-refresh toggle (10-second polling interval).
   - Manual refresh with animated loading spinner.
   - Full dark and light theme adaptability following the V2 design system tokens.
   - Explicit disclaimer that values reflect live in-memory Micrometer snapshots (no fabricated historical time series).

---

## 8. Troubleshooting & Known Limitations

1. **In-Memory Snapshot Scope**:
   - Micrometer counters and timers in `ObservabilitySummaryDto` track metrics accumulated during the current JVM lifetime.
   - For long-term historical analysis spanning container restarts, Prometheus and Grafana or an OpenTelemetry collector should be deployed.
2. **PostgreSQL Connectivity in Health Probes**:
   - If PostgreSQL becomes temporarily unreachable, `/actuator/health` and readiness probes will report `DOWN`, preventing new traffic routing while keeping the JVM process running.
3. **External Rate Limits (HTTP 429)**:
   - When Gemini or GitHub triggers HTTP 429 rate limiting, `ResilienceExecutor` increments `external.service.ratelimit` before applying exponential backoff and circuit breaker protection.
