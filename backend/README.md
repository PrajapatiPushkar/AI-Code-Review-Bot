# AI Code Review Bot - Backend Configuration & Docker Guide

This document explains the environment configuration, profile management, production validation, secret security, local execution, Docker deployment, and testing procedures for the AI Code Review Bot backend.

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
                       Named Volume
```

---

## 2. Docker Compose Quickstart

The backend and PostgreSQL database can be launched together using Docker Compose.

### Step 1: Create Environment File
Copy `.env.example` to `.env` in the root workspace directory:
```bash
cp .env.example .env
```

### Step 2: Build & Start Containers
Run Docker Compose in detached mode:
```bash
docker compose up --build -d
```

This starts:
1. `postgres` (PostgreSQL 16 Alpine container with healthcheck on `5432`)
2. `backend` (Spring Boot Java 21 app on `8080`, waiting for PostgreSQL to be healthy, running Flyway migrations automatically)

---

## 3. Useful Docker Commands

```bash
# Build services
docker compose build

# Start services in foreground
docker compose up

# Build and start in background
docker compose up --build -d

# List running containers
docker compose ps

# View backend logs
docker compose logs -f backend

# View PostgreSQL logs
docker compose logs -f postgres

# Stop containers without removing persistent database volume
docker compose down

# Stop containers AND delete persistent database volume (caution: erases data)
docker compose down -v
```

---

## 4. Required Environment Variables

All sensitive settings and environment-specific parameters are configured via environment variables.

| Variable | Description | Required in Prod | Default (Dev Profile) |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`, `prod`, `test`) | Recommended | `dev` |
| `PORT` | HTTP server port | No | `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | Yes | `jdbc:postgresql://postgres:5432/code_review_bot` |
| `DB_HOST` | Database host (use `postgres` for Docker, `localhost` for local) | Yes | `postgres` |
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

## 5. Production Security & Validation

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Startup Validation (`ProductionConfigValidator`):
   When running under `prod` profile, the backend validates all required credentials at startup. If any required production secret is missing or using dev fallback defaults, application startup immediately terminates with explicit validation logging.
3. Secret Protection:
   - Credentials and secrets are never printed in logs or exception tracebacks.
   - Property beans (`GithubProperties`, `GeminiProperties`, `JwtProperties`) mask secrets in `toString()`.
   - `.env` files, `.pem` files, and `.key` files are strictly excluded via `.gitignore` and `.dockerignore`.

---

## 6. Running Unit & Integration Tests

To run the backend test suite:

```bash
mvn test "-Dtest=*Test,!AiCodeReviewBotApplicationTests"
```
