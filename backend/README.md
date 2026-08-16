# AI Code Review Bot - Backend Configuration & Docker Integration Guide

This document provides complete operational guidance for environment configuration, profile management, Docker Compose local deployment, container lifecycle commands, health verification, database inspection, and testing procedures.

---

## 1. Architecture Overview

```
                    ┌─────────────────┐
                    │     Client      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Spring Boot API │
                    │    Container    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    │    Container    │
                    └────────┬────────┘
                             │
                             ▼
                       Named Volume (postgres-data)
```

---

## 2. Docker Setup & Lifecycle Management

### Setup Environment
1. Copy `.env.example` to `.env` in the workspace root:
   ```bash
   cp .env.example .env
   ```
2. Configure any custom secrets in `.env` if needed. (Do NOT commit `.env` to Git).

### Docker Compose Commands

#### 1. Build Containers
Build or rebuild the Spring Boot backend container image:
```bash
docker compose build
```

#### 2. Start Containers
Start both PostgreSQL and Spring Boot backend containers in detached background mode:
```bash
docker compose up -d
```

To build and start simultaneously:
```bash
docker compose up --build -d
```

#### 3. View Running Containers
Check container status and health:
```bash
docker compose ps
```

#### 4. View Logs
View combined logs:
```bash
docker compose logs -f
```

View backend logs specifically:
```bash
docker compose logs -f backend
```

View PostgreSQL database logs specifically:
```bash
docker compose logs -f postgres
```

#### 5. Health Check Verification
Verify backend application health status via HTTP Actuator endpoint:
```bash
curl http://localhost:8080/api/v1/actuator/health
```
Expected Output:
```json
{"status":"UP"}
```

#### 6. Database Connection Verification
Inspect Flyway database tables directly inside the PostgreSQL container:
```bash
docker exec -it ai-code-review-db psql -U postgres -d code_review_bot -c "\dt"
```

#### 7. Restart Containers
Restart all services without rebuilding:
```bash
docker compose restart
```

Restart backend service specifically:
```bash
docker compose restart backend
```

#### 8. Stop Containers (Preserve Persistent Volume)
Stop running containers while preserving all PostgreSQL database data in `postgres-data` named volume:
```bash
docker compose stop
```
or
```bash
docker compose down
```

#### 9. Complete Reset & Cleanup (Deletes Database Volume)
Stop containers AND remove persistent database volume (resets database to clean state):
```bash
docker compose down -v
```

---

## 3. Required Environment Variables

| Variable | Description | Required in Prod | Default (Dev Profile) |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`, `prod`, `test`) | Recommended | `dev` |
| `PORT` | HTTP server port | No | `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | Yes | `jdbc:postgresql://postgres:5432/code_review_bot` |
| `DB_HOST` | Database host (`postgres` for Docker, `localhost` for local) | Yes | `postgres` |
| `DB_PORT` | Database port | No | `5432` |
| `DB_NAME` | Database name | Yes | `code_review_bot` |
| `DB_USERNAME` | Database username | Yes | `postgres` |
| `DB_PASSWORD` | Database password | **Yes** | `postgres` |
| `JWT_SECRET` | Secret key used for signing JWT tokens (min 32 chars) | **Yes** | Dev fallback secret |
| `JWT_EXPIRATION_MS` | JWT expiration time in milliseconds | No | `3600000` (1 hour) |
| `GITHUB_APP_ID` | GitHub App ID | **Yes** | Empty |
| `GITHUB_PRIVATE_KEY` | Raw RSA Private Key PEM string | **Yes** (or path) | Empty |
| `GITHUB_PRIVATE_KEY_PATH` | File path or `classpath:` resource for RSA Private Key | **Yes** (or string) | Empty |
| `GITHUB_WEBHOOK_SECRET` | GitHub Webhook secret | Recommended | Empty |
| `GEMINI_API_KEY` | Google Gemini AI API key | **Yes** | Empty |
| `GEMINI_MODEL` | Gemini AI model identifier | No | `gemini-2.5-flash` |

---

## 4. Production Security & Validation

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Startup Validation (`ProductionConfigValidator`):
   When running under `prod` profile, the backend validates all required credentials at startup. If any required production secret is missing or using dev fallback defaults, application startup immediately terminates with explicit validation logging.
3. Non-Root Security:
   `Dockerfile` runs the Spring Boot application under non-root system user `appuser:appgroup`.
4. Secret Protection:
   - Credentials and secrets are never printed in logs or exception tracebacks.
   - Property beans (`GithubProperties`, `GeminiProperties`, `JwtProperties`) mask secrets in `toString()`.
   - `.env` files, `.pem` files, and `.key` files are strictly excluded via `.gitignore` and `.dockerignore`.

---

## 5. Running Unit & Integration Tests

To run the full backend unit and configuration test suite:

```bash
mvn test "-Dtest=*Test,!AiCodeReviewBotApplicationTests"
```
