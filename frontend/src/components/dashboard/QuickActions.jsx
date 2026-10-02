import React from 'react';
import { Link } from 'react-router-dom';

export const QuickActions = ({ className = '' }) => {
  return (
    <div className={`card quick-actions-card ${className}`}>
      <div className="card-header" style={{ marginBottom: '1rem' }}>
        <h2 className="card-title" style={{ fontSize: '1rem', fontWeight: 600 }}>
          Quick Actions
        </h2>
      </div>

      <div className="quick-actions-list">
        {/* Action 1: Submit Review */}
        <Link to="/reviews/new" className="quick-action-item quick-action-primary">
          <div className="quick-action-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </div>
          <div className="quick-action-content">
            <span className="quick-action-title">Submit New Review</span>
            <span className="quick-action-desc">Trigger AI review for a pull request</span>
          </div>
          <span className="quick-action-arrow" aria-hidden="true">→</span>
        </Link>

        {/* Action 2: Review History */}
        <Link to="/reviews" className="quick-action-item">
          <div className="quick-action-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="18" cy="18" r="3" />
              <circle cx="6" cy="6" r="3" />
              <path d="M13 6h3a2 2 0 0 1 2 2v7" />
              <line x1="6" y1="9" x2="6" y2="21" />
            </svg>
          </div>
          <div className="quick-action-content">
            <span className="quick-action-title">Browse All Reviews</span>
            <span className="quick-action-desc">Filter and inspect past PR reviews</span>
          </div>
          <span className="quick-action-arrow" aria-hidden="true">→</span>
        </Link>

        {/* Action 3: Findings Explorer */}
        <Link to="/findings" className="quick-action-item">
          <div className="quick-action-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <div className="quick-action-content">
            <span className="quick-action-title">Findings Explorer</span>
            <span className="quick-action-desc">Security, bugs, and performance fixes</span>
          </div>
          <span className="quick-action-arrow" aria-hidden="true">→</span>
        </Link>
      </div>
    </div>
  );
};

export default QuickActions;
