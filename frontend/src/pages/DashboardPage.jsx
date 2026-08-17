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
  const [totalFindingsSum, setTotalFindingsSum] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch recent 5 reviews
      const pageData = await reviewService.getCodeReviews({ page: 0, size: 5, sort: 'createdAt,desc' });
      const recentList = pageData.content || [];
      setReviews(recentList);
      setTotalElements(pageData.totalElements || 0);

      // Calculate total findings across recent reviews
      const findingsSum = recentList.reduce((acc, curr) => acc + (curr.totalFindings || 0), 0);
      setTotalFindingsSum(findingsSum);

      // Fetch counts for status metrics
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

  const formatDuration = (createdStr, completedStr) => {
    if (!createdStr || !completedStr) return 'N/A';
    const created = new Date(createdStr);
    const completed = new Date(completedStr);
    const diffMs = completed - created;
    if (diffMs <= 0) return '< 1s';
    const seconds = Math.floor(diffMs / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ${seconds % 60}s`;
  };

  if (loading) return <Loading message="Loading dashboard overview..." />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Overview of automated AI code reviews and pull request activity.</p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/reviews" className="btn btn-outline">
            View All History →
          </Link>
          <Link to="/reviews/new" className="btn btn-primary">
            + Submit New Review
          </Link>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchDashboardData} />}

      {/* Metrics Summary Grid */}
      <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="metric-card">
          <span className="metric-label">Total Reviews</span>
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
        <div className="metric-card">
          <span className="metric-label">Recent Findings</span>
          <span className="metric-value" style={{ color: '#a5f3fc' }}>
            {totalFindingsSum}
          </span>
        </div>
      </div>

      {/* Recent Activity Table */}
      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Recent Code Reviews</h2>
          <Link to="/reviews" className="btn btn-outline btn-sm">
            View All →
          </Link>
        </div>

        {reviews.length === 0 ? (
          <EmptyState
            title="No Code Reviews Recorded"
            message="Submit a pull request review to get started with automated AI reviews."
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
                  return (
                    <tr key={review.id}>
                      <td>#{review.id}</td>
                      <td><strong>{fullRepo}</strong></td>
                      <td>#{review.pullRequestNumber}</td>
                      <td>
                        <span className={`badge badge-${(review.status || '').toLowerCase()}`}>
                          {review.status}
                        </span>
                      </td>
                      <td>{review.totalFindings || 0}</td>
                      <td>{review.postedCommentsCount || 0}</td>
                      <td>{new Date(review.createdAt).toLocaleString()}</td>
                      <td>{formatDuration(review.createdAt, review.completedAt)}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <Link to={`/reviews/${review.id}`} className="btn btn-secondary btn-sm">
                            Details
                          </Link>
                          {review.status === 'COMPLETED' && (
                            <Link to={`/reviews/${review.id}/findings`} className="btn btn-outline btn-sm">
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
    </div>
  );
};

export default DashboardPage;
