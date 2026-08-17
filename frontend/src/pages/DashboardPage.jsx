import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

const DashboardPage = () => {
  const [reviews, setReviews] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [completedCount, setCompletedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [failedCount, setFailedCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch recent 5 reviews
      const pageData = await reviewService.getCodeReviews({ page: 0, size: 5, sort: 'createdAt,desc' });
      setReviews(pageData.content || []);
      setTotalElements(pageData.totalElements || 0);

      // Fetch counts for metrics
      const [completedData, inProgressData, failedData] = await Promise.allSettled([
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'COMPLETED' }),
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'IN_PROGRESS' }),
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'FAILED' })
      ]);

      if (completedData.status === 'fulfilled') setCompletedCount(completedData.value.totalElements || 0);
      if (inProgressData.status === 'fulfilled') setInProgressCount(inProgressData.value.totalElements || 0);
      if (failedData.status === 'fulfilled') setFailedCount(failedData.value.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  if (loading) return <Loading message="Loading dashboard..." />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Overview of automated AI code reviews and pull request activity.</p>
        </div>
        <Link to="/reviews/new" className="btn btn-primary">
          + Submit New Review
        </Link>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchDashboardData} />}

      <div className="metrics-grid">
        <div className="metric-card">
          <span className="metric-label">Total Code Reviews</span>
          <span className="metric-value">{totalElements}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Completed Reviews</span>
          <span className="metric-value" style={{ color: 'var(--status-completed)' }}>
            {completedCount}
          </span>
        </div>
        <div className="metric-card">
          <span className="metric-label">In Progress</span>
          <span className="metric-value" style={{ color: 'var(--status-in-progress)' }}>
            {inProgressCount}
          </span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Failed Reviews</span>
          <span className="metric-value" style={{ color: 'var(--status-failed)' }}>
            {failedCount}
          </span>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Recent Code Reviews</h2>
          <Link to="/reviews" className="btn btn-outline btn-sm">
            View All Reviews →
          </Link>
        </div>

        {reviews.length === 0 ? (
          <EmptyState
            title="No Recent Reviews"
            message="No code reviews have been recorded yet."
          />
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Repository</th>
                  <th>Pull Request</th>
                  <th>Status</th>
                  <th>Findings</th>
                  <th>Created At</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {reviews.map((review) => (
                  <tr key={review.id}>
                    <td>#{review.id}</td>
                    <td>{review.owner ? `${review.owner}/${review.repositoryName}` : review.repositoryName}</td>
                    <td>#{review.pullRequestNumber}</td>
                    <td>
                      <span className={`badge badge-${(review.status || '').toLowerCase()}`}>
                        {review.status}
                      </span>
                    </td>
                    <td>{review.totalFindings || 0}</td>
                    <td>{new Date(review.createdAt).toLocaleString()}</td>
                    <td>
                      <Link to={`/reviews/${review.id}`} className="btn btn-secondary btn-sm">
                        Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
