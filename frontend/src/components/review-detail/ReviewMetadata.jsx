import React from 'react';
import StatusBadge from '../common/StatusBadge';

export const ReviewMetadata = ({ review, formatDuration, githubPrUrl }) => {
  const reviewId = review.id || review.codeReviewId;
  const repoName = review.repository || review.repositoryName || 'N/A';
  const fullRepo = review.owner ? `${review.owner}/${repoName}` : repoName;
  const isInProgress = review.status === 'IN_PROGRESS';

  const createdFormatted = review.createdAt
    ? new Date(review.createdAt).toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'medium'
      })
    : 'N/A';

  const completedFormatted = review.completedAt
    ? new Date(review.completedAt).toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'medium'
      })
    : isInProgress
    ? 'Processing...'
    : 'N/A';

  const durationText = formatDuration
    ? formatDuration(review.createdAt, review.completedAt)
    : 'N/A';

  return (
    <div className="card review-metadata-card">
      <div className="card-header review-metadata-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="review-section-icon"
            aria-hidden="true"
          >
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
            <line x1="8" y1="21" x2="16" y2="21" />
            <line x1="12" y1="17" x2="12" y2="21" />
          </svg>
          <h2 className="card-title">Review Metadata</h2>
        </div>
      </div>

      <div className="review-metadata-table-wrapper">
        <table className="data-table review-metadata-table" aria-label="Review metadata details">
          <tbody>
            <tr>
              <th scope="row" className="meta-label">Review ID</th>
              <td className="meta-value">
                <span className="meta-code-badge">#{reviewId}</span>
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Repository</th>
              <td className="meta-value">
                <span className="meta-repo-text" title={fullRepo}>
                  {fullRepo}
                </span>
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Owner / Org</th>
              <td className="meta-value">{review.owner || 'N/A'}</td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Pull Request</th>
              <td className="meta-value">
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span className="meta-code-badge">#{review.pullRequestNumber}</span>
                  {githubPrUrl && (
                    <a
                      href={githubPrUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="meta-external-link"
                      title="Open PR on GitHub"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                        <polyline points="15 3 21 3 21 9" />
                        <line x1="10" y1="14" x2="21" y2="3" />
                      </svg>
                    </a>
                  )}
                </div>
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Review Status</th>
              <td className="meta-value">
                <StatusBadge status={review.status} />
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Created At</th>
              <td className="meta-value" title={review.createdAt ? new Date(review.createdAt).toISOString() : ''}>
                {createdFormatted}
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Completed At</th>
              <td className="meta-value" title={review.completedAt ? new Date(review.completedAt).toISOString() : ''}>
                {completedFormatted}
              </td>
            </tr>

            <tr>
              <th scope="row" className="meta-label">Duration</th>
              <td className="meta-value">
                <span className="meta-duration-text">{durationText}</span>
              </td>
            </tr>

            {review.installationId && (
              <tr>
                <th scope="row" className="meta-label">Installation ID</th>
                <td className="meta-value">
                  <span className="meta-code-badge">{review.installationId}</span>
                </td>
              </tr>
            )}

            {review.commitSha && (
              <tr>
                <th scope="row" className="meta-label">Commit SHA</th>
                <td className="meta-value">
                  <code className="meta-commit-sha">{review.commitSha.substring(0, 7)}</code>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ReviewMetadata;
