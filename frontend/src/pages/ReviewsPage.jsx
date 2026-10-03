import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import ErrorMessage from '../components/ErrorMessage';
import ReviewFilters from '../components/reviews/ReviewFilters';
import ReviewTable from '../components/reviews/ReviewTable';
import ReviewPagination from '../components/reviews/ReviewPagination';

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

  // Request & Feedback State
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTrigger, setSearchTrigger] = useState(0);

  const fetchReviews = useCallback(async () => {
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
      if (prFilter.trim() && !isNaN(Number(prFilter))) {
        params.pullRequestNumber = parseInt(prFilter.trim(), 10);
      }

      const data = await reviewService.getCodeReviews(params);
      setReviews(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch review history.');
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, sortFilter, ownerFilter, repoFilter, prFilter]);

  // Fetch when page, size, status, sort or explicit search trigger changes
  useEffect(() => {
    fetchReviews();
  }, [page, size, statusFilter, sortFilter, searchTrigger]);

  const handleSearchSubmit = (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (page === 0) {
      setSearchTrigger((prev) => prev + 1);
    } else {
      setPage(0);
    }
  };

  const handleResetFilters = () => {
    setStatusFilter('ALL');
    setOwnerFilter('');
    setRepoFilter('');
    setPrFilter('');
    setSortFilter('createdAt,desc');
    if (page === 0) {
      setSearchTrigger((prev) => prev + 1);
    } else {
      setPage(0);
    }
  };

  const handleStatusChange = (newStatus) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  const handleSortChange = (newSort) => {
    setSortFilter(newSort);
    setPage(0);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  const handleSizeChange = (newSize) => {
    setSize(newSize);
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

  const hasActiveFilters =
    statusFilter !== 'ALL' ||
    ownerFilter.trim() !== '' ||
    repoFilter.trim() !== '' ||
    prFilter.trim() !== '' ||
    sortFilter !== 'createdAt,desc';

  return (
    <div className="reviews-page">
      {/* Page Header */}
      <div className="page-header reviews-page-header">
        <div className="reviews-header-info">
          <div className="reviews-title-row">
            <h1 className="page-title">Reviews</h1>
            <span className="badge badge-info reviews-count-badge" aria-label={`${totalElements} total reviews`}>
              {totalElements} {totalElements === 1 ? 'review' : 'reviews'}
            </span>
          </div>
          <p className="page-subtitle">Review history and AI analysis activity.</p>
        </div>
        <Link to="/reviews/new" className="btn btn-primary reviews-new-btn">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>New Review</span>
        </Link>
      </div>

      {/* Modern Filter & Search Controls */}
      <ReviewFilters
        statusFilter={statusFilter}
        onStatusChange={handleStatusChange}
        ownerFilter={ownerFilter}
        onOwnerChange={setOwnerFilter}
        repoFilter={repoFilter}
        onRepoChange={setRepoFilter}
        prFilter={prFilter}
        onPrChange={setPrFilter}
        sortFilter={sortFilter}
        onSortChange={handleSortChange}
        onSubmit={handleSearchSubmit}
        onReset={handleResetFilters}
        isLoading={loading}
      />

      {/* Persistent Error State */}
      {error && <ErrorMessage message={error} onRetry={fetchReviews} />}

      {/* Reviews Table Card Container */}
      <div className="card reviews-table-card">
        <div className="reviews-table-header">
          <div className="reviews-table-title-group">
            <h2 className="reviews-table-heading">Review Activity</h2>
            {hasActiveFilters && (
              <span className="reviews-filtered-indicator">Filtered results</span>
            )}
          </div>
          <button
            type="button"
            className="btn btn-outline btn-sm reviews-refresh-btn"
            onClick={fetchReviews}
            disabled={loading}
            title="Refresh review history"
            aria-label="Refresh review history"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className={loading ? 'spinning' : ''}
              aria-hidden="true"
            >
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
            </svg>
            <span className="reviews-refresh-text">Refresh</span>
          </button>
        </div>

        {/* Real table with SkeletonRows during loading */}
        <ReviewTable
          reviews={reviews}
          loading={loading}
          formatDuration={formatDuration}
          sortFilter={sortFilter}
          onSortChange={handleSortChange}
          onResetFilters={handleResetFilters}
          hasActiveFilters={hasActiveFilters}
          totalElements={totalElements}
        />

        {/* Server-side Pagination */}
        {!loading && reviews.length > 0 && (
          <ReviewPagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={handlePageChange}
            onSizeChange={handleSizeChange}
            disabled={loading}
          />
        )}
      </div>
    </div>
  );
};

export default ReviewsPage;
