import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

export const ReviewActions = ({
  review,
  isInProgress,
  isFailed,
  isCompleted,
  pollCount,
  githubPrUrl
}) => {
  const navigate = useNavigate();
  const reviewId = review?.id || review?.codeReviewId;

  if (isInProgress) {
    return (
      <div className="card review-banner-inprogress" role="status" aria-live="polite">
        <div className="review-banner-content">
          <div className="review-banner-spinner" aria-hidden="true" />
          <div className="review-banner-text">
            <strong className="review-banner-title">
              AI Code Review Running...
            </strong>
            <p className="review-banner-desc">
              Extracted pull request diff. Gemini AI automated code quality and security analysis is executing in the background.
            </p>
          </div>
        </div>
        <div className="review-banner-meta">
          <span className="review-polling-counter">
            Polling status (attempt #{pollCount})
          </span>
        </div>
      </div>
    );
  }

  if (isFailed) {
    return (
      <div className="card review-banner-failed" role="alert">
        <div className="review-banner-content">
          <div className="review-banner-error-icon" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <div className="review-banner-text">
            <strong className="review-banner-title">
              Code Review Execution Failed
            </strong>
            <p className="review-banner-desc">
              The AI review encountered an issue during execution. Please verify your repository configuration, GitHub App installation permissions, or submit a new review.
            </p>
          </div>
        </div>
        <div className="review-banner-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => navigate('/reviews')}
          >
            ← Back to Reviews
          </button>
          <Link to="/reviews/new" className="btn btn-primary btn-sm">
            + Submit New Review
          </Link>
        </div>
      </div>
    );
  }

  if (isCompleted) {
    return (
      <div className="card review-completed-bar">
        <div className="review-completed-info">
          <div className="review-completed-icon" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <div>
            <strong className="review-completed-title">AI Analysis Completed</strong>
            <p className="review-completed-desc">
              {review.totalFindings || 0} findings flagged across diff inspection. {review.postedCommentsCount || 0} automated comments posted to GitHub.
            </p>
          </div>
        </div>

        <div className="review-completed-actions">
          <Link
            to={`/reviews/${reviewId}/findings`}
            className="btn btn-primary btn-sm"
          >
            <span>View Findings ({review.totalFindings || 0})</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </Link>

          {githubPrUrl && (
            <a
              href={githubPrUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline btn-sm"
            >
              <span>GitHub PR</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                <polyline points="15 3 21 3 21 9" />
                <line x1="10" y1="14" x2="21" y2="3" />
              </svg>
            </a>
          )}
        </div>
      </div>
    );
  }

  return null;
};

export default ReviewActions;
