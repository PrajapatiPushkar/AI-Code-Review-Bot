# GitHub Actions CI and Automated Quality Gates

This document details the Continuous Integration (CI) pipeline and automated quality gates configured for the AI-Code-Review-Bot project under `.github/workflows/ci.yml`.

---

## 1. Pipeline Overview

The CI pipeline automatically validates every pull request and branch push targeting `main`, feature branches (`feature/**`), and bugfix branches (`fix/**`). It enforces automated quality gates covering whitespace hygiene, the complete backend test suite, the frontend production bundle build, and Docker Compose deployment and container image build validation.

```
+---------------------------------------------------------------------------------------------------------+
|                                        GitHub Actions CI Pipeline                                       |
+---------------------------------------------------------------------------------------------------------+
        |                                  |                              |                              |
        v                                  v                              v                              v
+-----------------------+      +-----------------------+      +-----------------------+      +-----------------------+
|  Whitespace & Hygiene |      |   Backend Test Suite  |      |     Frontend Build    |      | Docker Build & Compose|
|   (git diff --check)  |      |   (Java 21 / Maven)   |      |    (Node 20 / Vite)   |      | (Compose & Dockerfile)|
+-----------------------+      +-----------------------+      +-----------------------+      +-----------------------+
```

---

## 2. Workflow Triggers and Concurrency

- **Triggers**:
  - `push`: Triggered on commits pushed to `main`, `feature/**`, and `fix/**`.
  - `pull_request`: Triggered on pull requests opened, synchronized, or reopened against `main`, `feature/**`, and `fix/**`.
- **Concurrency Control**:
  - Configured with `concurrency.group: ${{ github.workflow }}-${{ github.ref }}`.
  - In-progress runs for pull requests are cancelled when new commits are pushed (`cancel-in-progress: true`), saving CI runner minutes and preventing outdated check runs.

---

## 3. Workflow Jobs and Runtime Specifications

| Job Key | Display Name | Runner OS | Runtime / Toolchain | Primary Command | Caching |
|---|---|---|---|---|---|
| `whitespace` | `Whitespace & Hygiene Check` | `ubuntu-latest` | Git | `git diff --check` | N/A |
| `backend` | `Backend Test Suite (Java 21)` | `ubuntu-latest` | Eclipse Temurin JDK 21 | `mvn --batch-mode --no-transfer-progress test` | Maven (`backend/pom.xml`) |
| `frontend` | `Frontend Build (Node 20)` | `ubuntu-latest` | Node.js 20 LTS | `npm ci`<br>`npm run build` | npm (`frontend/package-lock.json`) |
| `docker` | `Docker Build & Compose Validation` | `ubuntu-latest` | Docker Engine & Docker Compose | `docker compose config --quiet`<br>`docker build backend`<br>`docker build frontend` | Docker layer caching |

### Job Details

1. **`whitespace` (`Whitespace & Hygiene Check`)**:
   - Inspects the commit diff or PR diff for whitespace errors (such as trailing whitespace or carriage-return irregularities) using `git diff --check`.
   - Ensures code cleanliness before merging without running heavy compilers.

2. **`backend` (`Backend Test Suite (Java 21)`)**:
   - Matches the project Java version (`<java.version>21</java.version>`) defined in [backend/pom.xml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/backend/pom.xml).
   - Utilizes Eclipse Temurin JDK 21 distribution.
   - Caches Maven dependencies via `actions/setup-java`.
   - Executes the complete automated test suite (499+ unit and integration tests) in non-interactive batch mode.

3. **`frontend` (`Frontend Build (Node 20)`)**:
   - Matches the container runtime specification (`node:20-alpine`) defined in [frontend/Dockerfile](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/frontend/Dockerfile).
   - Sets up Node.js 20 with npm package caching based on `frontend/package-lock.json`.
   - Executes reproducible dependency installation via `npm ci`.
   - Builds the client bundle via `npm run build` (Vite) to verify JSX/CSS syntax, imports, and bundling.

4. **`docker` (`Docker Build & Compose Validation`)**:
   - **Compose Validation**: Validates both development (`docker-compose.yml`) and production override (`docker-compose.prod.yml`) Compose configurations via `docker compose config --quiet`. Suppresses resolved configuration output to prevent leaking interpolated secrets or sensitive values into workflow logs.
   - **Service Integrity Verification**: Verifies that the required services (`postgres`, `backend`, `frontend`) are properly parsed and present in both development and production configurations.
   - **Production Security Check**: Verifies that production Compose output leaves the PostgreSQL port completely unexposed to the host (`postgres.ports` length == 0).
   - **Backend Image Build**: Builds the backend image `ai-code-review-backend:ci-validate` from `backend/Dockerfile` using the multi-stage Maven build (Java 21 Temurin). Verifies that dependency resolution, compilation, and JAR packaging succeed without errors.
   - **Frontend Image Build**: Builds the frontend image `ai-code-review-frontend:ci-validate` from `frontend/Dockerfile` passing `--build-arg VITE_API_BASE_URL=/api/v1`. Verifies that Vite compilation and Nginx reverse-proxy configuration succeed without depending on runtime browser resolution of internal container hostnames.
   - **Build Validation Only**: Images are built strictly locally on the runner to validate syntax and compilation; no images are pushed or published to Docker Hub, GHCR, or any registry.

---

## 4. Database and Service Requirements

- **In-Memory Testing**: The automated backend test suite is completely self-contained. All Spring Data JPA, security, and REST controller tests utilize Mockito mocks, WebMvcTest contexts, or Spring Boot test stubs (such as the custom in-memory `StubDataSource` implemented in `ActuatorHealthTest` and `ActuatorSecurityTest`).
- **No Live Database Required in CI**: Because automated tests do not establish socket connections to an external PostgreSQL database, no Docker daemon or PostgreSQL service container is required for the backend unit/integration tests.
- **Excluded Live Integration Tests**:
  - `RealEndToEndCodeReviewVerificationTest` is explicitly marked `@Disabled` as it is designed exclusively for manual local end-to-end verification requiring a live local PostgreSQL instance and a valid GitHub App private key.
  - `RealGithubAppVerificationTest` includes a conditional check (`@EnabledIf("hasKeyFile")`) that skips execution when the local private key file is absent.
- **Docker Engine on Runner**: The `docker` validation job utilizes the Docker Engine pre-installed on GitHub Actions `ubuntu-latest` runners to validate configuration rendering and build application container images.

---

## 5. Security and Secret Handling

- **Least Privilege Permissions**: Top-level permissions are explicitly scoped to read-only (`permissions: contents: read`). No write permissions (`issues: write`, `pull-requests: write`, or `checks: write`) are granted to the workflow.
- **Safe Triggers**: Standard `pull_request` is used instead of privileged `pull_request_target`. This prevents untrusted pull-request forks from accessing secrets or executing malicious code with repository write permissions.
- **Zero Secrets Required**: The CI pipeline does not inject production credentials, GitHub App private keys, Gemini API keys, or JWT secrets. All tests and Compose validations rely either on mock values, template defaults, or placeholder fallbacks safely configured in `docker-compose.yml`.
- **Log Hygiene**: Docker Compose configurations are validated with `--quiet`, and service inspections query structure directly without echoing full resolved environment configurations that could reveal credentials.
- **Build Context Exclusion**: `.dockerignore` files across root, `backend/`, and `frontend/` exclude sensitive files (`.env`, `.env.*`, `*.pem`, `*.key`, `secrets/`) as well as local build artifacts (`target/`, `dist/`, `node_modules/`, `.git/`, `.idea/`, `.vscode/`).
- **Container Hardening**: Backend runtime images preserve the non-root `appuser:appgroup` system user and container health check probes.
- **Action Pinning**: Third-party GitHub actions are pinned to immutable 40-character commit SHAs with version comments to protect against supply-chain compromise:
  - `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683` (`v4.2.2`)
  - `actions/setup-java@c5195efecf7bdfc987ee8bae7a71cb8b11521c00` (`v4.7.1`)
  - `actions/setup-node@1d0ff469b62b6d0ad9b0af0fb3b1e40be53ce9b9` (`v4.2.0`)
- **No Artifact or Registry Leakage**: The pipeline does not upload secrets as workflow artifacts, and never publishes built container images to any public or private registry.

---

## 6. How to Reproduce CI Checks Locally

To verify all gates prior to pushing commits:

### 1. Whitespace Validation
From the repository root:
```bash
git diff --check
```

### 2. Backend Test Suite
From the repository root or `backend/`:
```bash
cd backend
mvn test
```
Expected output:
- Total tests executed: 499+
- Failures: 0
- Errors: 0
- Skipped: 1 (`RealEndToEndCodeReviewVerificationTest`, marked `@Disabled`)

### 3. Frontend Build
From the repository root or `frontend/`:
```bash
cd frontend
npm ci
npm run build
```
Expected output:
- Production bundle compiled in `dist/` without syntax or bundling errors.

### 4. Docker Compose Configuration Validation
From the repository root:
```bash
# Validate development configuration syntax
docker compose -f docker-compose.yml config --quiet

# Validate production configuration syntax and port isolation
docker compose -f docker-compose.yml -f docker-compose.prod.yml config --quiet
```
Expected output:
- Command exits with status code 0 and no syntax or schema warnings.

### 5. Application Container Image Builds
> [!NOTE]
> Local container builds require a running Docker daemon (Docker Desktop or Linux Docker service).

From the repository root:
```bash
# Build backend container image
docker build -t ai-code-review-backend:ci-validate -f backend/Dockerfile backend

# Build frontend container image
docker build --build-arg VITE_API_BASE_URL=/api/v1 -t ai-code-review-frontend:ci-validate -f frontend/Dockerfile frontend
```
Expected output:
- Backend multi-stage build finishes with compiled `app.jar` packaged into `eclipse-temurin:21-jre-alpine`.
- Frontend multi-stage build finishes with Vite production bundle served via `nginx:alpine`.

---

## 7. Configuring Required Status Checks in GitHub

> [!IMPORTANT]
> Workflow files alone cannot enforce merge blocking. A repository administrator must enable branch protection or rulesets in the GitHub repository settings.

### Recommended GitHub Branch Protection Setup

1. In the GitHub repository, navigate to **Settings** > **Branches** (or **Rules** > **Rulesets**).
2. Add or edit a branch protection rule targeting `main` and `feature/v2-platform`.
3. Enable **"Require status checks to pass before merging"**.
4. Enable **"Require branches to be up to date before merging"**.
5. In the status checks search box, select the stable check names emitted by this workflow:
   - `Whitespace & Hygiene Check`
   - `Backend Test Suite (Java 21)`
   - `Frontend Build (Node 20)`
   - `Docker Build & Compose Validation`
6. Save changes.

---

## 8. Known Limitations and External Dependencies

1. **No External Live Services**: The CI pipeline does not interact with external third-party APIs (GitHub REST API, Google Gemini AI API, or hosted PostgreSQL instances). Live verification of these integrations must be conducted in dedicated staging environments or via local integration tests where credentials are provided securely.
2. **Build Validation Only**: The CI workflow validates that Compose files parse correctly and that container images compile cleanly. It does NOT publish container images to Docker Hub, GitHub Container Registry (GHCR), or cloud registries.
3. **No Production Secrets**: CI runs with placeholder defaults. It does not verify production credentials, real GitHub App private keys, Gemini API keys, or production webhook secrets.
4. **No TLS / DNS Provisioning**: The pipeline does not provision public HTTPS/TLS certificates (e.g. Let's Encrypt) or public domain DNS routing.
5. **Frontend Linter**: The existing frontend project does not currently configure ESLint or TypeScript compiler scripts in `package.json`. Therefore, frontend CI focuses on strict module bundling and syntax verification via Vite (`npm run build`).
6. **Repository Settings**: Automated merge enforcement requires configuration by a GitHub repository owner or administrator; it cannot be modified through workflow YAML alone.
