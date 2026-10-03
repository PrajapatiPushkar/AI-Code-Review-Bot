import React from 'react';
import { Link } from 'react-router-dom';
import ReviewTableRow from './ReviewTableRow';
import EmptyState from '../EmptyState';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const ReviewTable = ({
  reviews = [],
  loading = false,
  formatDuration,
  sortFilter = 'createdAt,desc',
  onSortChange,
  onResetFilters,
  hasActiveFilters = false
}) => {
  // Sort helpers for table column headers
  const handleSortToggle = (field) => {
    if (!onSortChange) return;

    if (field === 'createdAt') {
      if (sortFilter === 'createdAt,desc') {
        onSortChange('createdAt,asc');
      } else {
        onSortChange('createdAt,desc');
      }
    } else if (field === 'totalFindings') {
      if (sortFilter === 'totalFindings,desc') {
        onSortChange('createdAt,desc');
      } else {
        onSortChange('totalFindings,desc');
      }
    }
  };

  const getSortIcon = (field) => {
    if (field === 'createdAt') {
      if (sortFilter === 'createdAt,desc') return ' ↓';
      if (sortFilter === 'createdAt,asc') return ' ↑';
    }
    if (field === 'totalFindings') {
      if (sortFilter === 'totalFindings,desc') return ' ↓';
    }
    return '';
  };

  return (
    <div className="reviews-table-container">
      <div className="table-responsive">
        <table className="data-table reviews-table" aria-label="Code reviews history">
          <thead>
            <tr>
              <th scope="col" style={{ width: '80px' }}>ID</th>
              <th scope="col" style={{ minWidth: '180px' }}>Repository</th>
              <th scope="col" style={{ width: '90px' }}>PR #</th>
              <th scope="col" style={{ width: '130px' }}>Status</th>
              <th scope="col" style={{ width: '100px' }}>
                <button
                  type="button"
                  className={`table-header-sort-btn ${sortFilter.startsWith('totalFindings') ? 'active' : ''}`}
                  onClick={() => handleSortToggle('totalFindings')}
                  title="Sort by total findings"
                >
                  Findings{getSortIcon('totalFindings')}
                </button>
              </th>
              <th scope="col" style={{ width: '90px' }}>Comments</th>
              <th scope="col" style={{ minWidth: '160px' }}>
                <button
                  type="button"
                  className={`table-header-sort-btn ${sortFilter.startsWith('createdAt') ? 'active' : ''}`}
                  onClick={() => handleSortToggle('createdAt')}
                  title="Sort by creation date"
                >
                  Created{getSortIcon('createdAt')}
                </button>
              </th>
              <th scope="col" style={{ width: '100px' }}>Duration</th>
              <th scope="col" style={{ width: '150px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              // Match table columns during loading without layout shift
              Array.from({ length: 6 }).map((_, rowIdx) => (
                <tr key={rowIdx} className="skeleton-table-row" aria-hidden="true">
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="50px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="130px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="45px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="rectangular" width="85px" height="22px" borderRadius="9999px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="30px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="30px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="120px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <LoadingSkeleton variant="text" width="50px" height="14px" style={{ marginBottom: 0 }} />
                  </td>
                  <td style={{ padding: '0.875rem 1rem', textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                      <LoadingSkeleton variant="rectangular" width="55px" height="26px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
                      <LoadingSkeleton variant="rectangular" width="65px" height="26px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
                    </div>
                  </td>
                </tr>
              ))
            ) : reviews.length > 0 ? (
              reviews.map((review) => (
                <ReviewTableRow
                  key={review.id}
                  review={review}
                  formatDuration={formatDuration}
                />
              ))
            ) : null}
          </tbody>
        </table>
      </div>

      {/* Empty State when not loading and zero reviews */}
      {!loading && reviews.length === 0 && (
        <div className="reviews-table-empty">
          <EmptyState
            title={hasActiveFilters ? "No reviews found" : "No reviews recorded yet"}
            message={
              hasActiveFilters
                ? "No code reviews matched your active filter criteria. Try adjusting your search query or reset your filters."
                : "No pull request reviews have been recorded. Submit a new review to start automated AI code quality and security analysis."
            }
            icon={
              <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                <line x1="11" y1="8" x2="11" y2="14" />
                <line x1="8" y1="11" x2="14" y2="11" />
              </svg>
            }
            action={
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                {hasActiveFilters && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={onResetFilters}
                  >
                    Clear Filters
                  </button>
                )}
                <Link to="/reviews/new" className="btn btn-primary btn-sm">
                  + New Review
                </Link>
              </div>
            }
          />
        </div>
      )}
    </div>
  );
};

export default ReviewTable;
