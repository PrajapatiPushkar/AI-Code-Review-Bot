import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';

const ReviewDetailsPage = () => {
  const { id } = useParams();
  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReviewDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await reviewService.getReviewById(id);
      setReview(data);
    } catch (err) {
      setError(err.response?.data?.message || `Failed to load review details for ID #${id}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviewDetails();
  }, [id]);

  if (loading) return <Loading message={`Loading code review #${id}...`} />;
  if (error) return <ErrorMessage message={error} onRetry={fetchReviewDetails} />;
  if (!review) return <ErrorMessage message="Code review not found." />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Code Review #{review.id}</h1>
          <p className="page-subtitle">
            Repository: {review.owner ? `${review.owner}/${review.repositoryName}` : review.repositoryName} | PR #{review.pullRequestNumber}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/reviews" className="btn btn-outline btn-sm">
            ← Back to Reviews
          </Link>
          <Link to={`/reviews/${review.id}/findings`} className="btn btn-primary btn-sm">
            View Findings ({review.totalFindings || 0}) →
          </Link>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: '1rem' }}>Metadata Overview</h2>
          <table className="data-table">
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Status</td>
                <td>
                  <span className={`badge badge-${(review.status || '').toLowerCase()}`}>
                    {review.status}
                  </span>
                </td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Repository</td>
                <td>{review.owner ? `${review.owner}/${review.repositoryName}` : review.repositoryName}</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Pull Request</td>
                <td>#{review.pullRequestNumber}</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Commit SHA</td>
                <td><code>{review.commitSha || 'N/A'}</code></td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Installation ID</td>
                <td>{review.installationId || 'N/A'}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="card">
          <h2 className="card-title" style={{ marginBottom: '1rem' }}>Review Metrics & Execution</h2>
          <table className="data-table">
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Total Findings</td>
                <td><strong>{review.totalFindings || 0}</strong></td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Posted Comments</td>
                <td><strong>{review.postedCommentsCount || 0}</strong></td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Created Time</td>
                <td>{review.createdAt ? new Date(review.createdAt).toLocaleString() : 'N/A'}</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Completed Time</td>
                <td>{review.completedAt ? new Date(review.completedAt).toLocaleString() : 'In Progress'}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <h2 className="card-title" style={{ marginBottom: '1rem' }}>AI Summary</h2>
        <div style={{ backgroundColor: 'var(--bg-color)', padding: '1.25rem', borderRadius: 'var(--radius)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', whiteSpace: 'pre-wrap' }}>
          {review.summary || 'No review summary generated yet.'}
        </div>
      </div>
    </div>
  );
};

export default ReviewDetailsPage;
