import React, { useState } from 'react';

export const ReviewSummary = ({ summary, isInProgress }) => {
  const [copied, setCopied] = useState(false);

  const summaryText = summary ? summary.trim() : '';

  const handleCopy = async () => {
    if (!summaryText) return;
    try {
      await navigator.clipboard.writeText(summaryText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback or ignore clipboard errors safely
    }
  };

  // Safe formatting without dangerouslySetInnerHTML
  // Splits by double newlines into blocks (paragraphs, bullet lists, headings)
  const renderSafeContent = () => {
    if (!summaryText) return null;

    const blocks = summaryText.split(/\n\s*\n/);

    return blocks.map((block, bIdx) => {
      const trimmedBlock = block.trim();
      if (!trimmedBlock) return null;

      // Detect markdown-style headings (# or ##)
      if (trimmedBlock.startsWith('### ')) {
        return (
          <h4 key={bIdx} className="review-summary-subheading">
            {trimmedBlock.replace(/^###\s+/, '')}
          </h4>
        );
      }
      if (trimmedBlock.startsWith('## ') || trimmedBlock.startsWith('# ')) {
        return (
          <h3 key={bIdx} className="review-summary-heading">
            {trimmedBlock.replace(/^#{1,2}\s+/, '')}
          </h3>
        );
      }

      // Detect bullet lists (lines starting with '-' or '*')
      const lines = trimmedBlock.split('\n');
      const isBulletList = lines.every((line) => line.trim().startsWith('- ') || line.trim().startsWith('* '));

      if (isBulletList) {
        return (
          <ul key={bIdx} className="review-summary-list">
            {lines.map((line, lIdx) => (
              <li key={lIdx} className="review-summary-list-item">
                {line.trim().replace(/^[-*]\s+/, '')}
              </li>
            ))}
          </ul>
        );
      }

      // Default paragraph with preserved whitespace
      return (
        <p key={bIdx} className="review-summary-paragraph">
          {trimmedBlock}
        </p>
      );
    });
  };

  return (
    <div className="card review-summary-card">
      <div className="card-header review-summary-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <div className="review-summary-ai-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
            </svg>
          </div>
          <div>
            <h2 className="card-title">AI Review Summary</h2>
          </div>
          <span className="badge badge-info review-summary-badge">Gemini AI</span>
        </div>

        {summaryText && (
          <button
            type="button"
            className="btn btn-outline btn-sm review-summary-copy-btn"
            onClick={handleCopy}
            title="Copy AI summary to clipboard"
            aria-label="Copy AI summary to clipboard"
          >
            {copied ? (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span style={{ color: 'var(--status-completed)' }}>Copied!</span>
              </>
            ) : (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
                <span>Copy Summary</span>
              </>
            )}
          </button>
        )}
      </div>

      <div className="review-summary-body">
        {isInProgress ? (
          <div className="review-summary-in-progress">
            <div className="review-summary-loading-spinner" aria-hidden="true" />
            <div className="review-summary-loading-content">
              <strong style={{ color: 'var(--status-in-progress)', display: 'block', marginBottom: '0.25rem' }}>
                AI Review In Progress
              </strong>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', margin: 0 }}>
                Gemini AI is reviewing the pull request diff for security vulnerabilities, logic bugs, and code smells. The executive summary will appear here once processing completes.
              </p>
            </div>
          </div>
        ) : summaryText ? (
          <div className="review-summary-content">
            {renderSafeContent()}
          </div>
        ) : (
          <div className="review-summary-empty">
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: 0 }}>
              No review summary was generated for this review execution.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReviewSummary;
