import React, { useState } from 'react';
import useToast from '../../hooks/useToast';

export const FindingCodeBlock = ({
  suggestion,
  lineNumber,
  endLineNumber,
  filePath
}) => {
  const { toast } = useToast();
  const [copied, setCopied] = useState(false);

  if (!suggestion || !suggestion.trim()) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(suggestion);
      setCopied(true);
      if (toast && toast.success) {
        toast.success('Suggestion copied to clipboard', { title: 'Copied' });
      }
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
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

  return (
    <div className="finding-code-block-container" role="region" aria-label="Suggested code fix">
      <div className="finding-code-header">
        <div className="finding-code-title-group">
          <svg
            className="finding-code-icon"
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
          <span className="finding-code-heading">AI Recommendation & Fix</span>
          {lineContext && (
            <span className="finding-code-target-badge">{lineContext}</span>
          )}
        </div>

        <button
          type="button"
          className="btn btn-outline btn-sm finding-copy-btn"
          onClick={handleCopy}
          title="Copy suggested fix to clipboard"
          aria-label="Copy suggested code fix to clipboard"
        >
          {copied ? (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span style={{ color: 'var(--status-completed)' }}>Copied!</span>
            </>
          ) : (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
              <span>Copy Fix</span>
            </>
          )}
        </button>
      </div>

      <div className="finding-code-wrapper">
        <pre className="finding-code-content">
          <code>{suggestion}</code>
        </pre>
      </div>
    </div>
  );
};

export default FindingCodeBlock;
