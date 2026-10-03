import React from 'react';
import StatusBadge from '../common/StatusBadge';

export const ReviewMetrics = ({ review, formatDuration }) => {
  const durationText = formatDuration
    ? formatDuration(review.createdAt, review.completedAt)
    : 'N/A';

  const hasFindings = (review.totalFindings || 0) > 0;

  return (
    <div className="review-detail-metrics-grid" role="region" aria-label="Review metrics summary">
      {/* Metric 1: Total Findings */}
      <div className={`card review-metric-card ${hasFindings ? 'metric-has-findings' : ''}`}>
        <div className="review-metric-header">
          <span className="review-metric-label">Total Findings</span>
          <div className="review-metric-icon review-metric-icon-findings" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </div>
        </div>
        <div className="review-metric-value">{review.totalFindings || 0}</div>
        <p className="review-metric-desc">
          {hasFindings ? 'Code issues & suggestions identified' : 'No critical issues flagged'}
        </p>
      </div>

      {/* Metric 2: Posted Comments */}
      <div className="card review-metric-card">
        <div className="review-metric-header">
          <span className="review-metric-label">Posted Comments</span>
          <div className="review-metric-icon review-metric-icon-comments" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
          </div>
        </div>
        <div className="review-metric-value">{review.postedCommentsCount || 0}</div>
        <p className="review-metric-desc">Automated review comments sent to GitHub</p>
      </div>

      {/* Metric 3: Duration */}
      <div className="card review-metric-card">
        <div className="review-metric-header">
          <span className="review-metric-label">Execution Duration</span>
          <div className="review-metric-icon review-metric-icon-duration" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </div>
        </div>
        <div className="review-metric-value review-metric-duration">{durationText}</div>
        <p className="review-metric-desc">
          {review.completedAt ? 'End-to-end AI analysis runtime' : 'Analysis currently in progress'}
        </p>
      </div>

      {/* Metric 4: Review Lifecycle State */}
      <div className="card review-metric-card">
        <div className="review-metric-header">
          <span className="review-metric-label">Review Status</span>
          <div className="review-metric-icon review-metric-icon-status" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
          </div>
        </div>
        <div className="review-metric-badge-container">
          <StatusBadge status={review.status} />
        </div>
        <p className="review-metric-desc">
          {review.status === 'COMPLETED'
            ? 'Full analysis completed'
            : review.status === 'IN_PROGRESS'
            ? 'Gemini processing active'
            : 'Execution failed or cancelled'}
        </p>
      </div>
    </div>
  );
};

export default ReviewMetrics;
