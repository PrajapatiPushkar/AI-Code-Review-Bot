# Docker Compose Deployment and Production Readiness Guide

This guide describes the container architecture, orchestration, security boundaries, and operational procedures for deploying the **AI Code Review Bot** using Docker Compose.

---

## 1. System Topology & Architecture

The application is packaged as a modular monolith composed of three containerized services connected via an internal bridge network (`app-network`):

```
+---------------------------------------------------------------------------------------+
|                                    Host / Internet                                    |
+---------------------------------------------------------------------------------------+
        |                                                              |
        | Port 5173 (Dev) / Port 80 (Prod)                             | Port 8080 (Direct API)
        v                                                              v
+-------------------------------+                            +--------------------------+
|      frontend (Nginx)         |                            |   backend (Spring Boot)  |
| - Serves React / Vite SPA     |--- Reverse Proxy /api/v1 ->| - Java 21 Temurin        |
| - Security Headers & Gzip     |    (internal Docker DNS)   | - Port 8080 (REST / Hooks)|
+-------------------------------+                            | - Non-root user (appuser)|
                                                             +--------------------------+
                                                                          |
                                                                          | Port 5432 (Internal)
                                                                          v
                                                             +--------------------------+
                                                             |   postgres (PostgreSQL)  |
                                                             | - Alpine 16              |
                                                             | - Named volume:          |
                                                             |   postgres-data          |
                                                             | - Port 5432 NOT exposed  |
                                                             |   to host in production  |
                                                             +--------------------------+
```

---

## 2. Prerequisites

- **Docker**: Docker Engine 24.0+ or Docker Desktop with Linux container support.
- **Docker Compose**: Compose v2.20+ (supports Compose specification).
- **Hardware Resources**: Minimum 2 CPU cores, 4 GB RAM, and 10 GB free disk space.
- **Security Assets**:
  - RSA Private Key for the GitHub App (PEM format).
  - Google Gemini API key.
  - Cryptographically secure JWT and Webhook secrets.

---

## 3. Environment Configuration & Secret Management

### 3.1. Create Environment File

Copy the template to your deployment environment:

```bash
cp .env.example .env
chmod 600 .env
```

### 3.2. Secret Generation & Placement

1. **JWT Secret**:
   Generate a 32+ byte cryptographic secret for signing authentication tokens:
   ```bash
   openssl rand -base64 32
   ```
   Assign the generated string to `JWT_SECRET` in `.env`.

2. **Webhook Secret**:
   Generate a random secret for validating GitHub webhook HMAC signatures:
   ```bash
   openssl rand -hex 24
   ```
   Assign this string to `GITHUB_WEBHOOK_SECRET` in `.env` and configure the identical secret in your GitHub App settings.

3. **GitHub App Private Key**:
   Save your GitHub App private key PEM file to:
   ```bash
   mkdir -p secrets
   cp /path/to/your-app-private-key.pem secrets/github-app-private-key.pem
   chmod 600 secrets/github-app-private-key.pem
   ```
   The backend container mounts `./secrets` to `/app/secrets:ro` with read-only permissions.

4. **Production Startup Validator**:
   When `SPRING_PROFILES_ACTIVE=prod`, the backend's `ProductionConfigValidator` automatically blocks startup if:
   - `JWT_SECRET` is blank, shorter than 32 characters, or contains dev default placeholders.
   - `GITHUB_WEBHOOK_SECRET` is blank or contains dev default placeholders.
   - `DB_PASSWORD` or `GEMINI_API_KEY` is blank.
   - `GITHUB_APP_ID` is blank.
   - `GITHUB_PRIVATE_KEY_PATH` does not point to an accessible file.

---

## 4. Development vs. Production Configurations

The deployment separates local development and production configurations cleanly:

| Concern | Local Development (`docker-compose.yml`) | Production (`-f docker-compose.yml -f docker-compose.prod.yml`) |
|---|---|---|
| **Spring Profile** | `dev` (`show-sql: false`, default dev settings) | `prod` (enforces strict startup validation) |
| **Restart Policy** | `unless-stopped` | `always` |
| **PostgreSQL Port** | Bound strictly to loopback: `127.0.0.1:5432:5432` | `!reset []` — completely unexposed to host |
| **Frontend Port** | `5173:80` (or `FRONTEND_PORT`) | `80:80` (or `FRONTEND_PORT`) |
| **Backend Port** | `8080:8080` | `8080:8080` |
| **Health Checks** | Database `pg_isready`, backend `/actuator/health/readiness` | Database `pg_isready`, backend `/actuator/health/readiness` |

---

## 5. Build and Startup Commands

### 5.1. Validate Compose Configuration

Before launching, validate the resolved Compose configuration without starting containers:

```bash
# Validate local development configuration
docker compose config

# Validate production configuration
docker compose -f docker-compose.yml -f docker-compose.prod.yml config
```

### 5.2. Build Images Reproducibly

```bash
# Local development build
docker compose build

# Production build
docker compose -f docker-compose.yml -f docker-compose.prod.yml build
```

### 5.3. Launch Services

```bash
# Start local development stack in background
docker compose up -d

# Start production stack in background
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 5.4. Shutdown Services

```bash
# Graceful stop and remove containers (preserves database data)
docker compose down

# For production
docker compose -f docker-compose.yml -f docker-compose.prod.yml down
```

> [!CAUTION]
> Never run `docker compose down -v` in production. The `-v` flag deletes persistent volumes, destroying all database tables, review histories, and user records.

---

## 6. Service Health, Readiness, and Logs

### 6.1. Inspect Container Status

```bash
docker compose ps
```

Expected output:
- `ai-code-review-db`: `Up (healthy)`
- `ai-code-review-backend`: `Up (healthy)`
- `ai-code-review-frontend`: `Up (healthy)`

### 6.2. Health Check Endpoints

| Service | Target URL | Expected Status | Description |
|---|---|---|---|
| Frontend | `http://localhost:5173/` (or port 80) | `HTTP 200` | Nginx static file server |
| Backend API | `http://localhost:8080/api/v1/health` | `HTTP 200` (`status: UP`) | Application health controller |
| Readiness Probe | `http://localhost:8080/api/v1/actuator/health/readiness` | `HTTP 200` (`status: UP`) | Database & bean initialization readiness |
| Liveness Probe | `http://localhost:8080/api/v1/actuator/health/liveness` | `HTTP 200` (`status: UP`) | JVM runtime liveness |

### 6.3. Monitoring Logs

```bash
# Tail all container logs
docker compose logs -f

# Tail backend service logs only
docker compose logs -f backend

# Tail PostgreSQL logs only
docker compose logs -f postgres
```

---

## 7. Frontend API Routing Considerations

- **The Browser-Network Topology**:
  React/Vite single-page applications execute inside the client's web browser, not inside the frontend container. Consequently, the browser cannot resolve internal Docker container names like `http://backend:8080`.
- **Reverse Proxy Architecture**:
  In `frontend/nginx.conf`, requests to `/api/v1/` are forwarded directly to `http://backend:8080/api/v1/` through Docker's internal DNS network.
- **Single-Origin Deployment**:
  Because the browser communicates with `/api/v1/` on the same host and port as the frontend, cross-origin resource sharing (CORS) complications are eliminated while securing the internal network.

---

## 8. Database Persistence, Migrations, and Operations

### 8.1. Data Persistence

PostgreSQL data is stored in the Docker named volume `postgres-data` mapped to `/var/lib/postgresql/data`.
- Normal container restarts (`docker compose restart`), recreation (`docker compose up --force-recreate`), and updates (`docker compose pull`) do not impact data stored in the volume.

### 8.2. Flyway Schema Migrations

- Flyway migrations are applied automatically during backend container startup (`spring.flyway.enabled: true`).
- Migration files are located in `backend/src/main/resources/db/migration/`.
- In case of migration failure, the backend container will log the exact SQL migration that failed and exit before serving requests:
  ```bash
  docker compose logs backend | grep -i "flyway"
  ```

### 8.3. Safe Database Backup & Recovery

1. **Create Backup**:
   ```bash
   docker compose exec postgres pg_dump -U postgres code_review_bot > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Restore Backup**:
   ```bash
   cat backup_20261014.sql | docker compose exec -T postgres psql -U postgres code_review_bot
   ```

---

## 9. Troubleshooting & Recovery Procedures

### 9.1. Backend Fails to Start (Production Validator Error)

**Symptom**: `ai-code-review-backend` exits immediately on startup with `IllegalStateException`.
**Root Cause**: `SPRING_PROFILES_ACTIVE=prod` is enabled and one or more secrets are either blank or set to dev defaults.
**Resolution**: Check logs with `docker compose logs backend`. Replace default placeholders in `.env` with secure values.

### 9.2. PostgreSQL Connection Refused

**Symptom**: Backend logs report `Connection refused` or `Connection to postgres:5432 refused`.
**Resolution**: Check database health using `docker compose ps postgres`. Verify the health check (`pg_isready`) is passing and that both services reside on `app-network`.

### 9.3. GitHub App Private Key Not Found

**Symptom**: `GITHUB_PRIVATE_KEY_PATH file does not exist: /app/secrets/github-app-private-key.pem`.
**Resolution**: Verify that the host directory `./secrets` contains `github-app-private-key.pem` and that file permissions allow read access.

---

## 10. Automated CI Pipeline Validation

To ensure configuration drift, malformed Compose syntax, or container build regressions are caught early:

- **Continuous Integration Gate**: Every push and pull request targeting `main`, `feature/**`, and `fix/**` runs the `Docker Build & Compose Validation` job defined in [.github/workflows/ci.yml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/.github/workflows/ci.yml).
- **Automated Checks**:
  1. Validates `docker compose -f docker-compose.yml config --quiet` (development configuration).
  2. Validates `docker compose -f docker-compose.yml -f docker-compose.prod.yml config --quiet` (production override).
  3. Verifies that `postgres`, `backend`, and `frontend` services are present in rendered configurations.
  4. Enforces production security by asserting that PostgreSQL host ports are unexposed (`postgres.ports` count == 0).
  5. Compiles both `backend/Dockerfile` and `frontend/Dockerfile` via Docker build to verify buildability without publishing images.
- **Reference**: See [docs/ci/github-actions-ci.md](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/docs/ci/github-actions-ci.md) for full CI pipeline specifications.

---

## 11. Known Limitations & Checks Requiring Manual Verification

1. **Build and Configuration Only in CI**: GitHub Actions validates Compose configuration rendering and Docker image builds; it does NOT publish container images to Docker Hub, GHCR, or cloud registries, nor does it start the full multi-container stack against live databases in CI.
2. **Remote Cloud Deployment**: The Docker Compose workflow has been verified locally and in CI. Remote server provisioning, TLS certificate issuance (e.g. Let's Encrypt), and DNS records require manual operator configuration.
3. **Docker Daemon Execution**: Full multi-container runtime execution (`docker compose up`) requires the Docker daemon to be running on the host system.
4. **External API Quotas & Secrets**: CI executes with safe placeholder defaults. Real GitHub App webhooks, private keys, and Gemini AI tokens depend on external service availability and valid credentials provided by the operator in `.env`.
