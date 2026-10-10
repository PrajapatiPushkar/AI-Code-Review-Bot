# Review Analytics & Repository Health Architecture

This document describes the architectural design and operational model for the **Review Analytics and Repository Health** system in the AI Code Review Bot, introduced in V2 Commit 17.

---

## 1. Metrics and Definitions

The analytics system calculates metrics exclusively from actual persisted reviews (`code_reviews`), findings (`code_review_findings`), and linked repositories (`repositories`). It strictly avoids synthetic calculations or arbitrary quality scores.

### 1.1 Review Volume & Execution Metrics
- **Total Reviews (`totalReviews`)**: The total count of review records executed within the requested scope and date window.
- **Completed Reviews (`completedReviews`)**: Count of reviews with `status = 'COMPLETED'`.
- **Failed Reviews (`failedReviews`)**: Count of reviews with `status = 'FAILED'`.
- **In-Progress Reviews (`inProgressReviews`)**: Count of reviews with `status = 'IN_PROGRESS'`.
- **Completion Rate**: `(completedReviews / totalReviews) * 100`, calculated client-side for presentation.
- **Failure Rate**: `(failedReviews / totalReviews) * 100`, calculated client-side for presentation.

### 1.2 Finding Distributions & Intelligence Metrics
- **Total Findings (`totalFindings`)**: Total count of findings recorded across all analyzed code reviews matching the filter criteria.
- **Severity Breakdown (`severityBreakdown`)**: Factual distribution of findings grouped by `ReviewFindingSeverity`:
  - `CRITICAL`
  - `HIGH`
  - `MEDIUM`
  - `LOW`
  - `INFO`
  Empty severities always return an explicit `0` to prevent frontend rendering errors.
- **Category Breakdown (`categoryBreakdown`)**: Factual distribution of findings grouped by `ReviewFindingCategory`:
  - `BUG`
  - `SECURITY`
  - `PERFORMANCE`
  - `CODE_STYLE`
  - `MAINTAINABILITY`
  - `OTHER`
- **Hybrid Source Breakdown (`sourceBreakdown`)**:
  - `AI`: Findings detected through generative LLM semantic reasoning (Gemini).
  - `RULE`: Findings detected through deterministic static code quality rules (`RULE-JAVA-SYSTEM-OUT`, `RULE-JAVA-EMPTY-CATCH`, `RULE-TODO-FIXME`).

### 1.3 Repository Health Status Rules
Repository health describes factual activity and finding counts. It does **not** claim a repository is secure merely because no findings were recorded. The system strictly distinguishes three factual states:

| Health Status | Description | Condition |
| :--- | :--- | :--- |
| `NO_REVIEWS` | *No reviews available* | Repository is registered, but `totalReviews == 0`. |
| `NO_FINDINGS_RECORDED` | *No findings recorded* | `totalReviews > 0` and `totalFindings == 0`. Explicitly represents historical absence of recorded issues, not an absolute security guarantee. |
| `COMPLETED_WITH_FINDINGS` | *Reviews completed with findings* | `totalReviews > 0` and `totalFindings > 0`. |

---

## 2. API Endpoints and Response Shapes

All analytics endpoints are exposed under `/api/v1/analytics` (and `/analytics` for backwards compatibility).

### 2.1 GET `/api/v1/analytics/overview`
Returns high-level review totals, completion metrics, and finding totals.

**Query Parameters:**
- `from` *(optional)*: Start date filter (`YYYY-MM-DD` or ISO-8601 UTC timestamp).
- `to` *(optional)*: End date filter (`YYYY-MM-DD` or ISO-8601 UTC timestamp).
- `repository` *(optional)*: Full repository name (`owner/repo`) or repository name.
- `owner` *(optional)*: Repository owner/organization.

**Response (HTTP 200 OK):**
```json
{
  "totalReviews": 42,
  "completedReviews": 35,
  "failedReviews": 2,
  "inProgressReviews": 5,
  "totalFindings": 128
}
```

### 2.2 GET `/api/v1/analytics/findings`
Returns severity, category, and hybrid engine source breakdowns.

**Response (HTTP 200 OK):**
```json
{
  "totalFindings": 128,
  "severityBreakdown": {
    "CRITICAL": 12,
    "HIGH": 28,
    "MEDIUM": 45,
    "LOW": 31,
    "INFO": 12
  },
  "categoryBreakdown": {
    "BUG": 40,
    "SECURITY": 25,
    "PERFORMANCE": 18,
    "CODE_STYLE": 20,
    "MAINTAINABILITY": 15,
    "OTHER": 10
  },
  "sourceBreakdown": {
    "AI": 110,
    "RULE": 18
  }
}
```

### 2.3 GET `/api/v1/analytics/trends`
Returns daily review and finding activity grouped by calendar date for the requested window. Gaps in activity are populated with explicit `0` values.

**Response (HTTP 200 OK):**
```json
[
  {
    "date": "2026-10-01",
    "totalReviews": 4,
    "completedReviews": 4,
    "failedReviews": 0,
    "inProgressReviews": 0,
    "totalFindings": 14
  },
  {
    "date": "2026-10-02",
    "totalReviews": 0,
    "completedReviews": 0,
    "failedReviews": 0,
    "inProgressReviews": 0,
    "totalFindings": 0
  }
]
```

### 2.4 GET `/api/v1/analytics/repositories`
Returns repository-level aggregations and factual health statuses.

**Response (HTTP 200 OK):**
```json
[
  {
    "repository": "octocat/hello-world",
    "owner": "octocat",
    "name": "hello-world",
    "totalReviews": 15,
    "completedReviews": 14,
    "failedReviews": 1,
    "inProgressReviews": 0,
    "totalFindings": 42,
    "criticalFindings": 3,
    "highFindings": 8,
    "mediumFindings": 15,
    "lowFindings": 10,
    "infoFindings": 6,
    "lastReviewAt": "2026-10-08T14:32:00Z",
    "healthStatus": "COMPLETED_WITH_FINDINGS",
    "healthStatusDescription": "Reviews completed with findings"
  }
]
```

---

## 3. Date Filtering & Timezone Semantics

1. **Timezone Convention**: All timestamps in `code_reviews` and `code_review_findings` are stored as `Instant` in **UTC**.
2. **Boundary Semantics**:
   - `from=YYYY-MM-DD` expands to `YYYY-MM-DDT00:00:00.000000000Z` (inclusive start-of-day UTC).
   - `to=YYYY-MM-DD` expands to `YYYY-MM-DDT23:59:59.999999999Z` (inclusive end-of-day UTC).
   - Full ISO-8601 strings (e.g. `2026-10-01T15:30:00Z`) are also accepted and parsed directly as `Instant`.
3. **Validation**:
   - If `from > to`, the request is rejected with **HTTP 400 Bad Request** (`Invalid date range: 'from' (...) must not be after 'to' (...)`).
   - If a date parameter is malformed, the request is rejected with **HTTP 400 Bad Request**.

---

## 4. Security & Repository Authorization

1. **Authentication Requirement**: All `/api/v1/analytics/**` endpoints require an authenticated user session (`ROLE_USER`, `ROLE_ADMIN`, or `ROLE_DEVELOPER`). Unauthenticated requests are rejected with **HTTP 401 Unauthorized**.
2. **Server-Side Scoping**:
   - Regular users (`USER`, `DEVELOPER`) are automatically and strictly scoped to reviews and repositories associated with their authenticated `user_id`.
   - The user cannot supply a `userId` query parameter to override this decision.
   - If a regular user specifies a repository name belonging to another user, the server queries `WHERE r.user.id = :currentUserId AND r.repository = :requestedRepo`, returning 0 reviews and 0 findings. Private repository existence cannot be inferred.
3. **Admin Access Policy**: Users with `ROLE_ADMIN` can inspect system-wide reviews and repository analytics or filter by repository across all accounts.

---

## 5. Query and Aggregation Strategy

To guarantee scalability and eliminate N+1 queries, all metrics are aggregated directly in PostgreSQL using Spring Data JPA JPQL queries:

```
[HTTP Request]
       │
       ▼
[AnalyticsService]
       ├──> CodeReviewRepository.getOverviewStats() [Single SQL GROUP BY / Aggregation]
       ├──> CodeReviewFindingRepository.countTotalFindings() [Single COUNT query]
       ├──> CodeReviewFindingRepository.countFindingsBySeverity() [Single GROUP BY severity]
       ├──> CodeReviewFindingRepository.countFindingsByCategory() [Single GROUP BY category]
       ├──> CodeReviewFindingRepository.countRuleBasedFindings() [Single COUNT with rule LIKE clause]
       └──> Repository Summaries [2 queries total, O(1) in database]
```

- **No Entity Loading**: Queries use Spring Data JPA projections rather than fetching full entity graphs or text LOB columns.
- **Zero N+1 Queries**: Repository summaries execute at most 2 database queries: 1 for review statistics and 1 for finding severities grouped by repository, merged in memory by repository key.
- **Rule Source Grouping**: Since finding source is classified by rule signatures, the database counts rule matches using SQL string pattern matching (`LIKE '%System.out/err%' OR ...`), and computes AI findings as `totalFindings - ruleFindings`.

---

## 6. Known Limitations

1. **Historical Source Column**: Earlier migrations stored findings without an explicit `source` database column. The analytics engine accurately infers `RULE` findings from registered rule output messages and attributes all remaining findings to `AI`.
2. **Deleted Repositories**: If a repository record is unlinked from the database but past reviews remain, the reviews continue to be aggregated by their historical `owner` and `repository` names.
3. **Date Resolution**: Trends group activity by calendar day in UTC. Users in timezones far from UTC may see review counts grouped according to UTC day boundaries rather than local midnight.
