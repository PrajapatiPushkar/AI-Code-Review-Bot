import React from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';

export const ReviewTableRow = ({ review, formatDuration }) => {
  const repoName = review.repository || review.repositoryName || 'N/A';
  const fullRepo = review.owner ? `${review.owner}/${repoName}` : repoName;
  const isCompleted = review.status === 'COMPLETED';

  // Format created date cleanly
  const formattedDate = review.createdAt
    ? new Date(review.createdAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    : 'N/A';

  const durationText = formatDuration
    ? formatDuration(review.createdAt, review.completedAt)
    : 'N/A';

  return (
    <tr className="review-table-row">
      {/* Review ID */}
      <td className="review-cell-id">
        <Link
          to={`/reviews/${review.id}`}
          className="review-id-link"
          title={`View review #${review.id}`}
        >
          #{review.id}
        </Link>
      </td>

      {/* Repository */}
      <td className="review-cell-repo">
        <div className="review-repo-container" title={fullRepo}>
          <svg
            className="review-repo-icon"
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
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          <span className="review-repo-name">{fullRepo}</span>
        </div>
      </td>

      {/* Pull Request */}
      <td className="review-cell-pr">
        <div className="review-pr-container">
          <svg
            className="review-pr-icon"
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
            <circle cx="18" cy="18" r="3" />
            <circle cx="6" cy="6" r="3" />
            <path d="M13 6h3a2 2 0 0 1 2 2v7" />
            <line x1="6" y1="9" x2="6" y2="21" />
          </svg>
          <span className="review-pr-number">#{review.pullRequestNumber}</span>
        </div>
      </td>

      {/* Status */}
      <td className="review-cell-status">
        <StatusBadge status={review.status} />
      </td>

      {/* Findings */}
      <td className="review-cell-findings">
        <span
          className={`review-findings-count ${
            review.totalFindings > 0 ? 'has-findings' : ''
          }`}
          title={`${review.totalFindings || 0} issues detected`}
        >
          {review.totalFindings || 0}
        </span>
      </td>

      {/* Posted Comments */}
      <td className="review-cell-comments">
        <span className="review-comments-count" title={`${review.postedCommentsCount || 0} comments posted`}>
          {review.postedCommentsCount || 0}
        </span>
      </td>

      {/* Created Date */}
      <td className="review-cell-created">
        <span
          className="review-timestamp"
          title={review.createdAt ? new Date(review.createdAt).toISOString() : ''}
        >
          {formattedDate}
        </span>
      </td>

      {/* Duration */}
      <td className="review-cell-duration">
        <span className="review-duration" title="Analysis execution duration">
          {durationText}
        </span>
      </td>

      {/* Actions */}
      <td className="review-cell-actions">
        <div className="review-actions-group">
          <Link
            to={`/reviews/${review.id}`}
            className="btn btn-secondary btn-sm review-action-btn"
            title="Inspect review details and summary"
          >
            Details
          </Link>
          {isCompleted && (
            <Link
              to={`/reviews/${review.id}/findings`}
              className="btn btn-outline btn-sm review-action-btn"
              title="Inspect detected findings and suggestions"
            >
              Findings
            </Link>
          )}
        </div>
      </td>
    </tr>
  );
};

export default ReviewTableRow;
