import React from 'react';

export const PullRequestInput = ({
  pullRequestNumber = '',
  onChangePullRequestNumber,
  commitSha = '',
  onChangeCommitSha,
  errors = {},
  disabled = false
}) => {
  return (
    <div className="submit-form-card" aria-labelledby="pr-details-heading">
      <div className="submit-card-section-title" id="pr-details-heading">
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
          <circle cx="18" cy="18" r="3" />
          <circle cx="6" cy="6" r="3" />
          <path d="M13 6h3a2 2 0 0 1 2 2v7" />
          <line x1="6" y1="9" x2="6" y2="21" />
        </svg>
        <span>Pull Request Details</span>
      </div>
      <p className="submit-card-section-subtitle">
        Specify the pull request number and optional commit SHA for targeted AI analysis.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem' }}>
        {/* Pull Request Number */}
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label" htmlFor="pull-request-number-input">
            Pull Request Number
            <span className="input-required-star" aria-hidden="true">*</span>
          </label>
          <input
            id="pull-request-number-input"
            type="number"
            min="1"
            step="1"
            className={`form-input ${errors.pullRequestNumber ? 'form-input-error' : ''}`}
            placeholder="e.g. 42"
            value={pullRequestNumber}
            onChange={(e) => onChangePullRequestNumber(e.target.value)}
            disabled={disabled}
            required
            aria-describedby={errors.pullRequestNumber ? 'pr-number-error' : 'pr-number-helper'}
            aria-invalid={Boolean(errors.pullRequestNumber)}
          />
          {errors.pullRequestNumber ? (
            <div id="pr-number-error" className="input-error-msg" role="alert">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{errors.pullRequestNumber}</span>
            </div>
          ) : (
            <p id="pr-number-helper" className="input-helper-note">
              Positive integer corresponding to the open GitHub PR.
            </p>
          )}
        </div>

        {/* Commit SHA */}
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label" htmlFor="commit-sha-input">
            Commit SHA
            <span className="input-optional-tag">(Optional)</span>
          </label>
          <input
            id="commit-sha-input"
            type="text"
            className={`form-input ${errors.commitSha ? 'form-input-error' : ''}`}
            placeholder="e.g. 6dcb09b57..."
            value={commitSha}
            onChange={(e) => onChangeCommitSha(e.target.value)}
            disabled={disabled}
            maxLength={64}
            aria-describedby={errors.commitSha ? 'commit-sha-error' : 'commit-sha-helper'}
            aria-invalid={Boolean(errors.commitSha)}
          />
          {errors.commitSha ? (
            <div id="commit-sha-error" className="input-error-msg" role="alert">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{errors.commitSha}</span>
            </div>
          ) : (
            <p id="commit-sha-helper" className="input-helper-note">
              Optional. Leave blank to use the pull request's current revision if supported by the backend.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default PullRequestInput;
