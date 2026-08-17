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
  const [statusFilter, setStatusFilter] = useState('');
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

      if (statusFilter) params.status = statusFilter;
      if (ownerFilter) params.owner = ownerFilter;
      if (repoFilter) params.repository = repoFilter;
      if (prFilter) params.pullRequestNumber = parseInt(prFilter, 10);

      const data = await reviewService.getCodeReviews(params);
      setReviews(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch code reviews.');
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
    setStatusFilter('');
    setOwnerFilter('');
    setRepoFilter('');
    setPrFilter('');
    setSortFilter('createdAt,desc');
    setPage(0);
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Code Reviews</h1>
        <p className="page-subtitle">Browse and filter all pull request code reviews.</p>
      </div>

      <form className="filters-bar" onSubmit={handleSearchSubmit}>
        <div className="filter-item">
          <label className="form-label">Status</label>
          <select
            className="form-select"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="FAILED">Failed</option>
          </select>
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
          <label className="form-label">Sort</label>
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
        <Loading message="Fetching code reviews..." />
      ) : reviews.length === 0 ? (
        <EmptyState title="No Reviews Found" message="No code reviews matched your criteria." />
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
                  <th>Posted Comments</th>
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
                    <td>{review.postedCommentsCount || 0}</td>
                    <td>{new Date(review.createdAt).toLocaleString()}</td>
                    <td>
                      <Link to={`/reviews/${review.id}`} className="btn btn-secondary btn-sm">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
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
