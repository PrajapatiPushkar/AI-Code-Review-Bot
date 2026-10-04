import React from 'react';
import { Link } from 'react-router-dom';

export const ReviewSubmissionHeader = ({ hasOriginState = false, repository = '' }) => {
  return (
    <header className="submit-review-header">
      <div className="submit-review-header-content">
        {hasOriginState && repository && (
          <div className="submit-origin-badge" aria-label="Preselected from Repository Explorer">
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
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <span>Target Selected from Repositories</span>
          </div>
        )}
        <h1 className="page-title">Submit Code Review</h1>
        <p className="page-subtitle">
          Select a repository and pull request to start an AI-assisted code review.
        </p>
      </div>

      <div className="page-header-actions">
        <Link to="/repositories" className="btn btn-outline btn-sm" title="View all accessible repositories">
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
            <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
            <circle cx="12" cy="13" r="2" />
          </svg>
          <span>Repositories</span>
        </Link>
        <Link to="/reviews" className="btn btn-outline btn-sm" title="View past code review executions">
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
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span>Review History</span>
        </Link>
      </div>
    </header>
  );
};

export default ReviewSubmissionHeader;
