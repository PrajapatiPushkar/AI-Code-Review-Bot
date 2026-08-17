import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import ErrorMessage from '../components/ErrorMessage';

const SubmitReviewPage = () => {
  const [installationId, setInstallationId] = useState('');
  const [owner, setOwner] = useState('');
  const [repository, setRepository] = useState('');
  const [pullRequestNumber, setPullRequestNumber] = useState('');
  const [commitSha, setCommitSha] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [submittedReview, setSubmittedReview] = useState(null);

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    // Frontend Validation
    if (!installationId || isNaN(Number(installationId)) || Number(installationId) <= 0) {
      setError('Please provide a valid positive GitHub Installation ID.');
      return;
    }
    if (!owner.trim()) {
      setError('Repository owner is required.');
      return;
    }
    if (!repository.trim()) {
      setError('Repository name is required.');
      return;
    }
    if (!pullRequestNumber || isNaN(Number(pullRequestNumber)) || Number(pullRequestNumber) <= 0) {
      setError('Please provide a valid positive Pull Request Number.');
      return;
    }

    const payload = {
      installationId: Number(installationId),
      owner: owner.trim(),
      repository: repository.trim(),
      pullRequestNumber: Number(pullRequestNumber),
      ...(commitSha.trim() ? { commitSha: commitSha.trim() } : {})
    };

    try {
      setSubmitting(true);
      const result = await reviewService.submitPullRequestReview(payload);
      setSubmittedReview(result);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to submit pull request for AI review. Please check inputs.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Submit Pull Request Review</h1>
          <p className="page-subtitle">Trigger an asynchronous AI code review for a GitHub PR.</p>
        </div>
        <Link to="/reviews" className="btn btn-outline btn-sm">
          ← Back to Reviews
        </Link>
      </div>

      {error && <ErrorMessage message={error} />}

      {submittedReview ? (
        <div className="card" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center', padding: '2.5rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem', color: 'var(--status-completed)' }}>✅</div>
          <h2 className="card-title" style={{ justifyContent: 'center', marginBottom: '0.5rem' }}>
            Code Review Submitted Successfully
          </h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
            Review request has been accepted and is processing asynchronously in the background.
          </p>

          <div className="card" style={{ backgroundColor: 'var(--bg-color)', textAlign: 'left', marginBottom: '2rem' }}>
            <table className="data-table">
              <tbody>
                <tr>
                  <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Review ID</td>
                  <td><strong>#{submittedReview.codeReviewId}</strong></td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Repository</td>
                  <td>{submittedReview.owner ? `${submittedReview.owner}/${submittedReview.repository}` : submittedReview.repository}</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Pull Request</td>
                  <td>#{submittedReview.pullRequestNumber}</td>
                </tr>
                <tr>
                  <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Initial Status</td>
                  <td>
                    <span className={`badge badge-${(submittedReview.status || 'in_progress').toLowerCase()}`}>
                      {submittedReview.status || 'IN_PROGRESS'}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <button
              className="btn btn-primary"
              onClick={() => navigate(`/reviews/${submittedReview.codeReviewId}`)}
            >
              View Review →
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setSubmittedReview(null);
                setPullRequestNumber('');
                setCommitSha('');
              }}
            >
              Submit Another PR
            </button>
          </div>
        </div>
      ) : (
        <div className="card" style={{ maxWidth: '640px', margin: '0 auto' }}>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="installationId">
                GitHub App Installation ID *
              </label>
              <input
                id="installationId"
                type="number"
                className="form-input"
                placeholder="e.g. 100"
                value={installationId}
                onChange={(e) => setInstallationId(e.target.value)}
                disabled={submitting}
                required
              />
              <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                Numerical ID of the GitHub App installation authorized for this repository.
              </small>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="owner">
                  Repository Owner *
                </label>
                <input
                  id="owner"
                  type="text"
                  className="form-input"
                  placeholder="e.g. octocat"
                  value={owner}
                  onChange={(e) => setOwner(e.target.value)}
                  disabled={submitting}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="repository">
                  Repository Name *
                </label>
                <input
                  id="repository"
                  type="text"
                  className="form-input"
                  placeholder="e.g. hello-world"
                  value={repository}
                  onChange={(e) => setRepository(e.target.value)}
                  disabled={submitting}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pullRequestNumber">
                  Pull Request Number *
                </label>
                <input
                  id="pullRequestNumber"
                  type="number"
                  className="form-input"
                  placeholder="e.g. 42"
                  value={pullRequestNumber}
                  onChange={(e) => setPullRequestNumber(e.target.value)}
                  disabled={submitting}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="commitSha">
                  Commit SHA (Optional)
                </label>
                <input
                  id="commitSha"
                  type="text"
                  className="form-input"
                  placeholder="e.g. 6dcb09b5..."
                  value={commitSha}
                  onChange={(e) => setCommitSha(e.target.value)}
                  disabled={submitting}
                />
              </div>
            </div>

            <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
              <Link to="/reviews" className="btn btn-secondary">
                Cancel
              </Link>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Submitting PR for AI Review...' : 'Submit for AI Review'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default SubmitReviewPage;
