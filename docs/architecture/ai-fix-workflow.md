# AI Fix Proposal Workflow Architecture

## 1. Overview & Goals

The AI Fix Proposal subsystem generates context-aware patches for code review findings, validates candidate patches for safety and syntactical integrity, persists patch artifacts to PostgreSQL, and manages a safe developer-in-the-loop lifecycle.

Crucially, this workflow is **strictly read-only with respect to GitHub**: it never automatically modifies pull requests, commits, pushes branches, or posts unsolicited external comments.

```
+-------------------------------------------------------------------------------+
|                             Developer Request                                 |
|            POST /api/v1/code-reviews/findings/{findingId}/fix                 |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                             CodeFixService                                    |
|   1. Verify Finding & CodeReview ownership (User / ADMIN)                    |
|   2. Fetch PR patch context via GithubPullRequestReviewService                |
|   3. Build structured prompt (FixPromptBuilder)                               |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                            GeminiAiFixEngine                                  |
|   - Call Google Gemini LLM API with structured schema                         |
|   - Fallback model & ResilienceExecutor retry / circuit-breaker               |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                         FixResponseParser & Validator                         |
|   - Strip markdown code blocks & sanitize raw JSON                            |
|   - PatchValidator: path traversal guard, unified diff check, target match    |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                         Persistence & History                                 |
|   - Persist CodeFixProposal entity in PostgreSQL (`code_fix_proposals`)       |
|   - Assign initial status: PROPOSED                                           |
|   - Return CodeFixResponse with proposalId                                    |
+-------------------------------------------------------------------------------+
```

---

## 2. Validation & Security Guardrails

Patch generation is subject to multiple strict safety checks before any artifact is accepted or persisted:

1. **Finding & Review Ownership**: Enforced before external LLM calls or database lookups. Requests for findings belonging to another user are rejected with `403 Forbidden`.
2. **Developer Instructions Constraint**: Optional guidance is capped at 500 characters and stripped of dangerous injection sequences.
3. **Target File Path Validation**: 
   - Rejects relative path traversal attempts (e.g. `../`, `..\\`).
   - Rejects absolute system paths (e.g. `/etc/passwd`, `C:\Windows`).
   - Ensures the proposed patch file path matches the finding target file.
4. **Unified Diff Format Check**: Must include valid Git hunk headers (`@@ -start,len +start,len @@`) or valid diff headers (`--- a/...`, `+++ b/...`).

---

## 3. Database Persistence

Fix proposals are persisted in PostgreSQL in the `code_fix_proposals` table (migration `V10__create_code_fix_proposals_table.sql`).

### Schema Definition

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY | Unique proposal ID |
| `finding_id` | `BIGINT` | NOT NULL, FK, INDEX | Associated finding ID |
| `file_path` | `VARCHAR(500)` | NOT NULL | Target file path |
| `explanation` | `TEXT` | | Human-readable reasoning for the fix |
| `unified_diff` | `TEXT` | NOT NULL | Unified diff format patch |
| `original_content` | `TEXT` | | Original code snippet |
| `proposed_content` | `TEXT` | | Proposed replacement snippet |
| `provider` | `VARCHAR(50)` | | LLM provider (`Gemini`) |
| `model` | `VARCHAR(100)` | | Model used (e.g., `gemini-3.6-flash`) |
| `developer_instructions`| `VARCHAR(500)` | | Optional guidance from developer |
| `status` | `VARCHAR(50)` | NOT NULL | Current lifecycle status |
| `created_at` | `TIMESTAMP` | NOT NULL | Creation timestamp |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last update timestamp |

---

## 4. Proposal Lifecycle & State Machine

Proposals follow an explicit, finite state machine:

```
                  +-------------------+
                  |     PROPOSED      |  (Initial state)
                  +-------------------+
                    /       |       \
                   /        |        \
    [Mark Reviewed]   [Reject]    [Mark Expired]
                 v          v          v
          +----------+  +----------+  +----------+
          | REVIEWED |  | REJECTED |  | EXPIRED  |
          +----------+  +----------+  +----------+
                |         (Final)       (Final)
          [Mark Expired]
                |
                v
          +----------+
          | EXPIRED  |
          +----------+
            (Final)
```

### Allowed Status Transitions

- `PROPOSED -> REVIEWED`: Developer inspected the patch and verified its approach.
- `PROPOSED -> REJECTED`: Developer rejected the proposed fix.
- `PROPOSED -> EXPIRED`: Proposal became stale due to newer commits or code changes.
- `REVIEWED -> EXPIRED`: A reviewed patch was superseded or no longer needed.

### Disallowed Transitions

- `REJECTED -> *`: Terminal state. Cannot be changed to `REVIEWED` or `PROPOSED`.
- `EXPIRED -> *`: Terminal state. Cannot be unexpired.
- Self-transitions and undefined status hops return `400 Bad Request`.

---

## 5. REST API Endpoints

All endpoints require authentication (`ROLE_USER`, `ROLE_ADMIN`, or `ROLE_DEVELOPER`).

### Generate Fix
- **Endpoint**: `POST /api/v1/code-reviews/findings/{findingId}/fix`
- **Body**: `{ "instructions": "Optional guidance" }`
- **Response**: `200 OK` with `CodeFixResponse` including `proposalId`.

### List Proposals for Finding
- **Endpoint**: `GET /api/v1/code-reviews/findings/{findingId}/fixes`
- **Response**: `200 OK` with list of `CodeFixProposalResponse` (sorted newest first).

### Get Single Proposal
- **Endpoint**: `GET /api/v1/code-reviews/fixes/{proposalId}`
- **Response**: `200 OK` with `CodeFixProposalResponse`.

### Update Proposal Status
- **Endpoint**: `PATCH /api/v1/code-reviews/fixes/{proposalId}/status`
- **Body**: `{ "status": "REVIEWED" | "REJECTED" | "EXPIRED" }`
- **Response**: `200 OK` with updated `CodeFixProposalResponse` (or `400 Bad Request` if invalid transition).

---

## 6. Authorization Model

Ownership is strictly validated server-side by tracing:
$$\text{CodeFixProposal} \longrightarrow \text{CodeReviewFinding} \longrightarrow \text{CodeReview} \longrightarrow \text{User}$$

- The authenticated user must own the `CodeReview` entity associated with the finding.
- Users with role `ADMIN` have global administrative read/update access.
- Non-owners receive `403 Forbidden` if attempting to query or mutate another user's proposal by guessing IDs.

---

## 7. Why Automatic GitHub Application is Intentionally Not Implemented

1. **Safety First**: AI-generated code fixes can introduce subtle regressions, breaking changes, or syntax errors. Unattended commits or pushes undermine code stability.
2. **Review Integrity**: Pull requests represent author accountability. Silently modifying pull request branches from a bot creates race conditions and unexpected merge conflicts with active local developer branches.
3. **Explicit Developer Choice**: Developers should review the diff, copy the patch, and test it locally using standard Git workflows before committing.

---

## 8. Future Extension Points

- **GitHub Suggested Changes Comments**: Future versions can optionally format reviewed proposals as GitHub suggestion markdown blocks (` ```suggestion `) in PR review comments upon explicit developer command.
- **Automated Pull Request Branch Creation**: In managed environments with explicit opt-in, creating isolated preview branches (e.g. `ai-fix/finding-123`) for CI validation prior to developer review.
