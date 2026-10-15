# Release Readiness and Versioned Docker Image Preparation Guide

This document describes the release readiness process, manual verification workflow, and Docker image versioning conventions for the **AI Code Review Bot** platform.

---

## 1. Overview & Purpose

The release-readiness process provides an isolated, reliable, and secure quality gate to evaluate candidate code before any official version tag, release manifest, or container deployment is finalized.

```
+---------------------------------------------------------------------------------------------------------+
|                                    Manual Release-Readiness Pipeline                                    |
|                                (.github/workflows/release-readiness.yml)                                |
+---------------------------------------------------------------------------------------------------------+
                                                     |
                                                     v
                                  +-------------------------------------+
                                  |       validate-inputs               |
                                  | - Validates SemVer input            |
                                  | - Prevents v1.0.0 tag collision     |
                                  | - Computes short Git SHA & tags     |
                                  +-------------------------------------+
                                                     |
                         +---------------------------+---------------------------+
                         |                                                       |
                         v                                                       v
        +---------------------------------+                     +---------------------------------+
        |             backend             |                     |             frontend            |
        | - Java 21 Temurin / Maven       |                     | - Node 20 LTS / npm ci          |
        | - Full test suite (499+ tests)  |                     | - Vite production bundle build  |
        +---------------------------------+                     +---------------------------------+
                         |                                                       |
                         +---------------------------+---------------------------+
                                                     |
                                                     v
                                  +-------------------------------------+
                                  |         docker-verification         |
                                  | - Docker Compose validation         |
                                  | - PostgreSQL port isolation check   |
                                  | - Multi-stage backend build (SHA/v) |
                                  | - Multi-stage frontend build (SHA/v)|
                                  +-------------------------------------+
                                                     |
                                                     v
                                  +-------------------------------------+
                                  |               summary               |
                                  | - Generates Step Summary Manifest   |
                                  | - Enforces overall readiness gate   |
                                  +-------------------------------------+
```

### Core Tenets

1. **Non-Destructive & Read-Only**: The release-readiness workflow does not push Docker images, does not create Git tags, does not author GitHub Releases, and does not alter production infrastructure.
2. **Explicit Human Initiation**: Runs exclusively via `workflow_dispatch` (manual trigger) with optional semantic version input. Regular CI remains separate and lightweight.
3. **Traceability**: All candidate artifacts are deterministically bound to a short Git commit SHA and optional semantic release version.
4. **Zero-Secret Exposure**: No production credentials, private keys, database passwords, or interpolated Compose configurations are logged, baked into images, or stored in artifacts.

---

## 2. Docker Image Naming & Tagging Conventions

Candidate container images follow a standardized, predictable naming convention compatible with the root [docker-compose.yml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/docker-compose.yml) and container orchestration environments.

### Repository and Image Names

| Component | Image Name | Base Build Context | Target Dockerfile |
|---|---|---|---|
| Backend | `ai-code-review-backend` | `backend/` | [backend/Dockerfile](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/backend/Dockerfile) |
| Frontend | `ai-code-review-frontend` | `frontend/` | [frontend/Dockerfile](file:///c:/Users/kppus/OneDrive/Documents/Desktop/AI-Code-Review-Bot/frontend/Dockerfile) |

### Tagging Standards

1. **Short Commit SHA Tag (Mandatory)**:
   - Format: `ai-code-review-<service>:<short-sha>`
   - Example: `ai-code-review-backend:33498fa`, `ai-code-review-frontend:33498fa`
   - Purpose: Provides immutable traceability directly to the exact Git commit used for compilation.

2. **Semantic Version Tag (When Supplied)**:
   - Format: `ai-code-review-<service>:<release-version>`
   - Example: `ai-code-review-backend:2.0.0-rc1`, `ai-code-review-backend:v2.0.0`
   - Purpose: Maps the built image directly to an upcoming release milestone.

3. **CI Validation Tag (CI Workflow Baseline)**:
   - Format: `ai-code-review-<service>:ci-validate`
   - Used exclusively in automated PR/branch CI builds (`.github/workflows/ci.yml`) to validate syntax and dependencies without polluting local repository tags.

---

## 3. Release Version Validation Rules

When a release version is specified during manual workflow invocation or local scripting, it is validated against strict criteria:

### Validation Criteria

1. **Semantic Versioning Compliance**:
   - Matches regular expression: `^v?[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$`
   - Valid examples: `2.0.0`, `v2.0.0`, `2.0.0-rc1`, `v2.0.0-beta.2`
   - Invalid examples: `latest`, `v2.0`, `2.0.0.1`, `test-release`
2. **Preservation of Existing V1 Release Tag (`v1.0.0`)**:
   - The workflow explicitly checks if the input is `1.0.0` or `v1.0.0`.
   - Any attempt to reuse or overwrite `1.0.0` / `v1.0.0` is immediately rejected with an error to preserve historic V1 release immutability.
3. **Optional / SHA-Only Mode**:
   - If `release_version` is left empty, the workflow proceeds in **SHA-only verification mode**, tagging images solely with the short Git commit SHA.

---

## 4. Release-Readiness Quality Gates

The pipeline enforces five sequential quality gates:

| Gate | Stage | Verification Target | Failure Criteria |
|---|---|---|---|
| 1 | `validate-inputs` | Input string sanity & tag conflict detection | Version does not match SemVer regex or equals `v1.0.0` / `1.0.0` |
| 2 | `backend` | Java 21 Temurin Maven test suite | Any unit or integration test failure or compilation error |
| 3 | `frontend` | Node 20 LTS clean install & Vite build | Bundle compilation error, missing imports, or invalid syntax |
| 4 | `docker-verification` | Docker Compose configs & port isolation | Invalid YAML syntax, missing core services, or PostgreSQL host port exposure > 0 |
| 5 | `docker-verification` | Multi-stage image compilation | Multi-stage Docker build failure or missing image in local daemon |

---

## 5. How to Manually Trigger the Workflow

### Option A: Via GitHub Web Interface

1. Navigate to the repository on GitHub.
2. Click on the **Actions** tab.
3. Select the **Release Readiness & Versioned Image Preparation** workflow from the left sidebar.
4. Click the **Run workflow** dropdown button.
5. Select the target branch (typically `feature/v2-platform` or `main`).
6. Optionally enter the `release_version` (e.g. `2.0.0-rc1` or `2.0.0`), or leave blank for commit SHA-only verification.
7. Click **Run workflow**.

### Option B: Via GitHub CLI (`gh`)

```bash
# Trigger commit SHA-only verification
gh workflow run release-readiness.yml --ref feature/v2-platform

# Trigger with specific release version
gh workflow run release-readiness.yml --ref feature/v2-platform -f release_version=2.0.0-rc1
```

---

## 6. How to Inspect Results & Release Manifest

Once the workflow finishes, navigate to the run summary page. The `summary` job automatically renders a markdown **Release Readiness & Versioned Image Manifest** containing:

- **Overall Status**: `READY FOR RELEASE` or `BLOCKED`
- **Git Commit SHA**: 40-character and 7-character commit identifiers
- **Git Branch / Ref**: Source ref evaluated
- **Requested Release Version**: Supplied version or notice of SHA-only verification
- **Quality Gate Table**: Pass/Fail breakdown across input validation, backend tests, frontend build, Compose validation, and container builds
- **Prepared Local Image Tags**: List of generated image tags
- **Release Safeguards Notice**: Confirmation of dry-run boundaries

If any quality gate fails, the `Enforce Overall Readiness Gate` step fails the workflow run to clearly mark the candidate branch as not ready for release.

---

## 7. Security Safeguards & Operational Boundaries

To ensure maximum safety and prevent accidental deployments:

1. **Least-Privilege Scoping**: Workflow permissions are pinned to `contents: read`. No write tokens (`packages: write`, `deployments: write`, or `releases: write`) are granted.
2. **No Image Publishing**: The workflow does not authenticate to or push to Docker Hub, GitHub Container Registry (GHCR), AWS ECR, or any remote registry.
3. **No Automatic Git Tagging**: The workflow never executes `git tag` or modifies remote Git references.
4. **No Automatic GitHub Releases**: No draft or published GitHub releases are created.
5. **No Production Deployment**: No SSH connections, webhook triggers, Kubernetes manifests, or server updates are executed.
6. **No Production Secrets**: Workflow executes with test-scoped mocks and template placeholders. Production keys (`GEMINI_API_KEY`, `GITHUB_PRIVATE_KEY`, production `JWT_SECRET`) are never injected into the runner.
7. **Compose Secret Hygiene**: Compose commands use `--quiet` and format queries to avoid echoing interpolated environment values.

---

## 8. Manual Approval Requirements Before Publishing or Deployment

Passing the release-readiness workflow is a necessary prerequisite, but **never** automatically authorizes production deployment. Human maintainers must complete the following manual verification steps before final release:

1. **Review Release Readiness Manifest**: Confirm all 5 gates passed without warnings.
2. **Staging Environment Runtime Verification**:
   - Verify that the application boots against a live PostgreSQL 16 instance.
   - Verify that Flyway migrations execute cleanly.
   - Verify GitHub App webhook receipt and RS256 JWT generation using a test private key.
   - Verify Google Gemini API communication using a valid test API key.
3. **Formal Tag Creation**:
   - A repository maintainer manually creates and signs the release tag:
     ```bash
     git tag -a v2.0.0 -m "Release v2.0.0"
     git push origin v2.0.0
     ```
4. **Manual Image Publishing**:
   - When registry publication is required, images built from the verified commit SHA are pushed to the authorized container registry by an authenticated maintainer or dedicated publication pipeline.

---

## 9. How to Reproduce Checks Locally

All checks performed by the release-readiness workflow can be reproduced locally:

### 1. Whitespace & Hygiene Check
```bash
git diff --check
```

### 2. Backend Test Suite
From the repository root or `backend/`:
```bash
cd backend
mvn test
```
Expected: 499+ tests pass, 0 failures, 0 errors.

### 3. Frontend Bundle Build
From the repository root or `frontend/`:
```bash
cd frontend
npm ci
npm run build
```
Expected: Production bundle compiles cleanly into `frontend/dist/`.

### 4. Docker Compose & Port Isolation Validation
From the repository root:
```bash
# Validate development configuration syntax
docker compose -f docker-compose.yml config --quiet

# Validate production configuration syntax and port isolation
docker compose -f docker-compose.yml -f docker-compose.prod.yml config --quiet
```

### 5. Multi-Stage Container Image Builds
> [!NOTE]
> Building container images locally requires a running Docker daemon (Docker Desktop or Linux Docker service).

```bash
# Set version parameters for local testing
SHORT_SHA=$(git rev-parse --short=7 HEAD)
RELEASE_VER=2.0.0-rc1

# Build backend image with SHA and optional release tag
docker build \
  -t ai-code-review-backend:${SHORT_SHA} \
  -t ai-code-review-backend:${RELEASE_VER} \
  -f backend/Dockerfile \
  backend

# Build frontend image with SHA and optional release tag
docker build \
  --build-arg VITE_API_BASE_URL=/api/v1 \
  -t ai-code-review-frontend:${SHORT_SHA} \
  -t ai-code-review-frontend:${RELEASE_VER} \
  -f frontend/Dockerfile \
  frontend

# Inspect built local images
docker images --filter "reference=ai-code-review-*"
```

---

## 10. Known Limitations

1. **Runner-Local Images**: In GitHub Actions, Docker images built during the workflow are stored on the ephemeral `ubuntu-latest` runner virtual machine and are discarded when the runner shuts down.
2. **In-Memory Testing in CI**: Automated backend tests execute against mock services and in-memory test databases. True end-to-end integration with live external services (GitHub REST API, Google Gemini API) requires manual verification in a staging environment.
3. **Local Docker Requirement**: Running local container image builds requires an active Docker daemon on the developer workstation. Static Compose validation (`docker compose config --quiet`) functions offline without the daemon.
