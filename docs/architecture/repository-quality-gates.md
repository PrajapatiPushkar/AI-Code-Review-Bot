# Repository Quality Gates & Review Policy Architecture

This document describes the architectural design, database persistence, evaluation rules, and operational workflow for **Repository Review Policies and Quality Gates** in the AI Code Review Bot (Commit 18, V2 Platform).

---

## 1. Overview & Objectives

Repository Quality Gates introduce deterministic, enforceable code review standards at the repository level. Review policies allow repository owners to:
- Enable or disable quality gate evaluation.
- Define a severity threshold at or above which findings trigger a quality gate failure.
- Toggle individual deterministic static analysis rules from the engine's supported rule registry.
- Maintain immutable, reproducible historical records of quality gate evaluations per code review without corrupting the core review lifecycle.

The system strictly operates on actual persisted review findings and metadata. It does not invent synthetic scores or claim that a passing review guarantees defect-free or secure software.

---

## 2. Policy Schema & Persistence

### 2.1 Database Schema (`repository_review_policies`)

Added via Flyway migration `V11__create_repository_review_policies_and_quality_gates.sql`:

```sql
CREATE TABLE repository_review_policies (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fail_on_severity VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    enabled_rule_ids VARCHAR(1024) NOT NULL DEFAULT 'RULE-JAVA-SYSTEM-OUT,RULE-JAVA-EMPTY-CATCH,RULE-TODO-FIXME',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_repository_review_policies_repo UNIQUE (repository_id),
    CONSTRAINT fk_repository_review_policies_repo FOREIGN KEY (repository_id)
        REFERENCES repositories(id) ON DELETE CASCADE
);

CREATE INDEX idx_repo_review_policies_repo ON repository_review_policies(repository_id);
```

### 2.2 Default Policy Behavior

If a repository does not have an explicit custom policy persisted in `repository_review_policies`, the system applies a deterministic default policy:
- **`enabled`**: `true` (Quality gate evaluation is active).
- **`failOnSeverity`**: `HIGH` (Any finding with severity `HIGH` or `CRITICAL` fails the gate).
- **`enabledRuleIds`**: All rules currently supported by the deterministic rule engine (`RULE-JAVA-SYSTEM-OUT`, `RULE-JAVA-EMPTY-CATCH`, `RULE-TODO-FIXME`).
- **`isCustom`**: `false` in API responses (`true` when an owner-defined policy row exists).

A missing policy row **never** disables code reviews or causes evaluation errors.

---

## 3. Supported Deterministic Rules

The review engine evaluates deterministic rules independently of LLM reasoning. Rules can only be toggled if registered in `RuleRegistry`.

| Rule ID | Name | Description | Default State |
| :--- | :--- | :--- | :--- |
| `RULE-JAVA-SYSTEM-OUT` | Java System.out / err Usage | Flags standard output or error stream usage instead of structured logging frameworks | Enabled |
| `RULE-JAVA-EMPTY-CATCH` | Java Empty Catch Block | Flags empty `catch` blocks that silently swallow exceptions without handling or rethrowing | Enabled |
| `RULE-TODO-FIXME` | Unresolved TODO/FIXME Comments | Flags remaining `TODO` and `FIXME` comments left in production code | Enabled |

### 3.1 Validation & Security Rules
- **No arbitrary rule execution**: Rule IDs sent in API requests are strictly validated against `RuleRegistry.isSupportedRuleId(id)`. Unknown IDs are rejected with `HTTP 400 Bad Request`.
- **No shell/code execution**: Rule IDs are identifiers matched against pre-compiled Java visitors, never executed as external scripts or commands.
- **Empty rule set semantics**: If `enabledRuleIds` is empty, deterministic rule execution is skipped for that repository, while generative AI review analysis continues normally.

---

## 4. Quality Gate Evaluation

### 4.1 Status Definitions

| Quality Gate Status | Condition | Description |
| :--- | :--- | :--- |
| `NOT_EVALUATED` | Gate disabled (`policy.enabled == false`) OR Review not completed (`status != COMPLETED`) | The quality gate was not evaluated because it was turned off or the review has not completed cleanly. In-progress and failed reviews are always reported as `NOT_EVALUATED`. |
| `FAIL` | Review is `COMPLETED` and one or more findings have severity $\ge$ `failOnSeverity` | One or more findings met or exceeded the failure threshold. |
| `PASS` | Review is `COMPLETED`, gate is enabled, and zero findings have severity $\ge$ `failOnSeverity` | All findings (if any) are strictly below the configured failure threshold. |

### 4.2 Severity Threshold Ordering

The severity ordering conforms to `ReviewFindingSeverity`:

$$\text{INFO} < \text{LOW} < \text{MEDIUM} < \text{HIGH} < \text{CRITICAL}$$

When `failOnSeverity = HIGH`:
- `CRITICAL` and `HIGH` findings cause a `FAIL`.
- `MEDIUM`, `LOW`, and `INFO` findings do not cause a `FAIL`.

When `failOnSeverity = CRITICAL`:
- Only `CRITICAL` findings cause a `FAIL`.

When `failOnSeverity = LOW`:
- `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL` findings cause a `FAIL`.

### 4.3 Evaluation Timing & Lifecycle Safety

1. **Async Pipeline Integration**: In `AsyncCodeReviewRunner`, evaluation occurs immediately after all findings (AI + deterministic) have been persisted and the review transitions to `COMPLETED`.
2. **Failure Isolation**: An unexpected error during quality gate evaluation is logged, but does **not** fail or roll back the completed code review.
3. **Idempotence & Duplicate Prevention**: The result is saved to `code_review_quality_gates`. Repeated calls or status polling do not recalculate or duplicate rows.
4. **Historical Immutability**: The evaluation snapshot retains the exact `failOnSeverity`, `failureCount`, and explanation evaluated at completion time, guaranteeing reproducible historical review audit trails even if repository policy changes subsequently.

---

## 5. Quality Gate Persistence (`code_review_quality_gates`)

```sql
CREATE TABLE code_review_quality_gates (
    id BIGSERIAL PRIMARY KEY,
    code_review_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    gate_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fail_on_severity VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    failure_count INT NOT NULL DEFAULT 0,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(1024),
    CONSTRAINT uk_code_review_quality_gates_review UNIQUE (code_review_id),
    CONSTRAINT fk_code_review_quality_gates_review FOREIGN KEY (code_review_id)
        REFERENCES code_reviews(id) ON DELETE CASCADE
);

CREATE INDEX idx_quality_gates_review ON code_review_quality_gates(code_review_id);
```

---

## 6. API Endpoints

All endpoints are secured with Spring Security JWT authentication.

### 6.1 Policy Endpoints

#### `GET /api/v1/repositories/{repositoryId}/review-policy`
Returns the current effective policy for the repository. If no custom policy is saved, returns the default policy with `isCustom: false`.

#### `PUT /api/v1/repositories/{repositoryId}/review-policy`
Creates or updates the repository policy.

**Request Body:**
```json
{
  "enabled": true,
  "failOnSeverity": "HIGH",
  "enabledRuleIds": [
    "RULE-JAVA-SYSTEM-OUT",
    "RULE-JAVA-EMPTY-CATCH",
    "RULE-TODO-FIXME"
  ]
}
```

**Validation:**
- `failOnSeverity` must be a valid `ReviewFindingSeverity`.
- `enabledRuleIds` must only contain registered rule identifiers.

#### `POST /api/v1/repositories/{repositoryId}/review-policy/reset`
Resets the repository policy to the default configuration. Deletes any custom override.

### 6.2 Quality Gate Endpoints

#### `GET /api/v1/code-reviews/{reviewId}/quality-gate`
Returns the quality gate evaluation for the specified code review.

**Response Body:**
```json
{
  "codeReviewId": 42,
  "status": "FAIL",
  "enabled": true,
  "failOnSeverity": "HIGH",
  "failureCount": 3,
  "evaluatedAt": "2026-10-10T06:45:00Z",
  "reason": "Quality gate failed with 3 finding(s) at or above HIGH severity."
}
```

---

## 7. Authorization & Cross-User Privacy

- **Repository Resolution & Ownership**: Repositories are linked to an `owner` (User). When fetching or updating policies, the service verifies that the authenticated user matches the repository owner.
- **No Existence Leaks**: If an authenticated user queries a repository that does not belong to them, the system returns `404 Not Found` (via `ResourceNotFoundException`), preventing enumeration of private repositories.
- **Review Access Delegation**: Quality gate retrieval follows the ownership chain: `CodeReview -> Repository -> Owner`. Unauthorized users receive `404 Not Found`.

---

## 8. Frontend Experience

1. **Repository Review Policy Modal**:
   - Accessible via the **Review Policy** button on repository cards in the Repositories workspace.
   - Allows toggling quality gate evaluation on/off.
   - Allows selecting the failure severity threshold (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`).
   - Allows toggling individual deterministic rules with descriptions and rule identifiers.
   - Provides **Save Changes** and **Reset to Default** actions with full loading, error, and dirty state tracking.
2. **Review Details Quality Gate Card**:
   - Rendered prominently above findings on the Review Details page.
   - Visual badges for `PASS`, `FAIL`, and `NOT EVALUATED`.
   - Clear display of failure count, configured threshold, and factual explanation.
   - Explicit disclaimer noting that passing quality gates indicates adherence to configured thresholds and does not guarantee defect-free code.

---

## 9. Known Limitations

- **Rule Language Scope**: Deterministic rules currently target Java source files (`.java`). Multi-language deterministic rules (e.g., Python, TypeScript) can be added to `RuleRegistry` in future commits without changing the policy schema.
- **Branch-Specific Policies**: Policies are currently configured per repository. Branch-specific or PR-tag-specific override rules are reserved for future platform iterations.
- **Synchronous Post-Review Gate**: The quality gate evaluation is persisted immediately upon completion of the async review pipeline; live policy updates after a review has completed do not retroactively alter previously evaluated review records.
