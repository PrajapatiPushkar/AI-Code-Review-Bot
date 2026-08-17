# AI Code Review Bot — Technical Interview Guide

This guide contains project-specific technical questions, detailed answers, architectural rationale, and system design explanations to help present the **AI Code Review Bot** effectively in software engineering interviews.

---

## 1. Backend Architecture & Core Technologies

### Q1. Why did you choose Spring Boot for the backend?
**Answer:**
Spring Boot 3 provides an enterprise-ready foundation with out-of-the-box support for RESTful Web Services, Spring Security, Spring Data JPA, dependency injection, and asynchronous thread pool execution (`@Async`). Additionally, Spring Boot Actuator and Micrometer provide built-in production observability (health checks, Prometheus metrics, MDC correlation tracing) without requiring custom monitoring infrastructure.

### Q2. Why PostgreSQL as the relational database?
**Answer:**
Code review management requires strict ACID compliance, transactional integrity, and strong relational schema enforcement between `code_reviews` and `code_review_findings`. PostgreSQL 16 offers high reliability, strong indexing capabilities, robust JSON/Text column support, and seamless integration with Spring Data JPA and Flyway migration tools.

### Q3. Why use Flyway for database schema migration?
**Answer:**
Flyway provides version-controlled, repeatable database migrations. It ensures that database schema evolution is tracked in source code (`V1__...sql`, `V2__...sql`), preventing environment drift across local development, CI testing, and production Docker deployments.

### Q4. Why use JPA / Hibernate instead of plain JDBC?
**Answer:**
Hibernate/JPA abstracts raw SQL queries into object-oriented Java entity graphs (`@Entity`, `@ManyToOne`, `@OneToMany`), eliminating boiler-plate SQL mapping. Spring Data JPA repository interfaces (`JpaRepository`, `JpaSpecificationExecutor`) simplify query creation, pagination (`Pageable`), and transaction management (`@Transactional`).

### Q5. Why use Data Transfer Objects (DTOs) instead of exposing JPA Entities?
**Answer:**
Exposing JPA entities directly via REST controllers creates security vulnerabilities (over-posting, mass assignment), causes serialization circular references (`Infinite Recursion` with bidirectional relationships), and tightly couples the external API contract to the internal database schema. DTOs (e.g. `CodeReviewHistoryResponse`, `GithubPullRequestReviewRequest`) decouple API models from data persistence.

### Q6. What are the benefits of Layered Architecture (Controller → Service → Repository)?
**Answer:**
- **Separation of Concerns**: Controllers manage HTTP request/response validation; Services handle business logic and async orchestration; Repositories manage database interaction.
- **Maintainability & Testability**: Each layer can be unit tested independently using mocks (e.g., Mockito testing `Service` without spinning up a real database).

---

## 2. Security & Authentication

### Q7. How does stateless JWT authentication work in this application?
**Answer:**
1. The client sends credentials (`usernameOrEmail`, `password`) to `POST /api/v1/auth/login`.
2. `AuthService` verifies credentials using `AuthenticationManager` and `BCryptPasswordEncoder`.
3. Upon success, `JwtService` generates an HMAC-SHA256 signed JWT containing the username, issuance time, and expiration time.
4. On subsequent requests, the frontend Axios interceptor attaches `Authorization: Bearer <token>`.
5. `JwtAuthenticationFilter` intercepts the request, validates the token signature, extracts user details, and sets `SecurityContextHolder`.

### Q8. How are protected endpoints secured in Spring Security?
**Answer:**
In `SecurityConfig`, `authorizeHttpRequests` configures rule matching:
- Public endpoints (`/auth/**`, `/actuator/health`, `/health`) use `.permitAll()`.
- Protected review endpoints (`/code-reviews/**`, `/github/installations/**`) require specific roles (`.hasAnyRole("USER", "ADMIN", "DEVELOPER")`).
- Custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) return structured JSON errors without exposing stack traces.

### Q9. How are GitHub App private keys and API credentials handled safely?
**Answer:**
No credentials, tokens, or private keys are ever hardcoded or committed to git. GitHub App ID, private keys (`GITHUB_PRIVATE_KEY`), and Gemini API keys (`GEMINI_API_KEY`) are injected at runtime via environment variables or external secret stores. In logs and metrics, credentials are explicitly sanitized to prevent secret leaks.

---

## 3. AI & Google Gemini Integration

### Q10. How does the application integrate with Google Gemini AI?
**Answer:**
The `AsyncCodeReviewRunner` retrieves the unified git diff of a Pull Request from GitHub API. It builds a structured multi-category prompt instructing Gemini AI to evaluate the diff across categories (`BUG`, `SECURITY`, `PERFORMANCE`, `CODE_STYLE`, `MAINTAINABILITY`, `OTHER`) and return a structured JSON response containing line numbers, file paths, severity ratings, messages, and code suggestions.

### Q11. How are generated AI findings persisted?
**Answer:**
The AI response is parsed into Java DTOs, converted into `CodeReviewFinding` entities linked to the parent `CodeReview` record via a `@ManyToOne` foreign key, and batch-saved to PostgreSQL inside a transactional database boundary.

### Q12. How are AI generation failures handled?
**Answer:**
If the Gemini API call fails due to transient rate limits or network issues, `ResilienceExecutor` retries the call using exponential backoff. If all retries fail, the `CodeReview` status is updated to `FAILED`, a clear failure summary is recorded, and the UI displays a clean error banner rather than crashing.

---

## 4. Asynchronous Processing & Status Polling

### Q13. Why is PR code review execution asynchronous?
**Answer:**
Fetching git diffs from GitHub and executing multimodal AI inference with Gemini can take several seconds. Executing this synchronously inside a REST request thread would block HTTP connection pools, risk gateway timeouts (504 Gateway Timeout), and create a poor user experience.

### Q14. Why return HTTP 202 Accepted upon submission?
**Answer:**
`HTTP 202 Accepted` indicates that the request has been validated and accepted for processing, but execution is not yet complete. It immediately returns the initialized `codeReviewId` and status `IN_PROGRESS`, allowing the client to unblock instantly.

### Q15. How does status polling work on the frontend?
**Answer:**
The React `ReviewDetailsPage` uses `setInterval` to poll `GET /api/v1/code-reviews/{id}/status` every 2.5 seconds when `status === 'IN_PROGRESS'`. When the status transitions to a terminal state (`COMPLETED` or `FAILED`), polling automatically stops (`clearInterval`) and the complete result is fetched via `GET /api/v1/code-reviews/{id}/result`.

---

## 5. Reliability, Resilience, & Idempotency

### Q16. Why implement retries and circuit breakers for external calls?
**Answer:**
External third-party APIs (GitHub REST API and Gemini API) are subject to network jitter, rate limiting (429 Too Many Requests), and temporary outages (503 Service Unavailable). Retry policies with exponential backoff handle transient blips, while Circuit Breakers prevent cascading resource exhaustion during prolonged outages.

### Q17. How does the system prevent duplicate PR review submissions?
**Answer:**
Before launching an async review, `GithubPullRequestCodeReviewService` queries the database for existing active reviews matching the same `(owner, repository, pullRequestNumber, commitSha)` combination. If an active `IN_PROGRESS` review exists, it returns the existing review ID rather than starting duplicate processing.

---

## 6. Database Schema & Data Modeling

### Q18. Explain the relationship between `code_reviews` and `code_review_findings`.
**Answer:**
There is a 1-to-Many (`1:N`) relationship between `code_reviews` and `code_review_findings`. A single PR review run (`code_reviews`) can produce multiple individual findings (`code_review_findings`). In JPA, this is mapped via `@ManyToOne` on `CodeReviewFinding` with a foreign key column `code_review_id`.

### Q19. Why is `commit_sha` stored on the review record?
**Answer:**
PRs evolve over time as new commits are pushed. Storing `commit_sha` ensures review findings are explicitly tied to the exact code state at execution time and allows re-reviews when new commits are pushed.

---

## 7. Docker & Infrastructure

### Q20. Why use multi-stage Docker builds?
**Answer:**
Multi-stage builds separate the build environment (which includes heavyweight tools like JDK, Maven, Node.js) from the runtime image (which only requires JRE or Nginx). This reduces production Docker image size by 70%+ and removes compilers from production containers to minimize security attack surface.

### Q21. Why should PostgreSQL port 5432 NOT be exposed publicly in production?
**Answer:**
Exposing database ports publicly to the internet invites brute-force attacks and unauthorized access attempts. In `docker-compose.prod.yml`, port `5432` is removed from host binding so PostgreSQL is strictly accessible over the internal container network (`app-network`).

---

## 8. Frontend Architecture

### Q22. How does React handle state and communication with Spring Boot?
**Answer:**
React pages communicate with Spring Boot via a centralized Axios instance (`api.js`). `AuthContext` maintains user identity and token state across components, while custom hooks (`useAuth`) provide clean access to authentication actions. Reusable components (`Loading`, `ErrorMessage`, `EmptyState`, `FindingCard`) handle async loading, error banners, and empty data states cleanly.

---

## 9. System Design & Elevator Pitch

### 2-Minute Project Elevator Pitch
> *"AI Code Review Bot is a full-stack automated code review system built with Spring Boot 3, React 18, PostgreSQL, and Google Gemini AI. It automates GitHub Pull Request reviews by extracting PR diffs, running AI analysis across security, performance, bug, and maintainability categories, and persisting findings. To ensure high throughput and user responsiveness, reviews run asynchronously using HTTP 202 Accepted and non-blocking worker threads. It features stateless JWT security, Resilience4j-style retry and circuit breaker policies for external API calls, Actuator and Micrometer observability with MDC correlation tracing, and a responsive React dashboard with severity filtering. The entire stack is containerized with multi-stage Docker builds."*
