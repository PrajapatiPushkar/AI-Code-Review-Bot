# AI Code Review Bot - Production Deployment & Operations Guide

This document provides complete production deployment guidance, architecture topology, reverse proxy setup, secret security, health monitoring, database backup/restore procedures, and operational commands for the AI Code Review Bot backend.

---

## 1. Production Architecture Topology

```
                  ┌───────────────────────┐
                  │   Internet Clients    │
                  └───────────┬───────────┘
                              │
                              ▼ HTTPS (443)
                  ┌───────────────────────┐
                  │ HTTPS Reverse Proxy   │ (NGINX / Caddy)
                  │   (TLS Termination)   │
                  └───────────┬───────────┘
                              │
                              ▼ HTTP (8080)
                  ┌───────────────────────┐
                  │    Spring Boot API    │
                  │   Backend Container   │
                  └───────────┬───────────┘
                              │
                              ▼ PostgreSQL (5432 Internal Network)
                  ┌───────────────────────┐
                  │ PostgreSQL Container  │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │ Persistent DB Volume  │ (postgres-data)
                  └───────────────────────┘
```

---

## 2. Production Prerequisites & Environment Setup

### Environment Variables (.env)
Create a `.env` file on your production server (or supply environment variables via your cloud deployment provider). 

> [!CAUTION]
> NEVER commit `.env` or real production secrets to version control.

Required Production Environment Variables:

| Environment Variable | Description | Example / Note |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | Must be set to `prod` |
| `PORT` | Backend application port | Default: `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://postgres:5432/code_review_bot` |
| `DB_HOST` | Database hostname inside Docker | `postgres` |
| `DB_PORT` | Database port inside Docker network | `5432` |
| `DB_NAME` | Database name | `code_review_bot` |
| `DB_USERNAME` | Database admin user | `postgres` |
| `DB_PASSWORD` | Database password | **REQUIRED** (Must not be blank) |
| `JWT_SECRET` | Secret key for signing HMAC-SHA256 JWT tokens | **REQUIRED** (Min 32 random chars, do NOT use dev key) |
| `JWT_EXPIRATION_MS` | Token lifespan | `3600000` (1 hour) |
| `GITHUB_APP_ID` | GitHub App ID | **REQUIRED** |
| `GITHUB_PRIVATE_KEY` | Raw RSA Private Key PEM string | **REQUIRED** (or `GITHUB_PRIVATE_KEY_PATH`) |
| `GITHUB_PRIVATE_KEY_PATH` | Path to RSA Private Key file | **REQUIRED** (or `GITHUB_PRIVATE_KEY`) |
| `GITHUB_WEBHOOK_SECRET` | GitHub Webhook secret | Recommended |
| `GEMINI_API_KEY` | Google Gemini AI API key | **REQUIRED** |
| `GEMINI_MODEL` | Gemini AI model identifier | Default: `gemini-3.6-flash` |

---

## 3. Production Deployment Commands

### Launching Production Containers
Use `docker-compose.yml` merged with `docker-compose.prod.yml` override:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

This ensures:
1. `SPRING_PROFILES_ACTIVE=prod` is enforced.
2. PostgreSQL port `5432` is **NOT** exposed to the host machine or public internet (kept private inside `app-network`).
3. Startup validation (`ProductionConfigValidator`) enforces that all required secrets are supplied and secure.
4. Auto-restart policy is set to `always`.

---

## 4. HTTPS Reverse Proxy Configuration (NGINX Example)

Spring Boot runs internally on port `8080`. Public HTTPS traffic should be handled by an HTTPS reverse proxy (such as NGINX or Caddy) providing TLS termination and forwarding HTTP headers.

Example NGINX Site Configuration (`/etc/nginx/sites-available/code-review-bot`):

```nginx
server {
    listen 80;
    server_name review-api.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name review-api.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/review-api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/review-api.yourdomain.com/privkey.pem;

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 5. Operations & Operational Procedures

### View Running Containers
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

### View Live Production Logs
```bash
# View backend application logs
docker compose logs -f backend

# View database logs
docker compose logs -f postgres
```

### Health Verification
Verify backend health status:
```bash
curl http://localhost:8080/api/v1/actuator/health
```
Expected Output:
```json
{"status":"UP"}
```

### Database Backup (`pg_dump`)
Create a timestamped SQL backup of the production database:
```bash
docker exec ai-code-review-db pg_dump -U postgres -d code_review_bot > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Database Restore
Restore database state from a SQL backup file:
```bash
cat backup_file.sql | docker exec -i ai-code-review-db psql -U postgres -d code_review_bot
```

### Graceful Restart
Restart backend without downtime to database:
```bash
docker compose restart backend
```

### Rollback Procedure
To rollback to a previous version/tag:
```bash
git checkout <previous-commit-or-tag>
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### Graceful Shutdown
Stop all running production services while preserving persistent database data:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop
```

---

## 6. Production Observability & Monitoring Guide

Lesson 42 introduces production observability foundations for the backend service without exposing sensitive configuration or requiring external platform agents.

### Available Actuator & Health Endpoints

| Endpoint | Context Path Route | Auth Required | Description |
|---|---|---|---|
| `/actuator/health` | `/api/v1/actuator/health` | Public | Overall service health (`UP`/`DOWN`), including database connectivity, `GeminiAiHealthIndicator`, and `GithubApiHealthIndicator`. In production, sensitive component details are suppressed (`show-details: never`). |
| `/actuator/health/liveness` | `/api/v1/actuator/health/liveness` | Public | Container liveness probe (`UP`). |
| `/actuator/health/readiness` | `/api/v1/actuator/health/readiness` | Public | Container readiness probe verifying backend and database connectivity (`UP`). |
| `/actuator/info` | `/api/v1/actuator/info` | Public | Application metadata (version, build name). Sensitive environment properties are disabled. |
| `/actuator/metrics` | `/api/v1/actuator/metrics` | Public | Application-level Micrometer metrics summary and metric keys. |
| `/actuator/prometheus` | `/api/v1/actuator/prometheus` | Public | Prometheus-formatted metrics scrape endpoint (`micrometer-registry-prometheus`). |
| `/health` | `/api/v1/health` | Public | Legacy simple JSON status endpoint. |

> [!SECURITY]
> Sensitive Actuator endpoints (`/actuator/env`, `/actuator/configprops`, `/actuator/beans`, `/actuator/heapdump`, `/actuator/threaddump`, `/actuator/mappings`, `/actuator/loggers`) are unexposed to ensure configuration properties and secrets are never leaked.

---

### Custom Health Indicators

The application includes production health indicators:
- **`GeminiAiHealthIndicator`**: Verifies Gemini AI engine configuration without exposing the API key in the response payload.
- **`GithubApiHealthIndicator`**: Verifies GitHub App integration credentials without exposing RSA private keys or tokens.

---

### Request Correlation ID (`X-Correlation-ID`)

- Every incoming HTTP request is assigned a Correlation ID via `CorrelationIdFilter`.
- If an incoming `X-Correlation-ID` or `X-Request-ID` header is present and valid, it is preserved; otherwise, a UUID is automatically generated.
- The Correlation ID is returned in the response header `X-Correlation-ID`.
- Log statements printed during HTTP handling include `[correlationId=...]` via SLF4J MDC (`correlationId`).
- For asynchronous code reviews, the triggering HTTP request's Correlation ID is explicitly passed to the worker thread MDC, allowing full request-to-background review traceability.

---

### Micrometer Application & Prometheus Metrics

Custom application metrics are recorded in `CodeReviewMetrics` using low-cardinality tags (avoiding user IDs, JWTs, repository names, PR numbers, or commit SHAs) and exposed via Spring Boot Actuator:

- `code_review.submissions.total` (Counter, tags: `type=new`, `type=duplicate`): Count of code review requests received.
- `code_review.completed.total` (Counter): Count of successfully completed code reviews.
- `code_review.failed.total` (Counter): Count of code review executions that encountered exceptions.
- `code_review.in_progress` (Gauge): Number of active in-progress code reviews currently running.
- `code_review.execution.time` (Timer): Duration (in milliseconds) of asynchronous review execution from start to finish.
- `code_review.ai.execution.time` (Timer): Duration (in milliseconds) of Gemini AI model inference execution.
- `code_review.github.failures` (Counter): Count of GitHub API request or comment posting failures.
- `code_review.findings.total` (Counter): Total number of AI findings generated.
- `code_review.comments.total` (Counter): Total number of inline review comments posted to GitHub PRs.

To scrape metrics in Prometheus format:
```bash
curl http://localhost:8080/api/v1/actuator/prometheus
```

---

### Production Log Traceability & Secret Masking

- All logs adhere to structured production logging practices.
- **Sensitive Masking Rules**: JWT tokens, GitHub Private Keys, Installation Access Tokens, API Keys (`GEMINI_API_KEY`), and Database Passwords are NEVER written to logs or MDC.
- Request headers and raw payload bodies containing credentials are explicitly excluded from logging.

---

### Troubleshooting a Failed Code Review

When a code review fails or completes with unexpected results, troubleshoot using the **Review ID** and **Correlation ID**:

1. **Obtain Correlation ID or Review ID**:
   Check the `X-Correlation-ID` response header returned to the caller, or locate the `reviewId` in the API response or database record.

2. **Search Production Container Logs by Correlation ID**:
   ```bash
   docker compose logs backend | grep "correlationId=abc123-def456"
   ```
   This trace displays every HTTP request entry, JWT validation, GitHub API fetch, Gemini AI review call, and response completion tied to that request.

3. **Search Async Background Execution Logs by Review ID**:
   ```bash
   docker compose logs backend | grep "reviewId=42"
   ```
   This trace exposes the async review lifecycle:
---

## 8. Lesson 44 — Production Observability & Monitoring

Lesson 44 expands backend production observability with comprehensive business metrics, external dependency latency/failure tracking, safe correlation ID propagation across HTTP and async execution threads, Actuator security, and container health verification.

### Observability Architecture

```
HTTP Request (Header: X-Correlation-Id)
                 │
                 ▼
     ┌──────────────────────┐
     │ CorrelationIdFilter  │ ◄── Extracted/Generated UUID in MDC ("correlationId")
     └──────────┬───────────┘     Header returned in Response
                │
                ▼
     ┌──────────────────────┐
     │  Security & Context  │ ◄── Credentials masked from MDC & logs
     └──────────┬───────────┘
                │
                ▼
     ┌──────────────────────┐
     │ CodeReviewController │ ◄── Records: code_review.submission.total
     └──────────┬───────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────┐
│                   AsyncCodeReviewRunner (@Async)                 │
│  - MDC Context: correlationId, reviewId                          │
│  - Records: code_review.in_progress (Gauge)                      │
│  - Records: code_review.duration (Timer)                         │
│  - Records: code_review.completed.total / code_review.failed.total│
└───────────────┬──────────────────────────────────┬───────────────┘
                │                                  │
                ▼                                  ▼
   ┌──────────────────────────┐      ┌──────────────────────────┐
   │        GitHub API        │      │        Gemini AI         │
   │ - github.api.request     │      │ - gemini.api.request     │
   │ - github.api.duration    │      │ - gemini.api.duration    │
   │ - github.api.failure     │      │ - gemini.api.failure     │
   └──────────────────────────┘      └──────────────────────────┘
```

---

### Actuator Endpoints & Access Control

| Endpoint Route | Public / Permitted | Description |
|---|---|---|
| `/api/v1/actuator/health` | Public | Application status (`UP`). Checks database connection and configuration health without executing expensive network calls. |
| `/api/v1/actuator/health/liveness` | Public | Container liveness status (`UP`). Indicates the JVM process is alive. |
| `/api/v1/actuator/health/readiness` | Public | Container readiness status (`UP`). Indicates the backend and PostgreSQL DB are ready to serve traffic. |
| `/api/v1/actuator/info` | Public | Safe application metadata (name, version, profile). Secrets are never exposed. |
| `/api/v1/actuator/metrics` | Public | Micrometer application metrics summary. |
| `/api/v1/actuator/prometheus` | Public | Prometheus scrape target (`micrometer-registry-prometheus`). |

> [!SECURITY]
> Sensitive Actuator endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/mappings`) remain unexposed and return HTTP 404 Not Found.

---

### Code-Review Business Metrics

All business metrics use low-cardinality tags (`type=new`, `type=duplicate`, `status=completed`, `status=failed`) to prevent unbounded memory growth:

- `code_review.submission.total` (Counter): Total code review submissions received.
- `code_review.completed.total` (Counter): Successfully completed code reviews.
- `code_review.failed.total` (Counter): Failed code review executions.
- `code_review.duration` (Timer): Duration (in milliseconds) of asynchronous review execution.
- `code_review.findings.total` (Counter): Total AI review findings generated.
- `code_review.comments.posted.total` (Counter): Total inline review comments posted to GitHub PRs.

---

### External Dependency Latency & Failure Metrics

Low-cardinality tags (`dependency=github`, `dependency=gemini`, `status=success`, `status=failed`):

- `github.api.request` (Counter): Total GitHub API request invocations.
- `github.api.duration` (Timer): GitHub API call response duration in milliseconds.
- `github.api.failure` (Counter): Total GitHub API failures.
- `gemini.api.request` (Counter): Total Gemini AI model inference calls.
- `gemini.api.duration` (Timer): Gemini AI inference execution duration in milliseconds.
- `gemini.api.failure` (Counter): Total Gemini AI inference failures.

---

### Request & Async Correlation ID Tracking

1. **HTTP Filter**: `CorrelationIdFilter` reads `X-Correlation-Id` or `X-Correlation-ID` header. If missing, it generates a UUID, attaches it to the response header, and populates `MDC.put("correlationId", ...)`.
2. **Async Propagation**: `AsyncCodeReviewRunner` receives `correlationId` from the caller and populates the worker thread MDC (`correlationId`, `reviewId`).
3. **MDC Cleanup**: MDC is cleared in a `finally` block in both HTTP filter and async thread worker.

---

### Docker Health Verification

- `docker-compose.yml` configures container health checks using `wget http://localhost:8080/api/v1/actuator/health`.
- Service dependencies ensure `postgres` achieves health status (`pg_isready`) before backend startup.
- Temporary external GitHub or Gemini outages do NOT cause Docker container readiness/liveness health check failures.

---

### Troubleshooting Examples

1. **Tracing a Review Execution in Container Logs**:
   ```bash
   docker compose logs backend | grep "correlationId=7d7c8e3a-1234-4567-89ab-cdef01234567"
   ```
2. **Scrape Metrics in Prometheus Format**:
   ```bash
   curl http://localhost:8080/api/v1/actuator/prometheus
   ```
3. **Check Container Readiness Probe**:
   ```bash
   curl http://localhost:8080/api/v1/actuator/health/readiness
   ```

