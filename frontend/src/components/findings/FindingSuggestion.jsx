import React, { useState } from 'react';
import useToast from '../../hooks/useToast';

/**
 * FindingSuggestion displays the recommended code fix for an issue.
 * Supports syntax formatting, line numbering context, and accessible copy functionality.
 * If no suggestion is provided, displays an informative fallback notice.
 */
export const FindingSuggestion = ({
  suggestion,
  lineNumber,
  endLineNumber,
  onCopySuccess,
  className = ''
}) => {
  const { toast } = useToast();
  const [copied, setCopied] = useState(false);

  const hasSuggestion = Boolean(suggestion && suggestion.trim().length > 0);

  const handleCopy = async () => {
    if (!hasSuggestion) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(suggestion);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = suggestion;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopied(true);
      if (toast && toast.success) {
        toast.success('Fix suggestion copied to clipboard.', { title: 'Copied' });
      }
      if (onCopySuccess) {
        onCopySuccess();
      }
      setTimeout(() => setCopied(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy suggestion to clipboard.', { title: 'Error' });
      }
    }
  };

  const getLineContext = () => {
    if (lineNumber && endLineNumber && endLineNumber > lineNumber) {
      return `Target Lines ${lineNumber}–${endLineNumber}`;
    }
    if (lineNumber) {
      return `Target Line ${lineNumber}`;
    }
    return null;
  };

  const lineContext = getLineContext();

  if (!hasSuggestion) {
    return (
      <div
        className={`finding-suggestion-container finding-suggestion-empty-state ${className}`}
        role="region"
        aria-label="Suggested code fix"
      >
        <div className="finding-suggestion-header">
          <div className="finding-suggestion-title-group">
            <svg
              className="finding-suggestion-icon"
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span className="finding-suggestion-heading">Suggested Fix</span>
          </div>
        </div>
        <div className="finding-suggestion-empty" role="note">
          <p className="finding-suggestion-empty-text">
            No fix suggestion was provided for this finding.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`finding-suggestion-container ${className}`}
      role="region"
      aria-label="Suggested code fix"
    >
      <div className="finding-suggestion-header">
        <div className="finding-suggestion-title-group">
          <svg
            className="finding-suggestion-icon"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5" />
            <path d="M9 18h6" />
            <path d="M10 22h4" />
          </svg>
          <span className="finding-suggestion-heading">Suggested Fix</span>
          {lineContext && (
            <span className="finding-suggestion-target-badge">{lineContext}</span>
          )}
        </div>

        <button
          type="button"
          className="btn btn-outline btn-sm finding-suggestion-copy-btn"
          onClick={handleCopy}
          title="Copy suggested fix to clipboard"
          aria-label="Copy fix suggestion to clipboard"
        >
          {copied ? (
            <>
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="var(--status-completed)"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span style={{ color: 'var(--status-completed)' }}>Copied!</span>
            </>
          ) : (
            <>
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
              <span>Copy Suggestion</span>
            </>
          )}
        </button>
      </div>

      <div className="finding-suggestion-content-wrapper">
        <pre className="finding-suggestion-content">
          <code>{suggestion}</code>
        </pre>
      </div>
    </div>
  );
};

export default FindingSuggestion;
