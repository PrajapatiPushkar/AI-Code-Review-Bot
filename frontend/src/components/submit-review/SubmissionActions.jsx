import React from 'react';
import { Link } from 'react-router-dom';

export const SubmissionActions = ({
  submitting = false,
  disabled = false,
  onCancelPath = '/reviews'
}) => {
  return (
    <div className="submit-actions-panel">
      <button
        type="submit"
        className="btn btn-primary submit-btn-primary"
        disabled={disabled || submitting}
        aria-busy={submitting}
      >
        {submitting ? (
          <>
            <span className="submit-spinner" aria-hidden="true" />
            <span>Starting Review...</span>
          </>
        ) : (
          <>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Submit Code Review</span>
          </>
        )}
      </button>

      <Link
        to={onCancelPath}
        className="btn btn-secondary submit-btn-secondary"
        tabIndex={submitting ? -1 : 0}
        aria-disabled={submitting}
        onClick={(e) => {
          if (submitting) e.preventDefault();
        }}
      >
        Cancel
      </Link>
    </div>
  );
};

export default SubmissionActions;
