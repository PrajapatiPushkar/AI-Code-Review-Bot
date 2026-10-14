# GitHub Actions CI and Automated Quality Gates

This document details the Continuous Integration (CI) pipeline and automated quality gates configured for the AI-Code-Review-Bot project under `.github/workflows/ci.yml`.

---

## 1. Pipeline Overview

The CI pipeline automatically validates every pull request and branch push targeting `main` and feature branches (`feature/**`). It enforces automated quality gates covering whitespace hygiene, the complete backend test suite, and the frontend production build.

```
+-----------------------------------------------------------------------------+
|                          GitHub Actions CI Pipeline                         |
+-----------------------------------------------------------------------------+
        |                                  |                                |
        v                                  v                                v
+-----------------------+      +-----------------------+      +-----------------------+
|  Whitespace & Hygiene |      |   Backend Test Suite  |      |     Frontend Build    |
|   (git diff --check)  |      |   (Java 21 / Maven)   |      |    (Node 20 / Vite)   |
+-----------------------+      +-----------------------+      +-----------------------+
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

---

## 4. Database and Service Requirements

- **In-Memory Testing**: The automated backend test suite is completely self-contained. All Spring Data JPA, security, and REST controller tests utilize Mockito mocks, WebMvcTest contexts, or Spring Boot test stubs (such as the custom in-memory `StubDataSource` implemented in `ActuatorHealthTest` and `ActuatorSecurityTest`).
- **No Live Database Required in CI**: Because automated tests do not establish socket connections to an external PostgreSQL database, no Docker daemon or PostgreSQL service container is required in GitHub Actions.
- **Excluded Live Integration Tests**:
  - `RealEndToEndCodeReviewVerificationTest` is explicitly marked `@Disabled` as it is designed exclusively for manual local end-to-end verification requiring a live local PostgreSQL instance and a valid GitHub App private key.
  - `RealGithubAppVerificationTest` includes a conditional check (`@EnabledIf("hasKeyFile")`) that skips execution when the local private key file is absent.

---

## 5. Security and Secret Handling

- **Least Privilege Permissions**: Top-level permissions are explicitly scoped to read-only (`permissions: contents: read`). No write permissions (`issues: write`, `pull-requests: write`, or `checks: write`) are granted to the workflow.
- **Safe Triggers**: Standard `pull_request` is used instead of privileged `pull_request_target`. This prevents untrusted pull-request forks from accessing secrets or executing malicious code with repository write permissions.
- **Zero Secrets Required**: The CI pipeline does not inject production credentials, GitHub App private keys, Gemini API keys, or JWT secrets. All tests rely either on mock values or default development/test configurations.
- **Action Pinning**: Third-party GitHub actions are pinned to immutable 40-character commit SHAs with version comments to protect against supply-chain compromise:
  - `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683` (`v4.2.2`)
  - `actions/setup-java@c5195efecf7bdfc987ee8bae7a71cb8b11521c00` (`v4.7.1`)
  - `actions/setup-node@1d0ff469b62b6d0ad9b0af0fb3b1e40be53ce9b9` (`v4.2.0`)
- **No Artifact Leakage**: The pipeline does not upload secrets, build environment variables, or sensitive configuration files as workflow artifacts.

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
6. Save changes.

---

## 8. Known Limitations and External Dependencies

1. **No External Live Services**: The CI pipeline does not interact with external third-party APIs (GitHub REST API, Google Gemini AI API, or hosted PostgreSQL instances). Live verification of these integrations must be conducted in dedicated staging environments or via local integration tests where credentials are provided securely.
2. **Frontend Linter**: The existing frontend project does not currently configure ESLint or TypeScript compiler scripts in `package.json`. Therefore, frontend CI focuses on strict module bundling and syntax verification via Vite (`npm run build`).
3. **Repository Settings**: Automated merge enforcement requires configuration by a GitHub repository owner or administrator; it cannot be modified through workflow YAML alone.
