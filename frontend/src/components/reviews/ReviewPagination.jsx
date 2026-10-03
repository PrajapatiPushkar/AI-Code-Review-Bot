import React from 'react';

export const ReviewPagination = ({
  page = 0,
  size = 10,
  totalPages = 0,
  totalElements = 0,
  onPageChange,
  onSizeChange,
  disabled = false
}) => {
  if (totalElements === 0 && totalPages <= 1) {
    return null;
  }

  const currentPage = page + 1;
  const effectiveTotalPages = Math.max(totalPages, 1);
  const startRecord = totalElements === 0 ? 0 : page * size + 1;
  const endRecord = Math.min((page + 1) * size, totalElements);

  // Generate visible page numbers
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;

    if (effectiveTotalPages <= maxVisible) {
      for (let i = 0; i < effectiveTotalPages; i++) {
        pages.push(i);
      }
    } else {
      let start = Math.max(0, page - 2);
      let end = Math.min(effectiveTotalPages - 1, page + 2);

      if (page <= 2) {
        end = maxVisible - 1;
      } else if (page >= effectiveTotalPages - 3) {
        start = effectiveTotalPages - maxVisible;
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
    }

    return pages;
  };

  return (
    <nav className="reviews-pagination" aria-label="Reviews pagination navigation">
      {/* Records count info */}
      <div className="pagination-info">
        <span>
          Showing <strong>{startRecord}</strong>–<strong>{endRecord}</strong> of{' '}
          <strong>{totalElements}</strong> reviews
        </span>
        <span className="pagination-pages-badge">
          Page {currentPage} of {effectiveTotalPages}
        </span>
      </div>

      {/* Page navigation controls */}
      <div className="pagination-controls">
        {onSizeChange && (
          <div className="pagination-size-selector">
            <label htmlFor="reviews-page-size" className="pagination-size-label">
              Per page:
            </label>
            <select
              id="reviews-page-size"
              className="form-select pagination-size-select"
              value={size}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              disabled={disabled}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>
        )}

        <button
          type="button"
          className="btn btn-outline btn-sm pagination-nav-btn"
          disabled={page === 0 || disabled}
          onClick={() => onPageChange(page - 1)}
          aria-label="Go to previous page"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>Prev</span>
        </button>

        <div className="pagination-numbers" role="group" aria-label="Page selection">
          {getPageNumbers().map((p) => {
            const isCurrent = p === page;
            return (
              <button
                key={p}
                type="button"
                className={`pagination-number-btn ${isCurrent ? 'active' : ''}`}
                onClick={() => onPageChange(p)}
                disabled={disabled || isCurrent}
                aria-current={isCurrent ? 'page' : undefined}
                aria-label={`Page ${p + 1}`}
              >
                {p + 1}
              </button>
            );
          })}
        </div>

        <button
          type="button"
          className="btn btn-outline btn-sm pagination-nav-btn"
          disabled={page >= effectiveTotalPages - 1 || disabled}
          onClick={() => onPageChange(page + 1)}
          aria-label="Go to next page"
        >
          <span>Next</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </nav>
  );
};

export default ReviewPagination;
