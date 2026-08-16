# AI Code Review Bot - Backend Configuration & Environment Guide

This document explains the environment configuration, profile management, production validation, secret security, local execution, and testing procedures for the AI Code Review Bot backend.

---

## 1. Required Environment Variables

All sensitive settings and environment-specific parameters are configured via environment variables.

| Variable | Description | Required in Prod | Default (Dev Profile) |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev`, `prod`, `test`) | Recommended | `dev` |
| `PORT` | HTTP server port | No | `8080` |
| `SPRING_DATASOURCE_URL` / `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL JDBC connection URL or parameters | Yes | `jdbc:postgresql://localhost:5432/code_review_bot` |
| `DB_USERNAME` / `SPRING_DATASOURCE_USERNAME` | Database username | Yes | `postgres` |
| `DB_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | Database password | **Yes** | `postgres` |
| `JWT_SECRET` | Secret key used for signing JWT tokens (min 32 chars) | **Yes** | Dev fallback secret |
| `JWT_EXPIRATION_MS` | JWT expiration time in milliseconds | No | `3600000` (1 hour) |
| `GITHUB_APP_ID` | GitHub App ID | **Yes** | Empty |
| `GITHUB_PRIVATE_KEY` | Raw RSA Private Key PEM string | **Yes** (or path) | Empty |
| `GITHUB_PRIVATE_KEY_PATH` | File path or `classpath:` resource for RSA Private Key | **Yes** (or string) | Empty |
| `GITHUB_WEBHOOK_SECRET` | GitHub Webhook secret | Recommended | Empty |
| `GEMINI_API_KEY` | Google Gemini AI API key | **Yes** | Empty |
| `GEMINI_MODEL` | Gemini AI model identifier | No | `gemini-2.5-flash` |

---

## 2. Local Development Setup

To run the backend locally using default development settings:

1. Copy `.env.example` to `.env` in the workspace root or set environment variables in your environment.
2. Ensure PostgreSQL is running on `localhost:5432` with database `code_review_bot`.
3. Launch the Spring Boot application using Maven:
   ```bash
   mvn spring-boot:run
   ```
   Or explicitly pass active profile and overrides:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--gemini.api-key=YOUR_KEY"
   ```

---

## 3. Production Deployment & Security

For production deployment:

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Configure all required environment variables (`SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `GITHUB_APP_ID`, `GITHUB_PRIVATE_KEY` or `GITHUB_PRIVATE_KEY_PATH`, `GEMINI_API_KEY`).
3. Startup Validation (`ProductionConfigValidator`):
   When running under `prod` profile, the backend validates all required credentials at startup. If any required production secret is missing or using dev fallback defaults, application startup immediately terminates with explicit validation logging.
4. Secret Protection:
   - Credentials and secrets are never printed in logs or exception tracebacks.
   - Property beans (`GithubProperties`, `GeminiProperties`, `JwtProperties`) mask secrets in `toString()`.
   - `.env` files, `.pem` files, and `.key` files are strictly excluded via `.gitignore`.

---

## 4. Running Tests

To run the backend unit test suite:

```bash
mvn test "-Dtest=*Test,!AiCodeReviewBotApplicationTests"
```

All unit and integration tests run independently of real external credentials.
