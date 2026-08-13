# Database Design & Domain Model

This document outlines the core domain concepts, relationships, lifecycle states, and enumeration values for the **AI Code Review Bot**.

---

## 1. Domain Concepts

### 1.1 User

- **Purpose**: Represents an account or registered developer using the AI Code Review Bot platform. Users own repositories, configure review preferences, trigger manual reviews, and inspect review metrics.
- **Important Fields**:
  - `id` (`BIGINT` / `UUID`): Primary key uniquely identifying the user record.
  - `github_id` (`BIGINT`): Unique identity numeric ID provided by GitHub OAuth/Webhook payloads.
  - `username` (`VARCHAR(255)`): GitHub handle or account username.
  - `email` (`VARCHAR(255)`): Primary contact and notification email.
  - `avatar_url` (`VARCHAR(512)`): URL pointing to the user's GitHub avatar image.
  - `role` (`VARCHAR(50)`): Role-based access control level (e.g., `ADMIN`, `DEVELOPER`).
  - `created_at` (`TIMESTAMP`): Creation timestamp (UTC).
  - `updated_at` (`TIMESTAMP`): Last modification timestamp (UTC).
- **Relationships**:
  - **One-to-Many** with `Repository`: A user can register and manage multiple repositories.
  - **One-to-Many** with `Review`: A user can manually trigger or request multiple AI code reviews.
- **Why the Relationship Exists**:
  - Linking `User` to `Repository` establishes ownership, authorization boundaries, and repository management access.
  - Linking `User` to `Review` maintains an audit log identifying which user initiated a specific review run.

---

### 1.2 Repository

- **Purpose**: Represents a software code repository registered with the bot for automated AI code review execution upon pull request activity.
- **Important Fields**:
  - `id` (`BIGINT` / `UUID`): Primary key.
  - `user_id` (`BIGINT` / `UUID`): Foreign key referencing `User.id` (repository owner/admin).
  - `github_repo_id` (`BIGINT`): Unique repository identifier assigned by GitHub.
  - `name` (`VARCHAR(255)`): Repository name (e.g., `AI-Code-Review-Bot`).
  - `full_name` (`VARCHAR(512)`): Qualified repository path including owner handle (e.g., `PrajapatiPushkar/AI-Code-Review-Bot`).
  - `html_url` (`VARCHAR(512)`): Web link to the repository on GitHub.
  - `default_branch` (`VARCHAR(100)`): Primary target branch (e.g., `main` or `master`).
  - `is_active` (`BOOLEAN`): Toggle flag enabling or disabling automated reviews for this repository.
  - `created_at` (`TIMESTAMP`): Registration timestamp.
  - `updated_at` (`TIMESTAMP`): Last modification timestamp.
- **Relationships**:
  - **Many-to-One** with `User`: Belongs to a single platform user or organization owner.
  - **One-to-Many** with `PullRequest`: A repository contains multiple pull requests over its lifecycle.
- **Why the Relationship Exists**:
  - Repositories form the organizational container for pull requests and store repository-specific rules, strictness settings, and analytics.

---

### 1.3 PullRequest

- **Purpose**: Tracks pull requests opened or updated in a registered repository that require or have undergone AI code review.
- **Important Fields**:
  - `id` (`BIGINT` / `UUID`): Primary key.
  - `repository_id` (`BIGINT` / `UUID`): Foreign key referencing `Repository.id`.
  - `github_pr_id` (`BIGINT`): GitHub's internal unique PR entity ID.
  - `pr_number` (`INT`): The repository-scoped pull request number (e.g., `#42`).
  - `title` (`VARCHAR(512)`): Headline description of the pull request.
  - `author_handle` (`VARCHAR(255)`): GitHub handle of the developer who authored the PR.
  - `source_branch` (`VARCHAR(255)`): Feature branch name being merged from.
  - `target_branch` (`VARCHAR(255)`): Target branch name being merged into (e.g., `main`).
  - `status` (`VARCHAR(50)`): State of the PR on GitHub (`OPEN`, `CLOSED`, `MERGED`).
  - `created_at` (`TIMESTAMP`): PR creation timestamp.
  - `updated_at` (`TIMESTAMP`): Last PR update timestamp.
- **Relationships**:
  - **Many-to-One** with `Repository`: Belongs to one registered repository.
  - **One-to-Many** with `Review`: A single pull request can undergo multiple reviews over time as new commits are pushed.
- **Why the Relationship Exists**:
  - Groups historical AI review sessions under a single code change request, enabling developers to track quality progression across pull request revisions.

---

### 1.4 Review

- **Purpose**: Represents an individual AI code review execution session performed against a specific commit SHA of a `PullRequest`.
- **Important Fields**:
  - `id` (`BIGINT` / `UUID`): Primary key.
  - `pull_request_id` (`BIGINT` / `UUID`): Foreign key referencing `PullRequest.id`.
  - `triggered_by_user_id` (`BIGINT` / `UUID`, Nullable): Foreign key referencing `User.id` if triggered manually (null for automated webhook triggers).
  - `commit_hash` (`VARCHAR(64)`): Git commit SHA hash analyzed during this review.
  - `status` (`VARCHAR(50)`): Current state in the review execution lifecycle (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`).
  - `summary` (`TEXT`, Nullable): High-level AI-generated executive summary of the review results.
  - `error_message` (`TEXT`, Nullable): Detailed failure output if execution encountered an error.
  - `started_at` (`TIMESTAMP`, Nullable): Execution start timestamp.
  - `completed_at` (`TIMESTAMP`, Nullable): Execution completion timestamp.
  - `created_at` (`TIMESTAMP`): Record creation timestamp.
  - `updated_at` (`TIMESTAMP`): Last record update timestamp.
- **Relationships**:
  - **Many-to-One** with `PullRequest`: Associated with a specific pull request revision.
  - **Many-to-One** with `User` (Optional): Associated with the user who initiated the review (if manually requested).
  - **One-to-Many** with `ReviewFinding`: Produces multiple detailed finding items.
- **Why the Relationship Exists**:
  - Encapsulates state management, status tracking, and performance metrics for a single analysis execution without locking individual code findings into a single monolithic record.

---

### 1.5 ReviewFinding

- **Purpose**: Captures a single line-level or file-level code review finding (bug, security flaw, suggestion) discovered by Google Gemini AI during a review session.
- **Important Fields**:
  - `id` (`BIGINT` / `UUID`): Primary key.
  - `review_id` (`BIGINT` / `UUID`): Foreign key referencing `Review.id`.
  - `file_path` (`VARCHAR(1024)`): Relative repository file path where the issue was detected (e.g., `src/main/java/Service.java`).
  - `line_number` (`INT`): Line number in the target file associated with the finding.
  - `severity` (`VARCHAR(50)`): Impact classification (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  - `category` (`VARCHAR(50)`): Domain category (`BUG`, `SECURITY`, `PERFORMANCE`, `STYLE`, `BEST_PRACTICE`, `MAINTAINABILITY`).
  - `title` (`VARCHAR(255)`): Concise headline summary of the finding.
  - `description` (`TEXT`): Detailed explanation of the issue and why it matters.
  - `suggested_code` (`TEXT`, Nullable): Refactored code snippet proposed by the AI to resolve the issue.
  - `created_at` (`TIMESTAMP`): Record creation timestamp.
- **Relationships**:
  - **Many-to-One** with `Review`: Belongs to a specific review execution session.
- **Why the Relationship Exists**:
  - Enables granular database queries, filtering by severity/category, calculation of aggregate repository metrics, and automated inline comment posting to GitHub.

---

## 2. One-to-Many Relationships Summary

| Parent Entity | Child Entity | Description |
| :--- | :--- | :--- |
| **`User`** | **`Repository`** | One user can register and configure multiple code repositories. |
| **`User`** | **`Review`** | One user can trigger multiple manual review runs across different repositories. |
| **`Repository`** | **`PullRequest`** | One repository contains multiple pull requests submitted by contributors. |
| **`PullRequest`** | **`Review`** | One pull request can have multiple review execution cycles over time. |
| **`Review`** | **`ReviewFinding`** | One review run produces multiple individual findings and code suggestions. |

---

## 3. Review Lifecycle & State Transitions

The review lifecycle tracks the real-time execution state of an AI code review job from invocation to completion.

### State Diagram

```
  [ GitHub Webhook / User Action ]
                 │
                 ▼
          ┌──────────────┐
          │   PENDING    │
          └──────┬───────┘
                 │
                 ▼
          ┌──────────────┐
          │ IN_PROGRESS  │
          └──────┬───────┘
                 ├──────────────────────────────┐
                 ▼                              ▼
          ┌──────────────┐              ┌──────────────┐
          │  COMPLETED   │              │    FAILED    │
          └──────────────┘              └──────────────┘
```

### Lifecycle Flow Explanation

1. **`PENDING`**:
   - A pull request webhook payload arrives or a user manually triggers a review.
   - The `Review` entity is persisted in the database with status `PENDING` and enqueued for processing.
2. **`IN_PROGRESS`**:
   - The backend worker picks up the review task.
   - Diff patches are fetched via the GitHub REST API, formatted into AI prompt context, and sent to Google Gemini AI.
3. **`COMPLETED`**:
   - Gemini AI returns the analysis successfully.
   - Granular `ReviewFinding` entities are persisted to the database.
   - Summary and inline comments are posted back to GitHub, and the review status transitions to `COMPLETED`.
4. **`FAILED`**:
   - If an error occurs (e.g., GitHub API failure, Gemini rate limit, network timeout, invalid payload), the exception details are captured in `error_message` and the status transitions to `FAILED`.

---

## 4. Enumeration Values

### 4.1 Review Status Values (`Review.status`)

| Value | Meaning |
| :--- | :--- |
| `PENDING` | Review has been created and queued for execution. |
| `IN_PROGRESS` | Review engine is currently fetching diffs, invoking Gemini AI, or processing data. |
| `COMPLETED` | Review execution finished successfully and findings were saved and posted. |
| `FAILED` | Review execution failed due to an API, system, or timeout error. |

### 4.2 Finding Severity Values (`ReviewFinding.severity`)

| Value | Description | Impact Level |
| :--- | :--- | :--- |
| `INFO` | Informational remarks, nitpicks, or syntax notes without operational risk. | Minimal |
| `LOW` | Minor formatting or style recommendations with low likelihood of impact. | Low |
| `MEDIUM` | Suboptimal logic, minor performance bottlenecks, or code smell requiring refactoring. | Moderate |
| `HIGH` | High-risk logic defects, unhandled edge-case exceptions, or resource leaks. | High |
| `CRITICAL` | Severe flaws such as SQL injection, exposed secrets, or severe memory/data corruption risks. | Urgent |

### 4.3 Finding Category Values (`ReviewFinding.category`)

| Value | Focus Area |
| :--- | :--- |
| `BUG` | Logic errors, null pointer vulnerabilities, off-by-one errors, or incorrect conditionals. |
| `SECURITY` | Authentication bypasses, hardcoded secrets, injection flaws, or unsafe dependencies. |
| `PERFORMANCE` | Inefficient algorithms, memory leaks, unindexed queries, or blocking operations. |
| `STYLE` | Naming conventions, code formatting, dead code, or redundant imports. |
| `BEST_PRACTICE` | Deviation from framework idioms, clean code principles, or language standards. |
| `MAINTAINABILITY` | Complex functions, high cyclomatic complexity, excessive coupling, or duplication. |
