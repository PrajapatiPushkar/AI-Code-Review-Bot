# AI Code Review Bot - Complete User Flow & Architecture Guide

Is document me AI Code Review Bot ka complete end-to-end user flow aur code structure simple Hinglish me samjhaya gaya hai.

---

## 1. Dashboard Overview (`DashboardPage.jsx`)

User jab login kar leta hai (`http://localhost:5173/login`), use Dashboard (`/dashboard`) dikhta hai.

### Dashboard Par Kya-Kya Dikhta Hai:
1. **Header & Quick Action Buttons**:
   - `View All History →`: Directs to `/reviews` (All reviews list).
   - `+ Submit New Review`: Directs to `/reviews/new` (Trigger form).
2. **Metrics Summary Grid (5 Cards)**:
   - **Total Reviews**: DB me kul kitne reviews recorded hain.
   - **Completed Reviews**: Successful reviews count (`status = COMPLETED`).
   - **In Progress**: Abhi chal rahe active reviews (`status = IN_PROGRESS`).
   - **Failed Reviews**: Error waale reviews count (`status = FAILED`).
   - **Recent Findings**: Recent reviews ke total AI findings ka sum.
3. **Recent Code Reviews Table**:
   - Top 5 recent reviews dikhaye jaate hain.
   - Har row me: ID, Repository Name, Pull Request #, Status Badge, Findings count, Created Time, Duration, aur Action Buttons (`Details` & `Findings`).

---

## 2. Complete Component-to-Database Mapping

| User Action | Frontend Component | API Service Method | HTTP Method | Backend Endpoint | Controller & Method | Backend Service & Repository |
|---|---|---|---|---|---|---|
| **Load Dashboard Metrics** | `DashboardPage.jsx` | `reviewService.getCodeReviews(...)` | `GET` | `/api/v1/code-reviews` | `CodeReviewHistoryController.getCodeReviews` | `CodeReviewHistoryService` → `CodeReviewRepository.findAll()` |
| **Submit New Review** | `SubmitReviewPage.jsx` | `reviewService.submitPullRequestReview(...)` | `POST` | `/api/v1/code-reviews/pull-request` | `CodeReviewController.reviewPullRequest` | `GithubPullRequestCodeReviewService` → `CodeReviewRepository.save()` |
| **View Review Details** | `ReviewDetailsPage.jsx` | `reviewService.getReviewById(id)` | `GET` | `/api/v1/code-reviews/{id}` | `CodeReviewHistoryController.getById` | `CodeReviewHistoryService` → `CodeReviewRepository.findById()` |
| **Poll Review Status** | `ReviewDetailsPage.jsx` | `reviewService.getReviewStatus(id)` | `GET` | `/api/v1/code-reviews/{id}/status` | `CodeReviewHistoryController.getStatusById` | `CodeReviewHistoryService` → `CodeReviewRepository.findById()` |
| **Fetch Review Result** | `ReviewDetailsPage.jsx` | `reviewService.getReviewResult(id)` | `GET` | `/api/v1/code-reviews/{id}/result` | `CodeReviewHistoryController.getResultById` | `CodeReviewHistoryService` → `CodeReviewRepository.findById()` |
| **View Review Findings** | `ReviewFindingsPage.jsx` | `reviewService.getReviewFindings(id, ...)` | `GET` | `/api/v1/code-reviews/{id}/findings` | `CodeReviewHistoryController.getFindingsByReviewId` | `CodeReviewHistoryService` → `CodeReviewFindingRepository.findByCodeReviewId()` |
| **Filter Review History** | `ReviewsPage.jsx` | `reviewService.getCodeReviews(params)` | `GET` | `/api/v1/code-reviews` | `CodeReviewHistoryController.getCodeReviews` | `CodeReviewHistoryService` → `CodeReviewSpecification` → `CodeReviewRepository` |

---

## 3. Code Review Lifecycle & Status States

Backend enum `CodeReviewStatus` (`com.pushkar.codereview.github.review.persistence.CodeReviewStatus`) me bilkul 3 states hoti hain:

1. **`IN_PROGRESS`**: PR review submit hone par initial state. Background `@Async` task code diff fetch aur AI analysis chala raha hota hai.
2. **`COMPLETED`**: AI review successful hone par terminal state. Code findings aur review summary DB me save ho chuki hoti hain.
3. **`FAILED`**: Execution me error aane par (e.g. GitHub API failure, Gemini API failure, bad credentials) terminal state.

---

## 4. Code Review Submission Payload

User `/reviews/new` par jaakar form fill karta hai:

```json
{
  "installationId": 100,
  "owner": "octocat",
  "repository": "hello-world",
  "pullRequestNumber": 42,
  "commitSha": "6dcb09b5a3f12345"
}
```

- `installationId`: GitHub App ka numeric installation ID.
- `owner`: Repository owner / organization.
- `repository`: Repository ka naam.
- `pullRequestNumber`: GitHub Pull Request number.
- `commitSha`: (Optional) Specific commit SHA.

---

## 5. Frontend Real-time Polling Mechanism

Jab user submission ke baad `/reviews/{id}` page par jata hai:

1. **Trigger Condition**: Aggar `review.status === 'IN_PROGRESS'` hai, toh real-time polling auto-start hoti hai.
2. **Polling Interval**: Har **2.5 seconds (2500 ms)** me frontend background me `GET /api/v1/code-reviews/{id}/status` call karta hai.
3. **Polling Stop Conditions**:
   - Status change hoker `COMPLETED` ya `FAILED` ho jaye.
   - Safety timeout reach ho jaye (60 seconds / 24 polling attempts ke baad automatic stop).
   - Component unmount ho jaye (user kisi doosre page par chala jaye).
4. **Completion Handling**: Jaise hi status `COMPLETED` milta hai, frontend turant `GET /api/v1/code-reviews/{id}/result` call karke poora summary load kar leta hai aur "View Findings" button enable kar deta hai.

---

## 6. Findings & History Inspection

### Findings Page (`/reviews/{id}/findings`):
- AI dwara detect kiye gaye saare bugs aur suggestions list hotey hain.
- **Severity Filters**: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`.
- **Finding Card Details**: File Path, Line Numbers, Category, Severity Badge, Message, aur Code Fix Suggestion diff.

### History Page (`/reviews`):
- Paginated table showing all past code reviews.
- Filtering support: Status (ALL, COMPLETED, IN_PROGRESS, FAILED), Owner, Repository, PR #, aur Sorting (Newest, Oldest, Most Findings).

---

## 7. Database Tables Involved

1. `users`: System users and authentication metadata.
2. `code_reviews`: Primary record of each PR review (`installation_id`, `owner`, `repository`, `pull_request_number`, `status`, `review_summary`, `total_findings`, `created_at`, `completed_at`).
3. `code_review_findings`: Line-by-line AI findings (`code_review_id`, `file_path`, `line_number`, `end_line_number`, `severity`, `category`, `message`, `suggestion`).
4. `github_installations`: GitHub App installation mapping.
5. `repositories`: Tracked repository details.

---

## 8. Complete Application End-to-End Flow (In Simple Hinglish)

```
[ User Browser ]
       │
       ▼ (1) Login at /login with devuser/password123
[ AuthController ] ──▶ Returns JWT Token
       │
       ▼ (2) Stored in localStorage & redirected to /dashboard
[ Dashboard Page ] ──▶ Shows Metrics & Recent Reviews
       │
       ▼ (3) Click "+ Submit New Review" (/reviews/new)
[ SubmitReviewPage ] ──▶ Submits PR details (installationId, owner, repo, pr#)
       │
       ▼ (4) POST /api/v1/code-reviews/pull-request
[ CodeReviewController ]
       │
       ├─▶ Inserts record in `code_reviews` table (status = IN_PROGRESS)
       ├─▶ Triggers @Async background execution
       └─▶ Returns HTTP 202 Accepted to Frontend
       │
       ▼ (5) User redirected to /reviews/{id}
[ ReviewDetailsPage ] ──▶ Starts Polling (GET /api/v1/code-reviews/{id}/status every 2.5s)
       │
       ▼ (Background Async Task Execution)
┌─────────────────────────────────────────────────────────────┐
│ 1. GitHub API Call: Fetch Pull Request diff & changed files │
│ 2. Code Extraction: Parse modified files and line ranges   │
│ 3. Gemini AI API Call: Send diff prompt for AI analysis    │
│ 4. Response Parsing: Extract summary & line-by-line findings│
│ 5. DB Persistence:                                          │
│    - Save findings in `code_review_findings` table          │
│    - Update status to `COMPLETED` in `code_reviews` table   │
└─────────────────────────────────────────────────────────────┘
       │
       ▼ (6) Polling receives status = COMPLETED
[ ReviewDetailsPage ] ──▶ Stops Polling & fetches /result
       │
       ▼ (7) User clicks "View Findings" (/reviews/{id}/findings)
[ ReviewFindingsPage ] ──▶ Renders AI Findings, Security Suggestions & Diff Fixes
```
