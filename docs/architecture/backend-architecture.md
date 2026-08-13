# Backend Architecture & Layering Design

This document details the layered architecture model for the **AI Code Review Bot** backend service, explaining layer responsibilities and core architectural design principles.

---

## 1. High-Level Layered Flow

The application follows a standard layered (n-tier) architecture to separate concerns, enforce maintainability, and improve testability.

```
┌─────────────────────────────────────────────────────────┐
│                      HTTP Request                       │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                     │
│         (REST Endpoints & Webhook Receivers)            │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                      Service Layer                      │
│        (Business Logic & AI Engine Orchestration)       │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                     │
│           (Data Access & Query Abstractions)            │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                     Database Layer                      │
│              (Relational Data Storage)                  │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Layer Responsibilities

### 2.1 HTTP Request
- **Role**: Incoming network payload initiated by an external client (React Frontend UI, GitHub Webhook delivery agent, or third-party client).
- **Responsibility**:
  - Encapsulates HTTP method (`GET`, `POST`, `PUT`, `DELETE`), request headers (e.g. `Authorization`, `X-Hub-Signature-256`), URL parameters, and JSON payload body.

### 2.2 Controller Layer (`@RestController`)
- **Role**: Entry point for HTTP communication and REST API routing.
- **Responsibilities**:
  - Accept and route incoming HTTP requests to corresponding controller methods.
  - Parse request bodies into Data Transfer Objects (DTOs) and trigger validation annotations (`@Valid`).
  - Delegate business execution directly to the Service Layer.
  - Convert domain service responses into HTTP response DTOs and return appropriate HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`).

### 2.3 Service Layer (`@Service`)
- **Role**: The core computational and business processing heart of the application.
- **Responsibilities**:
  - Implement business rules, workflow pipelines, and validations (e.g., verifying user permissions, building prompt context for Gemini AI, evaluating review limits).
  - Manage transaction boundaries (`@Transactional`) ensuring atomic multi-step operations.
  - Orchestrate external integrations (e.g. fetching GitHub diffs, sending prompts to Gemini AI API, triggering email notifications).
  - Map entity data models to/from DTOs when communicating across architectural boundaries.

### 2.4 Repository Layer (`@Repository`)
- **Role**: Data Access Layer (DAL) abstracting database interaction.
- **Responsibilities**:
  - Provide CRUD (Create, Read, Update, Delete) interface methods for domain entities.
  - Execute Spring Data JPA query methods, custom JPQL, or native SQL queries.
  - Abstract database engine details away from the service layer.

### 2.5 Database Layer
- **Role**: Persistent relational storage engine (e.g., PostgreSQL / H2).
- **Responsibilities**:
  - Store entity records reliably with ACID guarantees.
  - Enforce table constraints, foreign key referential integrity, indexes, and primary keys.

---

## 3. Key Architectural Questions & Principles

### 3.1 Why Controllers Should Not Contain Business Logic

1. **Single Responsibility Principle (SRP)**: Controllers exist strictly to handle HTTP mechanics (request routing, header parsing, payload validation, status codes). Adding business rules violates SRP.
2. **Reusability**: Logic placed in controllers cannot be reused by non-HTTP triggers (such as scheduled cron background jobs, message queue listeners, or CLI tasks) without making unnecessary synthetic HTTP requests.
3. **Testability**: Unit testing controller logic requires mocking HTTP infrastructure (request wrappers, response headers). Keeping controllers thin allows core business logic in services to be tested quickly with pure Java unit tests.
4. **Maintenance & Readability**: Mixing data transformation, API calls, and validation rules in controllers leads to bloated "fat controllers" that are difficult to debug and maintain.

---

### 3.2 Why Services Exist

1. **Centralized Business Rules**: Serves as the authoritative location for all application rules, domain invariants, and logic execution flow.
2. **Transaction Management**: Defines logical unit-of-work boundaries (`@Transactional`). If a service operation performs multiple database updates (e.g., creating a `Review` and 10 `ReviewFinding` items), the service ensures all updates commit together or roll back on error.
3. **Decoupling Layers**: Serves as a buffer between web components (Controllers) and persistence engines (Repositories). Web layers remain unaware of how database queries or external API integrations operate under the hood.

---

### 3.3 Why Repositories Exist

1. **Data Access Abstraction**: Encapsulates persistence technology (JPA, Hibernate, JDBC). Services call high-level methods like `repository.findByGithubRepoId(id)` without managing SQL strings or connection pools.
2. **Portability & Swappability**: If the database vendor changes (e.g. migrating from H2 to PostgreSQL or MySQL), changes are isolated to configuration and repository implementations without altering business service code.
3. **Centralized Query Management**: Organizes query performance tuning, index utilization, and custom native queries in dedicated repository interfaces rather than scattering them across services.

---

### 3.4 Why DTOs Should Be Separated From Entities

Data Transfer Objects (DTOs) and Domain Entities serve fundamentally different purposes and must remain separate:

1. **Security & Data Exposure**:
   - Entities contain database structural details, audit fields, or internal state. Exposing entities directly in API responses can accidentally leak sensitive internal data or enable mass-assignment security vulnerabilities (where clients inject unexpected database fields).
2. **Decoupling API Contracts From DB Schema**:
   - Entities change when database schemas are refactored.
   - DTOs change when external REST API contracts evolve.
   - Keeping them separated ensures that a database migration or table column rename does not unexpectedly break client applications relying on the REST API.
3. **Preventing Circular Dependency / Infinite Loops**:
   - Relational entities often feature bi-directional mapping (e.g., `@ManyToOne` and `@OneToMany`). Serializing raw entities to JSON directly leads to infinite circular reference stack overflow exceptions.
4. **Tailored Data Representations**:
   - Different endpoints require different representations of the same entity (e.g., a lightweight `UserSummaryDto` for dropdown lists vs. a full `UserProfileDto` for settings pages). DTOs allow tailored projections without cluttering domain entities.
