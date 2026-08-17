# AI Code Review Bot — Project Walkthrough & Showcase

A concise, high-level project walkthrough guide designed for live demonstrations, portfolio presentations, and technical system reviews.

---

## 🎯 Problem Statement
Manual code reviews are critical for software quality but often slow down engineering velocity. Senior engineers spend valuable time spotting routine syntax errors, missing error handlers, style inconsistencies, and security flaws in Pull Requests instead of focusing on high-level system design.

## 💡 Solution
**AI Code Review Bot** is an automated, AI-assisted code review platform. It accepts GitHub Pull Requests, extracts code diffs, executes multi-category analysis using Google Gemini AI, generates actionable findings with exact line numbers and recommendations, and visualizes review metrics in a real-time React dashboard.

---

## 🏛️ System Architecture Overview

```text
[ Developer / Browser ] 
        │
        ▼ (React 18 SPA + Vite)
[ Frontend UI Layer ]
        │
        ▼ (REST API / JWT Auth)
[ Spring Boot 3 Backend ]
   ├── Controllers & Security (JWT, MDC Correlation Filter)
   ├── Async Review Engine (ThreadPool Task Executor)
   ├── Resilience Engine (Retry Policy & Circuit Breaker)
   └── Persistence Layer (Spring Data JPA / PostgreSQL)
        │                                  │
        ▼ (HTTPS)                          ▼ (HTTPS)
[ GitHub REST API ]               [ Google Gemini AI API ]
```

---

## ⭐ Core Features

1. **Async PR Submission**: Submits PR reviews via `POST /api/v1/code-reviews/pull-request`, returning `HTTP 202 Accepted` instantly.
2. **Asynchronous Polling Engine**: Frontend polls review status (`IN_PROGRESS` → `COMPLETED` / `FAILED`) every 2.5s and fetches AI summaries automatically.
3. **Rich Findings Visualizer**: Render finding cards with file paths, line ranges (`Line X` or `Lines X-Y`), severity badges (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`), category tags, and AI code fix recommendations.
4. **Interactive Severity Filtering**: Filter review findings dynamically by severity ratings (`ALL`, `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`).
5. **Review History & Search**: Paginated review history featuring status filters, repository/owner search inputs, sorting, and execution duration tracking.
6. **Production Observability**: Integrated Actuator health indicators, Micrometer Prometheus metrics, and MDC correlation ID tracing across threads.

---

## 🛠️ Key Technical Decisions

| Technical Choice | Rationale |
| :--- | :--- |
| **Spring Boot 3 + Java 21** | Type-safe, enterprise-grade REST APIs, async task execution, Actuator observability. |
| **React 18 + Vite 5** | High-performance single page application (SPA), fast HMR, clean component state. |
| **PostgreSQL 16** | ACID-compliant relational storage for structured reviews and findings. |
| **Async Execution (HTTP 202)** | Prevents HTTP thread pool exhaustion during multi-second AI inference calls. |
| **Resilience Engine** | Protects application against downstream GitHub/Gemini rate limits or outages. |
| **Multi-Stage Docker Builds** | Reduces container image size by 70%+ and enforces non-root container security. |

---

## 🔐 Security & Reliability Highlights

- **Stateless JWT**: Signed using HMAC-SHA256 with role-based authorization (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_DEVELOPER`).
- **Secret Protection**: Zero hardcoded secrets; API keys injected via runtime environment variables.
- **Fail-Safety**: Exponential backoff retries and circuit breaker policies for third-party API calls.
- **Audited Build Pipeline**: Verified 100% clean Vite production build and 258 passing Spring Boot regression unit/integration tests.

---

## 🚀 Deployment & Future Scalability

- **Containerized Stack**: Fully containerized with Docker and Docker Compose (`PostgreSQL`, `Backend`, `Frontend`).
- **Future Scaling**: Horizontal scaling of Spring Boot instances behind an AWS ALB or Nginx load balancer; migrating async task execution to Redis / RabbitMQ worker queues for high-volume enterprise workloads.
