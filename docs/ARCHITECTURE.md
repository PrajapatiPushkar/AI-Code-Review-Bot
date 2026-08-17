# AI Code Review Bot — System Architecture Document

This document provides a comprehensive technical overview of the system architecture, component design, data flow, authentication model, async execution engine, resilience mechanisms, and deployment infrastructure for the **AI Code Review Bot**.

---

## 1. System Overview

**AI Code Review Bot** is an enterprise-grade full-stack application engineered to automate Pull Request code reviews for GitHub repositories. By integrating Spring Boot 3, React 18, PostgreSQL 16, Google Gemini AI, and GitHub REST APIs, the system automatically analyzes code diffs, generates structured findings across security/performance/bug categories, tracks progress asynchronously, and exposes health and performance metrics via Spring Boot Actuator and Micrometer.

```mermaid
flowchart TD
    User([Developer / Reviewer]) -->|HTTPS / Port 5173| ReactApp[React 18 SPA / Vite]
    ReactApp -->|REST API / Port 8080| SecurityFilter[Spring Security & JWT Filter]
    SecurityFilter -->|Stateless Auth| Controllers[Spring REST Controllers]
    Controllers -->|Async Review Execution| AsyncRunner[Async Code Review Runner]
    
    AsyncRunner -->|Fetch PR Diffs| GitHubAPI[GitHub REST API]
    AsyncRunner -->|Multi-Category Prompt| GeminiAI[Google Gemini AI API]
    AsyncRunner -->|Resilience Engine| Resilience[Retry & Circuit Breaker Engine]
    
    AsyncRunner -->|JPA / Hibernate| Postgres[(PostgreSQL 16 Database)]
    
    Controllers -->|Health & Metrics| Actuator[Actuator & Micrometer Engine]
```

---

## 2. Component Architecture

The backend follows a strict **Layered Clean Architecture**:

```text
com.pushkar.codereview
├── auth/                       # JWT Authentication & Registration API
│   ├── dto/                    # LoginRequest, LoginResponse, RegisterRequest
│   ├── AuthController.java
│   └── AuthService.java
├── config/                     # Security, Actuator Health, & CORS Config
│   ├── health/                 # GithubApiHealthIndicator, GeminiAiHealthIndicator
│   ├── CorrelationIdFilter.java
│   ├── MetricConfig.java
│   └── SecurityConfig.java
├── exception/                  # Global Exception Handling
│   └── GlobalExceptionHandler.java
├── github/                     # GitHub Integration & Review Core
│   └── review/
│       ├── controller/         # CodeReviewController, CodeReviewHistoryController
│       ├── dto/                # Request & Response DTOs
│       ├── persistence/        # CodeReview, CodeReviewFinding Entities & Repositories
│       ├── AsyncCodeReviewRunner.java
│       ├── CodeReviewHistoryService.java
│       └── GithubPullRequestCodeReviewService.java
├── resilience/                 # Fail-Safe Execution & Retries
│   ├── CircuitBreaker.java
│   ├── ResilienceExecutor.java
│   └── RetryPolicy.java
├── security/                   # Spring Security & JWT Token Utilities
│   ├── CurrentUserService.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
└── user/                       # User Persistence & Management
    ├── dto/
    ├── User.java
    ├── UserRepository.java
    └── UserService.java
```

---

## 3. End-to-End Request & Asynchronous Review Flow

When a user triggers a code review for a GitHub Pull Request:

```mermaid
sequenceDiagram
    autonumber
    actor User as Developer / UI
    participant Frontend as React SPA (Vite)
    participant AuthFilter as JWT Auth Filter
    participant Controller as CodeReviewController
    participant Service as GithubPRCodeReviewService
    participant AsyncRunner as AsyncCodeReviewRunner
    participant GitHub as GitHub REST API
    participant Gemini as Google Gemini AI API
    participant DB as PostgreSQL DB

    User->>Frontend: Submit PR Form (installationId, owner, repo, pr#)
    Frontend->>AuthFilter: POST /api/v1/code-reviews/pull-request (Bearer JWT)
    AuthFilter->>Controller: Authenticated Request
    Controller->>Service: executeCodeReview(...)
    Service->>DB: Save CodeReview (status = IN_PROGRESS)
    Service->>AsyncRunner: Submit Async Task (reviewId)
    Service-->>Frontend: Return HTTP 202 Accepted (CodeReviewExecutionResult)
    
    par Async Processing
        AsyncRunner->>MDC: Set correlationId
        AsyncRunner->>GitHub: Fetch PR Diffs (Retries enabled)
        GitHub-->>AsyncRunner: Return Unified Diff
        AsyncRunner->>Gemini: Execute Multi-Category Prompt Analysis
        Gemini-->>AsyncRunner: Return JSON Review Findings
        AsyncRunner->>DB: Save Findings & Update Status (COMPLETED / FAILED)
        AsyncRunner->>MDC: Clear MDC Context
    and Status Polling
        loop Every 2.5 seconds
            Frontend->>Controller: GET /api/v1/code-reviews/{id}/status
            Controller-->>Frontend: Return status (IN_PROGRESS / COMPLETED / FAILED)
        end
    end

    Frontend->>Controller: GET /api/v1/code-reviews/{id}/result
    Controller->>DB: Fetch Review & Summary
    Controller-->>Frontend: Return Complete Review Result
    Frontend->>User: Render AI Summary & Navigate to Findings
```

---

## 4. Authentication Architecture

- **Stateless JWT**: Authentication relies on HMAC-SHA256 signed JSON Web Tokens.
- **Filter Chain**: `JwtAuthenticationFilter` intercepts incoming HTTP requests, parses the `Authorization: Bearer <token>` header, verifies the signature using `JwtService`, loads `UserDetails` via `CustomUserDetailsService`, and sets `SecurityContextHolder`.
- **CORS Compatibility**: `SecurityConfig` allows preflight `OPTIONS` requests and cross-origin requests from `http://localhost:5173`.
- **Exception Handling**: Custom `AuthenticationEntryPoint` (401 Unauthorized) and `AccessDeniedHandler` (403 Forbidden) return structured JSON error responses without leaking internal stack traces.

---

## 5. Persistence & Database Design

### ER Diagram

```mermaid
erDiagram
    USERS ||--o{ CODE_REVIEWS : creates
    CODE_REVIEWS ||--|{ CODE_REVIEW_FINDINGS : contains

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        varchar role
        timestamp created_at
    }

    CODE_REVIEWS {
        bigint id PK
        bigint installation_id
        varchar owner
        varchar repository_name
        integer pull_request_number
        varchar commit_sha
        varchar status
        text review_summary
        integer total_findings
        integer posted_comments_count
        timestamp created_at
        timestamp completed_at
    }

    CODE_REVIEW_FINDINGS {
        bigint id PK
        bigint code_review_id FK
        varchar file_path
        integer line_number
        integer end_line_number
        varchar severity
        varchar category
        text message
        text suggestion
        timestamp created_at
    }
```

---

## 6. Resilience & Observability

### Fail-Safe Execution
- **Exponential Backoff Retries**: `ResilienceExecutor` retries external calls to GitHub and Gemini APIs on transient errors (e.g. rate limits, 5xx server errors).
- **Circuit Breaker**: `CircuitBreaker` monitors failure rates. When threshold is exceeded, the circuit opens to prevent downstream system exhaustion.

### Observability & MDC Tracing
- **Spring Boot Actuator**: Exposes production-safe `/actuator/health` (with `/liveness` and `/readiness` probes), `/actuator/info`, `/actuator/metrics`, and `/actuator/prometheus`.
- **Custom Health Indicators**: `GithubApiHealthIndicator` and `GeminiAiHealthIndicator` verify API readiness without leaking credentials.
- **Micrometer Metrics**: Tracks counters and timers for reviews (`code_review.submission.total`, `code_review.duration`), GitHub API calls (`github.api.request`), and Gemini API calls (`gemini.api.request`).
- **MDC Correlation Tracing**: `CorrelationIdFilter` captures/generates `X-Correlation-ID` and injects it into SLF4J MDC, which `AsyncCodeReviewRunner` propagates across asynchronous execution threads.

---

## 7. Containerized Infrastructure

```mermaid
graph LR
    subgraph Docker Network [app-network]
        FE[ai-code-review-frontend<br/>React + Nginx<br/>Port 5173:80]
        BE[ai-code-review-backend<br/>Spring Boot 3 + Java 21<br/>Port 8080]
        DB[ai-code-review-db<br/>PostgreSQL 16<br/>Port 5432]
    end

    FE -->|HTTP API Calls| BE
    BE -->|HikariCP Connection| DB
    BE -->|External HTTPS| GitHub[GitHub REST API]
    BE -->|External HTTPS| Gemini[Google Gemini AI]
```

- **Multi-Stage Builds**:
  - Backend: Maven 3.9 build stage → Eclipse Temurin 21 JRE runtime stage under non-root user (`appuser`).
  - Frontend: Node 20 build stage → Nginx Alpine runtime stage.
- **Data Persistence**: Named volume `postgres-data` preserves PostgreSQL database state across container restarts.
- **Production Port Security**: PostgreSQL port `5432` is restricted to internal container network in production (`docker-compose.prod.yml`).
