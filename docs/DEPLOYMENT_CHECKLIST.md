# AI Code Review Bot — Production Deployment Checklist

This document provides a concise, step-by-step checklist for deploying the **AI Code Review Bot** into local, containerized, or production server environments.

---

## 1. Environment Variable Requirements

Ensure the following environment variables are exported on the host system or provided via a `.env` file prior to starting containers:

| Variable | Description | Required | Example |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring Boot profile | Yes | `prod` / `dev` |
| `PORT` | Backend server port | Yes | `8080` |
| `DB_NAME` | PostgreSQL database name | Yes | `code_review_bot` |
| `DB_USERNAME` | PostgreSQL database user | Yes | `postgres` |
| `DB_PASSWORD` | PostgreSQL database password | Yes | `your_secure_db_password` |
| `JWT_SECRET` | Secret key for signing JWTs (min 32 bytes) | Yes | `your_production_jwt_secret_key...` |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | Yes | `3600000` (1 hour) |
| `GITHUB_APP_ID` | GitHub App Integration ID | Yes | `123456` |
| `GITHUB_PRIVATE_KEY` | GitHub App PEM private key content | Yes | `-----BEGIN RSA PRIVATE KEY-----...` |
| `GITHUB_WEBHOOK_SECRET` | Secret for GitHub Webhook signatures | Optional | `your_webhook_secret` |
| `GEMINI_API_KEY` | Google Gemini AI API key | Yes | `AIzaSy...` |
| `GEMINI_MODEL` | Gemini AI model identifier | Yes | `gemini-3.6-flash` |
| `VITE_API_BASE_URL` | Frontend API base URL | Yes | `http://localhost:8080/api/v1` |

---

## 2. Status Breakdown (Verified vs Remote)

- **Local Docker Compose Stack**: `VERIFIED LOCALLY` (`docker compose config` warning-free, multi-container configuration validated).
- **Frontend SPA Build**: `VERIFIED LOCALLY` (`npx vite build` 100% clean).
- **Backend Test Suite**: `VERIFIED LOCALLY` (258 unit/integration tests passing).
- **Remote Cloud Deployment**: `NOT AVAILABLE / BLOCKED` (No remote cloud server IP or SSH credentials supplied).

---

## 3. Pre-Deployment Readiness Audit

- [x] **Secrets & Key Isolation**: Zero `.env` files, `.pem` keys, or passwords committed to Git repository.
- [x] **Database Migration**: Flyway migrations (`V1__...sql`) present in `backend/src/main/resources/db/migration/`.
- [x] **Docker Image Security**: Backend Docker container runs under a non-root system user (`appuser`).
- [x] **Port Security**: PostgreSQL port `5432` is not exposed publicly on host in production configuration (`docker-compose.prod.yml`).
- [x] **Health Check Probes**: Actuator `/actuator/health` endpoint configured with `/liveness` and `/readiness` probes.

---

## 4. Docker Production Deployment Workflow

### Step 1: Create Production `.env` File
```bash
cp .env.example .env
# Edit .env and supply production secrets
```

### Step 2: Build & Start Containers
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### Step 3: Verify Container Status
```bash
docker compose ps
```

### Step 4: Verify Actuator Health Indicator
```bash
curl http://localhost:8080/api/v1/actuator/health
```

*Expected Response*:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "githubApi": { "status": "UP" },
    "geminiAi": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

---

## 5. Production Security & Infrastructure Rationale

1. **HTTPS / Reverse Proxy**: In a cloud deployment (e.g. AWS EC2, GCP Compute Engine, DigitalOcean), place Nginx or AWS ALB in front of port `5173` (Frontend) and `8080` (Backend) to handle SSL/TLS termination and `X-Forwarded-*` headers.
2. **PostgreSQL Volume Persistence**: Ensure Docker named volume `postgres-data` is mounted to persist database data across container restarts.
3. **MDC Correlation Tracing**: Incoming requests automatically include or generate `X-Correlation-ID` header logs for end-to-end auditability.
