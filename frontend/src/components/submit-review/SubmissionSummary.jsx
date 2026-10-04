import React from 'react';

export const SubmissionSummary = ({
  owner = '',
  repository = '',
  pullRequestNumber = '',
  installationId = '',
  installationAccount = '',
  commitSha = ''
}) => {
  const repoDisplay = owner && repository ? `${owner} / ${repository}` : repository || owner || '—';
  const prDisplay = pullRequestNumber ? `#${pullRequestNumber}` : '—';
  const installationDisplay = installationAccount
    ? `${installationAccount} (ID: ${installationId || '—'})`
    : installationId
    ? `Installation #${installationId}`
    : '—';
  const commitDisplay = commitSha.trim() ? commitSha.trim() : 'Latest PR revision (HEAD)';

  return (
    <div className="submission-summary-card" aria-labelledby="summary-heading">
      <div className="summary-card-header">
        <svg
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
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
          <polyline points="10 9 9 9 8 9" />
        </svg>
        <h2 id="summary-heading" className="summary-card-title">
          Review Target Summary
        </h2>
      </div>

      <div className="summary-table">
        <div className="summary-table-row">
          <span className="summary-label">Target Repository</span>
          <span className="summary-value" title={repoDisplay}>
            {repoDisplay !== '—' ? repoDisplay : <span className="summary-value-empty">Not specified</span>}
          </span>
        </div>

        <div className="summary-table-row">
          <span className="summary-label">Pull Request</span>
          <span className="summary-value">
            {prDisplay !== '—' ? (
              <span className="summary-value-code">{prDisplay}</span>
            ) : (
              <span className="summary-value-empty">Not specified</span>
            )}
          </span>
        </div>

        <div className="summary-table-row">
          <span className="summary-label">GitHub Installation</span>
          <span className="summary-value">
            {installationDisplay !== '—' ? installationDisplay : <span className="summary-value-empty">Not specified</span>}
          </span>
        </div>

        <div className="summary-table-row">
          <span className="summary-label">Commit SHA</span>
          <span className="summary-value">
            {commitSha.trim() ? (
              <span className="summary-value-code" title={commitSha.trim()}>
                {commitSha.trim().length > 10 ? `${commitSha.trim().substring(0, 10)}...` : commitSha.trim()}
              </span>
            ) : (
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                Latest revision (HEAD)
              </span>
            )}
          </span>
        </div>
      </div>

      {/* Async review lifecycle notice */}
      <div className="async-review-banner" role="note">
        <div className="async-review-icon" aria-hidden="true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
        <div>
          <div className="async-review-title">Asynchronous Execution</div>
          <p className="async-review-text">
            Reviews execute asynchronously in the background. Once submitted, you'll be redirected to live review tracking where findings appear as they are discovered.
          </p>
        </div>
      </div>
    </div>
  );
};

export default SubmissionSummary;
