import React, { useState } from 'react';
import useToast from '../../hooks/useToast';

/**
 * CodeFixPreview displays an AI-generated patch proposal for a finding.
 * Prominently communicates that the fix has NOT been applied.
 * Allows developers to review unified diffs, copy the patch, and mark as reviewed locally.
 */
export const CodeFixPreview = ({
  fix,
  finding,
  onClose,
  onRegenerate,
  isRegenerating = false,
  className = ''
}) => {
  const { toast } = useToast();
  const [copiedPatch, setCopiedPatch] = useState(false);
  const [isReviewed, setIsReviewed] = useState(false);

  if (!fix) return null;

  const isRule = (finding?.source || '').toUpperCase() === 'RULE';

  const handleCopyPatch = async () => {
    if (!fix.unifiedDiff) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(fix.unifiedDiff);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = fix.unifiedDiff;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedPatch(true);
      if (toast && toast.success) {
        toast.success('Unified patch copied to clipboard.', { title: 'Copied' });
      }
      setTimeout(() => setCopiedPatch(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy patch to clipboard.', { title: 'Error' });
      }
    }
  };

  const handleToggleReviewed = () => {
    const nextState = !isReviewed;
    setIsReviewed(nextState);
    if (toast && toast.info) {
      if (nextState) {
        toast.success('Marked proposed patch as reviewed locally.', { title: 'Reviewed' });
      } else {
        toast.info('Unmarked patch review status.', { title: 'Review Status' });
      }
    }
  };

  // Render unified diff lines with explicit symbols and styles
  const renderDiffLines = () => {
    if (!fix.unifiedDiff) {
      return (
        <div className="diff-empty-state">
          <span>No diff content available for preview.</span>
        </div>
      );
    }

    const lines = fix.unifiedDiff.split('\n');
    return lines.map((line, idx) => {
      let lineType = 'context';
      if (line.startsWith('+++') || line.startsWith('---')) {
        lineType = 'header';
      } else if (line.startsWith('@@')) {
        lineType = 'hunk';
      } else if (line.startsWith('+')) {
        lineType = 'addition';
      } else if (line.startsWith('-')) {
        lineType = 'deletion';
      }

      return (
        <div key={idx} className={`diff-line diff-line-${lineType}`}>
          <span className="diff-line-number" aria-hidden="true">{idx + 1}</span>
          <span className="diff-line-indicator" aria-hidden="true">
            {lineType === 'addition' ? '+' : lineType === 'deletion' ? '-' : lineType === 'hunk' ? '@' : ' '}
          </span>
          <span className="diff-line-content">{line}</span>
        </div>
      );
    });
  };

  return (
    <div
      className={`code-fix-preview-container ${className}`}
      role="region"
      aria-label="AI proposed patch preview"
    >
      {/* Safety Warning Banner */}
      <div className="code-fix-warning-banner" role="alert">
        <div className="code-fix-warning-icon-group">
          <svg
            className="code-fix-warning-icon"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <div>
            <strong className="code-fix-warning-title">Proposed fix — not applied</strong>
            <p className="code-fix-warning-subtitle">
              This is an AI-generated proposal. Review it before applying manually.
            </p>
          </div>
        </div>

        {onClose && (
          <button
            type="button"
            className="btn btn-outline btn-sm code-fix-close-btn"
            onClick={onClose}
            aria-label="Close patch preview"
            title="Close patch preview"
          >
            ✕ Close
          </button>
        )}
      </div>

      {/* Provenance & Target File Info */}
      <div className="code-fix-meta-bar">
        <div className="code-fix-meta-item">
          <span className="code-fix-meta-label">Target File:</span>
          <code className="code-fix-meta-path">{fix.filePath || finding?.filePath}</code>
        </div>
        <div className="code-fix-meta-item">
          <span className="code-fix-meta-label">Finding Source:</span>
          <span className="code-fix-meta-value">
            {isRule ? 'Deterministic Rule' : 'AI Review'}
          </span>
        </div>
        <div className="code-fix-meta-item">
          <span className="code-fix-meta-label">Fix Source:</span>
          <span className="code-fix-meta-value code-fix-ai-tag">
            AI-generated proposal ({fix.provider || 'Gemini'})
          </span>
        </div>
        {isReviewed && (
          <div className="code-fix-meta-item">
            <span className="badge badge-success" style={{ backgroundColor: 'var(--status-completed-bg)', color: 'var(--status-completed)', border: '1px solid var(--status-completed)' }}>
              ✓ Reviewed locally
            </span>
          </div>
        )}
      </div>

      {/* AI Explanation Section */}
      {fix.explanation && (
        <div className="code-fix-explanation-box">
          <h5 className="code-fix-section-title">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            Proposed Fix Explanation
          </h5>
          <p className="code-fix-explanation-text">{fix.explanation}</p>
          {fix.limitations && (
            <p className="code-fix-limitations-text">
              <strong>Limitations:</strong> {fix.limitations}
            </p>
          )}
        </div>
      )}

      {/* Unified Diff Viewer */}
      <div className="code-fix-diff-wrapper">
        <div className="code-fix-diff-header">
          <span className="code-fix-diff-title">Unified Diff Preview</span>
          <span className="code-fix-diff-badge">Read-only preview</span>
        </div>
        <div className="code-fix-diff-body" tabIndex={0} role="region" aria-label="Unified diff code block">
          {renderDiffLines()}
        </div>
      </div>

      {/* Developer Action Controls */}
      <div className="code-fix-actions-toolbar" role="toolbar" aria-label="Patch review actions">
        <button
          type="button"
          className="btn btn-sm btn-outline code-fix-btn"
          onClick={handleCopyPatch}
          aria-label="Copy unified patch diff to clipboard"
          title="Copy unified diff to clipboard"
        >
          {copiedPatch ? (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span style={{ color: 'var(--status-completed)' }}>Copied Patch!</span>
            </>
          ) : (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
              <span>Copy Patch</span>
            </>
          )}
        </button>

        <button
          type="button"
          className={`btn btn-sm code-fix-btn ${isReviewed ? 'btn-secondary' : 'btn-outline'}`}
          onClick={handleToggleReviewed}
          aria-label={isReviewed ? 'Unmark reviewed status' : 'Mark proposed patch as reviewed locally'}
        >
          {isReviewed ? '✓ Reviewed locally' : 'Mark as Reviewed'}
        </button>

        {onRegenerate && (
          <button
            type="button"
            className="btn btn-sm btn-outline code-fix-btn"
            onClick={onRegenerate}
            disabled={isRegenerating}
            aria-label="Regenerate proposed patch with AI"
          >
            {isRegenerating ? 'Regenerating...' : '↻ Review Again'}
          </button>
        )}

        {onClose && (
          <button
            type="button"
            className="btn btn-sm btn-secondary code-fix-btn"
            onClick={onClose}
            aria-label="Close diff preview"
          >
            Close Preview
          </button>
        )}
      </div>
    </div>
  );
};

export default CodeFixPreview;
