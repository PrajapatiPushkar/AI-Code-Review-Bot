import React from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';

export const ReviewHeader = ({
  review,
  githubPrUrl,
  isCompleted,
  isInProgress,
  isFailed
}) => {
  const reviewId = review.id || review.codeReviewId;
  const repoName = review.repository || review.repositoryName || 'N/A';
  const fullRepo = review.owner ? `${review.owner}/${repoName}` : repoName;

  return (
    <div className="review-detail-header-wrapper">
      {/* Breadcrumb Navigation */}
      <nav aria-label="Breadcrumb" className="review-detail-breadcrumb">
        <Link to="/reviews" className="review-back-link">
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
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>Reviews History</span>
        </Link>
        <span className="breadcrumb-separator" aria-hidden="true">/</span>
        <span className="breadcrumb-current">Review #{reviewId}</span>
      </nav>

      {/* Main Header Content */}
      <div className="review-detail-header">
        <div className="review-detail-title-group">
          <div className="review-detail-title-row">
            <h1 className="review-detail-title">Review #{reviewId}</h1>
            <StatusBadge status={review.status} />
            {isInProgress && (
              <span className="review-polling-badge" title="Live status polling active">
                <span className="polling-pulse-dot" aria-hidden="true" />
                Live
              </span>
            )}
          </div>

          <div className="review-detail-meta-line">
            <span className="review-detail-repo" title={`Repository: ${fullRepo}`}>
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
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              <span>{fullRepo}</span>
            </span>

            <span className="meta-separator" aria-hidden="true">•</span>

            <span className="review-detail-pr">
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
                <circle cx="18" cy="18" r="3" />
                <circle cx="6" cy="6" r="3" />
                <path d="M13 6h3a2 2 0 0 1 2 2v7" />
                <line x1="6" y1="9" x2="6" y2="21" />
              </svg>
              <span>PR #{review.pullRequestNumber}</span>
            </span>

            {githubPrUrl && (
              <>
                <span className="meta-separator" aria-hidden="true">•</span>
                <a
                  href={githubPrUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="review-github-pr-link"
                  title="Open pull request on GitHub in a new tab"
                >
                  <span>Open on GitHub</span>
                  <svg
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
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                    <polyline points="15 3 21 3 21 9" />
                    <line x1="10" y1="14" x2="21" y2="3" />
                  </svg>
                </a>
              </>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="review-detail-actions">
          {isCompleted && (
            <Link
              to={`/reviews/${reviewId}/findings`}
              className="btn btn-primary btn-sm review-findings-action-btn"
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
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>View Findings ({review.totalFindings || 0})</span>
            </Link>
          )}

          {githubPrUrl && (
            <a
              href={githubPrUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline btn-sm review-github-action-btn"
              title="Open GitHub PR in new tab"
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
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                <polyline points="15 3 21 3 21 9" />
                <line x1="10" y1="14" x2="21" y2="3" />
              </svg>
              <span>Open GitHub PR</span>
            </a>
          )}

          <Link to="/reviews/new" className="btn btn-secondary btn-sm">
            + New Review
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ReviewHeader;
