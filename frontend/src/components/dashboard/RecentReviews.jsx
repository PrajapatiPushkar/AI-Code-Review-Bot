import React from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';

export const RecentReviews = ({ reviews = [], formatDuration, className = '' }) => {
  return (
    <div className={`card recent-reviews-card ${className}`}>
      <div className="card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <h2 className="card-title">Recent Code Reviews</h2>
          <span className="badge badge-info" style={{ textTransform: 'none', letterSpacing: 'normal' }}>
            Latest 5
          </span>
        </div>
        <Link to="/reviews" className="btn btn-outline btn-sm">
          View All History →
        </Link>
      </div>

      {!reviews || reviews.length === 0 ? (
        <div className="dashboard-empty-state">
          <div className="dashboard-empty-icon" aria-hidden="true">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
          </div>
          <h3 className="dashboard-empty-title">No Code Reviews Recorded</h3>
          <p className="dashboard-empty-desc">
            Submit a GitHub pull request review to start automated AI code quality and security analysis.
          </p>
          <Link to="/reviews/new" className="btn btn-primary btn-sm">
            + Start First Review
          </Link>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Repository</th>
                <th>PR #</th>
                <th>Status</th>
                <th>Findings</th>
                <th>Comments</th>
                <th>Created At</th>
                <th>Duration</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {reviews.map((review) => {
                const repoName = review.repository || review.repositoryName || 'N/A';
                const fullRepo = review.owner ? `${review.owner}/${repoName}` : repoName;
                const isCompleted = review.status === 'COMPLETED';

                return (
                  <tr key={review.id}>
                    <td>
                      <Link to={`/reviews/${review.id}`} className="review-id-link">
                        #{review.id}
                      </Link>
                    </td>
                    <td>
                      <span className="review-repo-name" title={fullRepo}>
                        {fullRepo}
                      </span>
                    </td>
                    <td>
                      <span className="review-pr-number">#{review.pullRequestNumber}</span>
                    </td>
                    <td>
                      <StatusBadge status={review.status} />
                    </td>
                    <td>
                      <span className={review.totalFindings > 0 ? 'review-findings-count has-findings' : 'review-findings-count'}>
                        {review.totalFindings || 0}
                      </span>
                    </td>
                    <td>{review.postedCommentsCount || 0}</td>
                    <td>
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                        {new Date(review.createdAt).toLocaleString()}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        {formatDuration ? formatDuration(review.createdAt, review.completedAt) : 'N/A'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.375rem', alignItems: 'center' }}>
                        <Link to={`/reviews/${review.id}`} className="btn btn-secondary btn-sm" style={{ padding: '0.25rem 0.625rem' }}>
                          Details
                        </Link>
                        {isCompleted && (
                          <Link to={`/reviews/${review.id}/findings`} className="btn btn-outline btn-sm" style={{ padding: '0.25rem 0.625rem' }}>
                            Findings
                          </Link>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default RecentReviews;
