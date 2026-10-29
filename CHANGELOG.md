# Changelog

All notable changes to the **AI Code Review Bot** platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0-rc1] - 2026-10-27

### Added
- **Deterministic Java AST Rule Engine**: Fast, local static analysis running before AI review:
  - `RULE-JAVA-EMPTY-CATCH`: Detects silent empty `catch` blocks.
  - `RULE-JAVA-SYSTEM-OUT`: Flags `System.out.println` and `System.err.println` usage.
  - `RULE-TODO-FIXME`: Highlights unresolved `TODO` and `FIXME` comments.
- **AI Code Fix Proposals & Patch Generation**:
  - Contextual fix generation via `/api/v1/code-reviews/findings/{id}/fix`.
  - Unified git diff patch validation with line offset safety checks.
  - Endpoints to inspect proposed/original content and download `.patch` files.
  - Status tracking lifecycle (`PENDING`, `APPLIED`, `REJECTED`).
- **Configurable Repository Review Policies & Quality Gates**:
  - Per-repository rule enablement and severity threshold configuration.
  - Automated quality gate evaluation (`PASSED`, `WARNING`, `FAILED`) per review.
- **GitHub Webhook Reliability Engine**:
  - `X-Hub-Signature-256` HMAC-SHA256 signature verification.
  - Idempotent delivery deduplication via `X-GitHub-Delivery` GUID.
  - Resilience4j retry with exponential backoff on transient errors and circuit breaker protection.
  - Background webhook retry scheduler for failed deliveries.
  - Webhook delivery audit API and management endpoints (`/api/v1/webhooks/deliveries/**`).
- **Observability & Operational Metrics**:
  - Prometheus scrape endpoint (`/actuator/prometheus`) exporting operational metrics.
  - Observability dashboard API (`/api/v1/observability/metrics`) reporting queue sizes, throughput, and status.
  - End-to-end trace propagation across threads via SLF4J MDC (`X-Correlation-Id`).
- **Container Security & Supply Chain Hardening**:
  - CycloneDX JSON SBOM generation for backend and frontend container images in CI.
  - Aqua Security Trivy container image scanning for CRITICAL and HIGH CVEs.
  - Automated security policy gate rejecting unapproved or expired CVE exceptions.
  - Non-root container execution (`appuser:appgroup`) in backend and Nginx alpine in frontend.
  - Completely isolated PostgreSQL port in production Compose (`ports: !reset []`).

### Changed
- **Dependency Baseline**:
  - Updated Spring Boot parent to `3.3.13`.
  - Upgraded embedded Apache Tomcat to `10.1.61`.
  - Aligned Spring Security to `6.5.11` and Spring Framework to `6.2.19`.
  - Upgraded PostgreSQL JDBC driver to `42.7.14`.
  - Standardized Jackson BOM to `2.18.11` and Micrometer to `1.15.12`.
- **Database Schema**: Added Flyway migrations `V10` through `V13` covering fix proposals, quality gates, and webhook delivery reliability.
- **Test Suite**: Expanded automated test suite to 499 tests (499 tests run, 0 failures, 0 errors, 2 skipped).

### Security Disclosure & Open Vulnerabilities
> **Notice**: The following four vulnerabilities remain unpatched in upstream dependencies and are NOT claimed as fixed:

1. **`CVE-2026-47884`** (CRITICAL - `org.springframework:spring-webmvc:6.2.19`):
   - *Status*: Unpatched in OSS 6.2.x (Spring 6.2.20 is commercial-only; OSS fix requires Spring 7 / Boot 4).
   - *Handling*: Audited, time-bounded exception in `.trivyignore` expiring on `2026-11-30` (Approver: `pushkar`).
   - *Mitigation*: The service is a pure `@RestController` REST API; `XsltView` is never instantiated or mapped. Container runs as non-root user `appuser`.
2. **`CVE-2026-47890`** (CRITICAL - `org.springframework:spring-web:6.2.19`):
   - *Status*: Unpatched in OSS 6.2.x (OSS fix requires Spring 7 / Boot 4).
   - *Handling*: Audited, time-bounded exception in `.trivyignore` expiring on `2026-11-30` (Approver: `pushkar`).
   - *Mitigation*: No Server-Sent Events (SSE) or streaming view fragment endpoints in codebase.
3. **`CVE-2026-40973`** (HIGH - `org.springframework.boot:spring-boot:3.3.13`):
   - *Status*: Unpatched in Spring Boot 3.3.x (upstream fix in `3.5.14+` / `4.0.6+`).
   - *Handling*: Evaluated as a non-blocking HIGH warning by the security gate.
   - *Mitigation*: Stateless JWT authentication (`SessionCreationPolicy.STATELESS`), persistent servlet sessions disabled (`server.servlet.session.persistent=false`), non-root container execution.
4. **`CVE-2026-41716`** (HIGH - `org.springframework.data:spring-data-commons:3.3.13`):
   - *Status*: Unpatched in Spring Data Commons 3.3.x (upstream fix in `3.5.12+` / `4.0.6+`).
   - *Handling*: Evaluated as a non-blocking HIGH warning by the security gate.
   - *Mitigation*: Strictly-typed entity repository lookups; dynamic user input strings are never evaluated as cache keys.
   - *Upgrade Dependency*: Upgrading requires refactoring repository test stubs due to breaking `SpecificationFluentQuery` signatures in Spring Data 3.5+.

### Verification Status & Staging Precondition
- **Automated CI Status**: **GREEN** (GitHub Actions run `40418480057`). Automated tests (499 tests run, 0 failures, 0 errors, 2 skipped), frontend build, container builds, and security gates pass.
- **Skipped Integration Tests**: Two live integration tests in `RealEndToEndCodeReviewVerificationTest` remain skipped (`@Disabled`) as they require a live local PostgreSQL database and real GitHub App private keys.
- **Production Readiness Notice**: This release candidate is **not yet verified for live production deployment**. Live staging smoke tests (verifying real GitHub App webhooks and Gemini AI review ingestion) must be conducted and signed off prior to official production release.
