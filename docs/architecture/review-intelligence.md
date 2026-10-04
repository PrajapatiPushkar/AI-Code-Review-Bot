# Review Intelligence Architecture: Hybrid Code Review

This document describes the architectural foundation for the **Hybrid Code Review** engine in the AI Code Review Bot, introduced in V2 Commit 11.

---

## 1. Why Deterministic Rules Are Being Introduced

Large Language Models (LLMs) excel at high-level contextual reasoning, semantic understanding, and architectural critique. However, relying entirely on LLMs for every code review concern introduces several challenges:
- **Non-deterministic evaluation:** LLMs may occasionally miss simple, syntax-level issues or apply inconsistent criteria across different runs.
- **Latency and Cost:** Running full prompt inference for basic style and hygiene patterns increases review latency and token consumption.
- **Precision:** Specific patterns—such as direct standard console logging (`System.out.println`), unhandled empty catch blocks, or lingering `TODO`/`FIXME` markers—have well-defined, deterministic heuristics. Evaluating them with deterministic code-quality rules guarantees 100% precision, zero token cost, and sub-millisecond execution.

---

## 2. Why AI Remains Necessary

Deterministic rules cannot understand business intent, cross-file relationships, subtle race conditions, complex security vulnerabilities, or semantic correctness. 

AI review remains critical for:
- Contextual logic bugs and edge cases.
- Architectural design patterns and API design soundness.
- Deep security vulnerabilities (e.g., subtle authentication flaws, insecure business flows).
- Idiomatic framework idioms and best-practice refactoring suggestions.

The **Hybrid Architecture** combines deterministic certainty for standard checks with AI contextual reasoning for deep code evaluation.

```
                  Pull Request
                       │
              ┌────────┴────────┐
              │                 │
              ▼                 ▼
     Deterministic Rules       AI Review (Gemini)
              │                 │
              │                 │
              └────────┬────────┘
                       ▼
            Finding Deduplication & Merge
                       │
                       ▼
                Unified Findings
                       │
                       ▼
              Review Persistence & GitHub Comments
```

---

## 3. Rule Abstraction

Deterministic rules implement a clean, lightweight abstraction:

```java
public interface CodeQualityRule {
    String getRuleId();
    String getName();
    String getDescription();
    ReviewFindingCategory getCategory();
    ReviewFindingSeverity getSeverity();
    List<RuleFinding> evaluate(ReviewAnalysisContext context);
}
```

Key characteristics:
- **Independent & Testable:** Each rule is self-contained and unit-testable in isolation without mock servers, GitHub APIs, or network dependencies.
- **Context-Bound:** Evaluates a `ReviewAnalysisContext` containing line-accurate file models (`AnalyzedFile`, `AnalyzedLine`) parsed from git diff hunks or raw source code.
- **Safe:** Rules only flag lines that were modified or added in the pull request (`isAdded = true`), preventing review spam on unchanged legacy code.

---

## 4. Rule Registry and Engine

- **`RuleRegistry`**: A Spring `@Component` that automatically collects all `CodeQualityRule` beans via dependency injection into an unmodifiable lookup map. Supports lookup by unique rule ID and iteration over enabled rules.
- **`DeterministicRuleEngine`**: A Spring `@Service` orchestrating rule evaluation. It iterates over registered rules within a protective try-catch boundary so that an unexpected failure in any single rule does not halt other rules or fail the overall review.

---

## 5. Finding Conversion & Model

Deterministic rules produce `RuleFinding` instances containing:
- `ruleId` (e.g., `RULE-JAVA-SYSTEM-OUT`)
- `filename` (target file path)
- `line` and `endLine` (accurate line boundaries in the modified file)
- `severity` (`ReviewFindingSeverity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, `INFO`)
- `category` (`ReviewFindingCategory`: `CODE_STYLE`, `BUG`, `MAINTAINABILITY`, `SECURITY`, `PERFORMANCE`)
- `message` (clear, concise explanation of the finding)
- `suggestion` (actionable recommendation or code alternative)

Conversion to the existing domain `ReviewFinding` model occurs via `RuleFinding.toReviewFinding()`, which sets the origin `FindingSource.RULE`.

---

## 6. Duplicate & Merge Strategy (`ReviewFindingMerger`)

When both the AI review and a deterministic rule evaluate a PR, they might identify the same issue at the same location.

The `ReviewFindingMerger` applies a deterministic deduplication strategy:
1. **Identity Key**: `normalizedFilePath + ":" + line + ":" + category`.
2. **Precedence**: When an AI finding and a deterministic rule finding collide on the exact same file, line, and category, the **deterministic rule finding takes precedence**. Deterministic rules provide exact pattern explanations and standardized remediations.
3. **Preservation**: All distinct AI findings and distinct rule findings are preserved in the final unified review.

---

## 7. Current Rules (Commit 11)

| Rule ID | Name | Category | Severity | Description |
|---|---|---|---|---|
| `RULE-JAVA-SYSTEM-OUT` | Avoid System.out/err in Production Code | `CODE_STYLE` | `LOW` | Detects `System.out.println`, `System.err.print`, etc. in added Java code, suggesting SLF4J logging instead. |
| `RULE-JAVA-EMPTY-CATCH` | Avoid Empty Catch Blocks | `BUG` | `MEDIUM` | Detects single-line and multi-line empty catch blocks in Java that swallow exceptions without handling or logging. |
| `RULE-TODO-FIXME` | Unresolved TODO or FIXME Marker | `MAINTAINABILITY` | `INFO` | Flags newly introduced `TODO` or `FIXME` comments in code changes across supported languages. |

---

## 8. Future Rule Categories

The modular rule abstraction is designed to easily accommodate future static checks:
1. **Security Rules:** Hardcoded secrets, insecure deserialization, SQL injection patterns in raw queries.
2. **Performance Rules:** Inefficient collection operations, unbuffered I/O streams, missing query limits.
3. **Architecture & Standards:** Disallowed imports (e.g., referencing internal packages across modular boundaries), improper transaction annotations, naming conventions.
4. **Testing Hygiene:** Empty test methods, `@Disabled` tests without explanation, assertions missing failure messages.
