import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import reviewService from '../services/reviewService';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';

const ReviewDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isPolling, setIsPolling] = useState(false);
  const [pollCount, setPollCount] = useState(0);

  const pollTimerRef = useRef(null);

  // Initial fetch
  const fetchReviewDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await reviewService.getReviewById(id);
      setReview(data);
      if (data.status === 'IN_PROGRESS') {
        setIsPolling(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to load review details for ID #${id}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviewDetails();

    return () => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
      }
    };
  }, [id]);

  // Polling logic when status is IN_PROGRESS
  useEffect(() => {
    if (!isPolling) return;

    pollTimerRef.current = setInterval(async () => {
      try {
        setPollCount((prev) => prev + 1);
        const statusData = await reviewService.getReviewStatus(id);

        if (statusData && statusData.status !== 'IN_PROGRESS') {
          // Polling reached terminal state (COMPLETED or FAILED)
          clearInterval(pollTimerRef.current);
          setIsPolling(false);

          // Fetch complete result
          if (statusData.status === 'COMPLETED') {
            const resultData = await reviewService.getReviewResult(id);
            setReview((prev) => ({
              ...prev,
              ...resultData,
              status: 'COMPLETED'
            }));
          } else {
            // FAILED
            setReview((prev) => ({
              ...prev,
              ...statusData,
              status: statusData.status || 'FAILED'
            }));
          }
        }
      } catch (err) {
        // Stop polling on repeated errors
        clearInterval(pollTimerRef.current);
        setIsPolling(false);
      }
    }, 2500);

    // Timeout safety after 60 seconds (24 polling attempts)
    const timeoutTimer = setTimeout(() => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
        setIsPolling(false);
      }
    }, 60000);

    return () => {
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
      clearTimeout(timeoutTimer);
    };
  }, [id, isPolling]);

  if (loading) return <Loading message={`Loading code review #${id}...`} />;
  if (error) return <ErrorMessage message={error} onRetry={fetchReviewDetails} />;
  if (!review) return <ErrorMessage message="Code review not found." />;

  const isCompleted = review.status === 'COMPLETED';
  const isFailed = review.status === 'FAILED';
  const isInProgress = review.status === 'IN_PROGRESS';

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Code Review #{review.id}</h1>
          <p className="page-subtitle">
            Repository: {review.owner ? `${review.owner}/${review.repositoryName || review.repository}` : (review.repositoryName || review.repository)} | PR #{review.pullRequestNumber}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/reviews" className="btn btn-outline btn-sm">
            ← Back to Reviews
          </Link>
          {isCompleted && (
            <Link to={`/reviews/${review.id}/findings`} className="btn btn-primary btn-sm">
              View Findings ({review.totalFindings || 0}) →
            </Link>
          )}
        </div>
      </div>

      {/* Asynchronous Execution Status Banner */}
      {isInProgress && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            backgroundColor: 'var(--status-in-progress-bg)',
            borderColor: 'var(--status-in-progress)',
            display: 'flex',
            alignItems: 'center',
            justify: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div className="spinner" style={{ width: '1.5rem', height: '1.5rem', borderWidth: '2px', margin: 0 }} />
            <div>
              <strong style={{ color: 'var(--status-in-progress)', fontSize: '1rem' }}>
                AI review is running...
              </strong>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
                Extracted PR diff. Executing Gemini AI review in the background.
              </p>
            </div>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Checking review status... (poll #{pollCount})
          </span>
        </div>
      )}

      {/* Failed Review Banner */}
      {isFailed && (
        <div className="error-card" style={{ marginBottom: '1.5rem', display: 'block' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '1.5rem' }}>⚠️</span>
            <strong style={{ fontSize: '1.1rem' }}>Code Review Execution Failed</strong>
          </div>
          <p style={{ fontSize: '0.875rem', marginBottom: '1rem' }}>
            The AI code review encountered an issue during execution. Please verify your repository configuration, installation permissions, or try submitting a new review.
          </p>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/reviews')}>
            Back to Reviews
          </button>
        </div>
      )}

      {/* Review Information Grid */}
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
                <td>{review.owner ? `${review.owner}/${review.repositoryName || review.repository}` : (review.repositoryName || review.repository)}</td>
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
                <td>{review.completedAt ? new Date(review.completedAt).toLocaleString() : (isInProgress ? 'Processing...' : 'N/A')}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* AI Summary Box */}
      <div className="card">
        <h2 className="card-title" style={{ marginBottom: '1rem' }}>AI Summary</h2>
        <div style={{ backgroundColor: 'var(--bg-color)', padding: '1.25rem', borderRadius: 'var(--radius)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', whiteSpace: 'pre-wrap' }}>
          {review.summary || review.reviewSummary || (isInProgress ? 'Review in progress. AI summary will be generated upon completion...' : 'No review summary generated.')}
        </div>
      </div>
    </div>
  );
};

export default ReviewDetailsPage;
