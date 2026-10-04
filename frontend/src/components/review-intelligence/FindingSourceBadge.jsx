import React from 'react';

/**
 * FindingSourceBadge communicates whether a review finding originated from
 * the contextual Gemini AI engine or a deterministic code-quality rule.
 */
export const FindingSourceBadge = ({ source = 'AI', ruleId = null, showRuleId = false, className = '' }) => {
  const isRule = (source || '').toUpperCase() === 'RULE';

  if (isRule) {
    return (
      <span
        className={`finding-source-badge finding-source-rule ${className}`}
        title={`Source: Deterministic Rule${ruleId ? ` (${ruleId})` : ''}`}
        aria-label={`Source: Deterministic Rule${ruleId ? `, rule ID ${ruleId}` : ''}`}
      >
        <svg
          className="finding-source-icon"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <polyline points="16 18 22 12 16 6" />
          <polyline points="8 6 2 12 8 18" />
        </svg>
        <span className="finding-source-label">Deterministic Rule</span>
        {showRuleId && ruleId && (
          <code className="finding-source-rule-id" title={`Rule identifier: ${ruleId}`}>
            {ruleId}
          </code>
        )}
      </span>
    );
  }

  return (
    <span
      className={`finding-source-badge finding-source-ai ${className}`}
      title="Source: AI Review (Gemini contextual reasoning)"
      aria-label="Source: AI Review"
    >
      <svg
        className="finding-source-icon"
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
      </svg>
      <span className="finding-source-label">AI</span>
    </span>
  );
};

export default FindingSourceBadge;
