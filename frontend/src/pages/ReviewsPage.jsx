import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

const ReviewsPage = () => {
  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [ownerFilter, setOwnerFilter] = useState('');
  const [repoFilter, setRepoFilter] = useState('');
  const [prFilter, setPrFilter] = useState('');
  const [sortFilter, setSortFilter] = useState('createdAt,desc');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        page,
        size,
        sort: sortFilter
      };

      if (statusFilter && statusFilter !== 'ALL') params.status = statusFilter;
      if (ownerFilter.trim()) params.owner = ownerFilter.trim();
      if (repoFilter.trim()) params.repository = repoFilter.trim();
      if (prFilter.trim() && !isNaN(Number(prFilter))) params.pullRequestNumber = parseInt(prFilter.trim(), 10);

      const data = await reviewService.getCodeReviews(params);
      setReviews(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch review history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews();
  }, [page, size, statusFilter, sortFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchReviews();
  };

  const handleResetFilters = () => {
    setStatusFilter('ALL');
    setOwnerFilter('');
    setRepoFilter('');
    setPrFilter('');
    setSortFilter('createdAt,desc');
    setPage(0);
  };

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

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Review History</h1>
          <p className="page-subtitle">Track, filter, and inspect past automated pull request reviews.</p>
        </div>
        <Link to="/reviews/new" className="btn btn-primary">
          + Submit New Review
        </Link>
      </div>

      {/* Filter & Search Bar */}
      <form className="filters-bar" onSubmit={handleSearchSubmit}>
        <div className="filter-item">
          <label className="form-label">Status Filter</label>
          <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
            {['ALL', 'COMPLETED', 'IN_PROGRESS', 'FAILED'].map((st) => (
              <button
                type="button"
                key={st}
                className={`btn btn-sm ${statusFilter === st ? 'btn-primary' : 'btn-outline'}`}
                style={{ padding: '0.25rem 0.625rem', fontSize: '0.75rem' }}
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        <div className="filter-item">
          <label className="form-label">Owner</label>
          <input
            type="text"
            className="form-input"
            placeholder="e.g. octocat"
            value={ownerFilter}
            onChange={(e) => setOwnerFilter(e.target.value)}
          />
        </div>

        <div className="filter-item">
          <label className="form-label">Repository</label>
          <input
            type="text"
            className="form-input"
            placeholder="e.g. hello-world"
            value={repoFilter}
            onChange={(e) => setRepoFilter(e.target.value)}
          />
        </div>

        <div className="filter-item">
          <label className="form-label">PR #</label>
          <input
            type="number"
            className="form-input"
            placeholder="e.g. 42"
            value={prFilter}
            onChange={(e) => setPrFilter(e.target.value)}
          />
        </div>

        <div className="filter-item">
          <label className="form-label">Sort By</label>
          <select
            className="form-select"
            value={sortFilter}
            onChange={(e) => {
              setSortFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="createdAt,desc">Newest First</option>
            <option value="createdAt,asc">Oldest First</option>
            <option value="totalFindings,desc">Most Findings</option>
          </select>
        </div>

        <div className="filter-item" style={{ display: 'flex', alignItems: 'flex-end', gap: '0.5rem' }}>
          <button type="submit" className="btn btn-primary btn-sm">
            Search
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleResetFilters}>
            Reset
          </button>
        </div>
      </form>

      {error && <ErrorMessage message={error} onRetry={fetchReviews} />}

      {loading ? (
        <Loading message="Fetching code review history..." />
      ) : reviews.length === 0 ? (
        <EmptyState title="No Review History Found" message="No code reviews matched your criteria." />
      ) : (
        <div className="card">
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
                            View
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

          <div className="pagination">
            <span className="pagination-info">
              Showing page {page + 1} of {totalPages || 1} ({totalElements} total reviews)
            </span>
            <div className="pagination-controls">
              <button
                className="btn btn-outline btn-sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <button
                className="btn btn-outline btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReviewsPage;
