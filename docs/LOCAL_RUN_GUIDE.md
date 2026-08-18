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

| Category | HTTP Method | Endpoint | Auth Required | Description |
|---|---|---|---|---|
| **Health** | `GET` | `/api/v1/health` | No | Basic health check |
| **Health** | `GET` | `/api/v1/actuator/health` | No | Spring Actuator health |
| **Auth** | `POST` | `/api/v1/auth/register` | No | Register new user |
| **Auth** | `POST` | `/api/v1/auth/login` | No | Login and get JWT |
| **Reviews** | `GET` | `/api/v1/code-reviews` | Yes | List user's code reviews |
| **Reviews** | `POST` | `/api/v1/code-reviews/trigger` | Yes | Trigger a new manual code review |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}` | Yes | Get code review details by ID |
| **Reviews** | `GET` | `/api/v1/code-reviews/{id}/findings` | Yes | Get review findings for code review |
| **GitHub** | `GET` | `/api/v1/github/installations/status` | Yes | Check GitHub App installation status |

---

## 7. Current Limitations (Missing Credentials)

When running locally without configuring API keys in `.env`:
1. **Gemini AI Code Analysis**: If `GEMINI_API_KEY` is not set, triggering automated AI reviews will fail when contacting Gemini API.
2. **GitHub Webhooks & PR Ingestion**: If `GITHUB_APP_ID`, `GITHUB_PRIVATE_KEY`, and `GITHUB_WEBHOOK_SECRET` are not set, automated GitHub PR webhooks cannot fetch PR diffs from GitHub repositories.
3. **Actuator Health Status**: Returns `503 SERVICE_UNAVAILABLE` due to failing external service health indicators (this does not affect local auth or static DB operations).
