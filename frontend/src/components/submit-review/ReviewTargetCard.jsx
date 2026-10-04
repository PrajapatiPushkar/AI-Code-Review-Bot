import React from 'react';

export const ReviewTargetCard = ({
  owner = '',
  repository = '',
  installationId = '',
  installationAccount = '',
  onChangeTarget,
  isLocked = false
}) => {
  if (!owner && !repository) return null;

  const fullName = owner && repository ? `${owner}/${repository}` : repository || owner;

  return (
    <section className="review-target-card highlight-target" aria-labelledby="target-card-heading">
      <div className="target-card-header">
        <span id="target-card-heading" className="target-card-title">
          Selected Target Repository
        </span>
        <span className="target-card-badge">
          <svg
            width="10"
            height="10"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
          Target Confirmed
        </span>
      </div>

      <div className="target-repo-info">
        <div className="target-repo-icon" aria-hidden="true">
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
          </svg>
        </div>

        <div className="target-repo-details">
          <h2 className="target-repo-name" title={fullName}>
            {fullName}
          </h2>
          <div className="target-repo-meta">
            {installationAccount && (
              <span className="target-repo-meta-item">
                <strong>Account:</strong> {installationAccount}
              </span>
            )}
            {installationId && (
              <span className="target-repo-meta-item">
                <strong>Installation ID:</strong> {installationId}
              </span>
            )}
          </div>
        </div>
      </div>

      {onChangeTarget && (
        <div className="target-card-actions">
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={onChangeTarget}
            aria-label="Change target repository or installation"
          >
            <svg
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
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
            <span>Change Target</span>
          </button>
        </div>
      )}
    </section>
  );
};

export default ReviewTargetCard;
