# AI Code Review Bot - Local Run & Architecture Guide

This guide explains how to start, interact with, and verify the AI Code Review Bot application locally using Docker Compose.

---

## 1. Quick Start Commands

### How to Start the Application (PostgreSQL + Backend + Frontend)
Run the following command from the project root directory:

```bash
docker compose up -d
```

To build and start all containers in one step:
```bash
docker compose up -d --build
```

### How to Start Individual Services
- **PostgreSQL Database Only**:
  ```bash
  docker compose up -d postgres
  ```
- **Backend Service Only**:
  ```bash
  docker compose up -d backend
  ```
- **Frontend Service Only**:
  ```bash
  docker compose up -d frontend
  ```

---

## 2. Browser Access

- **Frontend URL**: [http://localhost:5173](http://localhost:5173)
- **Login Direct Page**: [http://localhost:5173/login](http://localhost:5173/login)
- **Backend API Base URL**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)

---

## 3. Useful Docker Commands

- **Check Container Status**:
  ```bash
  docker compose ps
  ```
- **View All Container Logs**:
  ```bash
  docker compose logs -f
  ```
- **View Backend Logs**:
  ```bash
  docker compose logs -f backend
  ```
- **View Frontend Logs**:
  ```bash
  docker compose logs -f frontend
  ```
- **Rebuild Frontend Container**:
  ```bash
  docker compose build frontend
  docker compose up -d frontend
  ```

### How to Stop the Application
- **Stop containers (preserve database volume data)**:
  ```bash
  docker compose down
  ```
- **Stop containers and remove volumes (reset database)**:
  ```bash
  docker compose down -v
  ```

---

## 4. How to Check Backend Health

- **Actuator Health Endpoint**:
  ```bash
  curl http://localhost:8080/api/v1/actuator/health
  ```
  *(Note: Returns HTTP 503 if GitHub App / Gemini API credentials are not set in `.env`, because custom health indicators check external service configurations. This is expected in local dev mode when credentials are omitted).*

- **Custom Public Health Endpoint**:
  ```bash
  curl http://localhost:8080/api/v1/health
  ```

---

## 5. Authentication Flow & Creating Users

### Overview
- **User Registration**: Handled on backend at `POST /api/v1/auth/register`. Currently, there is **no Registration UI page** in the frontend (the UI provides a Login Page only).
- **Creating a Local User**: Execute a `POST` request to `/api/v1/auth/register` via `curl` or Postman:

  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"dev@example.com\",\"password\":\"password123\",\"username\":\"devuser\"}"
  ```

- **Logging In via Browser**:
  1. Open [http://localhost:5173/login](http://localhost:5173/login)
  2. Enter **Username or Email**: `devuser` (or `dev@example.com`)
  3. Enter **Password**: `password123`
  4. Click **Sign In**.

- **Token Storage & Authorization**:
  - The backend returns a JWT access token:
    ```json
    {
      "accessToken": "eyJhbGciOiJIUzI1Ni...",
      "tokenType": "Bearer",
      "expiresIn": 3600000
    }
    ```
  - The frontend saves `accessToken` in `localStorage.setItem('token', accessToken)`.
  - Every subsequent API call automatically includes the header:
    `Authorization: Bearer <accessToken>`

---

## 6. Important API Endpoints

### Public Endpoints (No Authentication Required)

| Category | HTTP Method | Endpoint | Description |
|---|---|---|---|
| **Health** | `GET` | `/api/v1/health` | Custom lightweight public health ping |
| **Health** | `GET` | `/api/v1/actuator/health` | Spring Boot Actuator component health status |
| **Health** | `GET` | `/api/v1/actuator/health/liveness` | Container liveness probe |
| **Health** | `GET` | `/api/v1/actuator/health/readiness` | Container readiness probe (verifies DB connectivity) |
| **Auth** | `POST` | `/api/v1/auth/register` | Register new user account |
| **Auth** | `POST` | `/api/v1/auth/login` | Authenticate with credentials and receive Bearer JWT |

### GitHub Webhook Endpoints (Signature-Protected)

| Category | HTTP Method | Endpoint | Security Header | Description |
|---|---|---|---|---|
| **Webhook** | `POST` | `/api/v1/webhooks/github` | `X-Hub-Signature-256` | Ingest and verify signed GitHub PR webhook events |

### Authenticated Endpoints (Requires `Authorization: Bearer <JWT>`)

| Category | HTTP Method | Endpoint | Description |
|---|---|---|---|
| **Reviews** | `POST` | `/api/v1/code-reviews/pull-request` | Submit manual asynchronous review request (`202 Accepted`) |
| **Reviews** | `GET` | `/api/v1/code-reviews` | Get paginated review history |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}` | Get review metadata by ID |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}/status` | Check review execution status (`IN_PROGRESS`, `COMPLETED`, `FAILED`) |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}/result` | Get full review result and summary |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}/findings` | Get paginated findings for a review |
| **Reviews** | `GET` | `/api/v1/code-reviews/repository/{owner}/{repository}` | Get review history for a specific repository |
| **Code Fixes** | `POST` | `/api/v1/code-reviews/findings/{findingId}/fix` | Request AI-generated code fix proposal for a finding |
| **Code Fixes** | `GET` | `/api/v1/code-reviews/findings/{findingId}/fixes` | List all fix proposals generated for a finding |
| **Code Fixes** | `GET` | `/api/v1/code-reviews/fixes/{proposalId}` | Get details of a specific fix proposal |
| **Code Fixes** | `PATCH` | `/api/v1/code-reviews/fixes/{proposalId}/status` | Update proposal status (`APPLIED`, `REJECTED`) |
| **Code Fixes** | `GET` | `/api/v1/code-reviews/fixes/{proposalId}/patch` | Download unified git diff patch file |
| **Code Fixes** | `GET` | `/api/v1/code-reviews/fixes/{proposalId}/proposed-content` | Download full proposed source content |
| **Code Fixes** | `GET` | `/api/v1/code-reviews/fixes/{proposalId}/original-content` | Download original source content |
| **Quality Gates** | `GET` | `/api/v1/code-reviews/{reviewId}/quality-gate` | Evaluate quality gate status for a review |
| **Quality Gates** | `GET` | `/api/v1/repositories/{repositoryId}/review-policy` | Fetch configured review policy for repository |
| **Quality Gates** | `PUT` | `/api/v1/repositories/{repositoryId}/review-policy` | Update review policy rules and thresholds |
| **Quality Gates** | `POST` | `/api/v1/repositories/{repositoryId}/review-policy/reset` | Reset repository policy to system defaults |
| **Webhooks** | `GET` | `/api/v1/webhooks/deliveries` | List paginated webhook delivery audit records |
| **Webhooks** | `GET` | `/api/v1/webhooks/deliveries/summary` | Webhook delivery statistics and reliability KPIs |
| **Webhooks** | `GET` | `/api/v1/webhooks/deliveries/{deliveryId}` | Get audit payload for specific webhook delivery |
| **Webhooks** | `POST` | `/api/v1/webhooks/deliveries/{deliveryId}/retry` | Manually retry a failed webhook delivery |
| **Analytics** | `GET` | `/api/v1/analytics/overview` | High-level review and finding totals |
| **Analytics** | `GET` | `/api/v1/analytics/findings` | Aggregated findings grouped by severity |
| **Analytics** | `GET` | `/api/v1/analytics/trends` | Historical review trend metrics |
| **Analytics** | `GET` | `/api/v1/analytics/repositories` | Per-repository review volume and pass rates |
| **Observability**| `GET` | `/api/v1/observability/metrics` | Processing queue sizes, throughput, and worker status |
| **Observability**| `GET` | `/api/v1/actuator/prometheus` | Prometheus-compatible metrics scrape endpoint |
| **GitHub App** | `GET` | `/api/v1/github/installations/user/{userId}` | List installations belonging to authenticated user |
| **GitHub App** | `GET` | `/api/v1/github/installations/{installationId}/repositories` | List repositories accessible by installation |

---

## 7. Current Limitations (Missing Credentials)

When running locally without configuring API keys in `.env`:
1. **Gemini AI Code Analysis**: If `GEMINI_API_KEY` is not set, triggering automated AI reviews will fail when contacting Gemini API.
2. **GitHub Webhooks & PR Ingestion**: If `GITHUB_APP_ID`, `GITHUB_PRIVATE_KEY`, and `GITHUB_WEBHOOK_SECRET` are not set, automated GitHub PR webhooks cannot fetch PR diffs from GitHub repositories.
3. **Actuator Health Status**: Returns `503 SERVICE_UNAVAILABLE` due to failing external service health indicators (this does not affect local auth or static DB operations).
