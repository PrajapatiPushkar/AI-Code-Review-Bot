import React from 'react';

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All' },
  { value: 'COMPLETED', label: 'Completed', color: 'var(--status-completed)' },
  { value: 'IN_PROGRESS', label: 'In Progress', color: 'var(--status-in-progress)' },
  { value: 'FAILED', label: 'Failed', color: 'var(--status-failed)' }
];

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest First' },
  { value: 'createdAt,asc', label: 'Oldest First' },
  { value: 'totalFindings,desc', label: 'Most Findings' }
];

export const ReviewFilters = ({
  statusFilter = 'ALL',
  onStatusChange,
  ownerFilter = '',
  onOwnerChange,
  repoFilter = '',
  onRepoChange,
  prFilter = '',
  onPrChange,
  sortFilter = 'createdAt,desc',
  onSortChange,
  onSubmit,
  onReset,
  isLoading = false
}) => {
  const hasActiveFilters =
    statusFilter !== 'ALL' ||
    ownerFilter.trim() !== '' ||
    repoFilter.trim() !== '' ||
    prFilter.trim() !== '' ||
    sortFilter !== 'createdAt,desc';

  return (
    <div className="card reviews-filter-card">
      <form onSubmit={onSubmit} className="reviews-filter-form" role="search" aria-label="Filter reviews">
        {/* Top Row: Search Inputs */}
        <div className="filter-inputs-grid">
          {/* Repository Input */}
          <div className="filter-field">
            <label htmlFor="filter-repo" className="form-label">
              Repository
            </label>
            <div className="input-with-icon">
              <svg
                className="input-icon"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              <input
                id="filter-repo"
                type="text"
                className="form-input"
                placeholder="e.g. hello-world"
                value={repoFilter}
                onChange={(e) => onRepoChange(e.target.value)}
                disabled={isLoading}
              />
              {repoFilter && (
                <button
                  type="button"
                  className="input-clear-btn"
                  onClick={() => onRepoChange('')}
                  title="Clear repository filter"
                  aria-label="Clear repository filter"
                >
                  ×
                </button>
              )}
            </div>
          </div>

          {/* Owner / Org Input */}
          <div className="filter-field">
            <label htmlFor="filter-owner" className="form-label">
              Owner / Org
            </label>
            <div className="input-with-icon">
              <svg
                className="input-icon"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <input
                id="filter-owner"
                type="text"
                className="form-input"
                placeholder="e.g. octocat"
                value={ownerFilter}
                onChange={(e) => onOwnerChange(e.target.value)}
                disabled={isLoading}
              />
              {ownerFilter && (
                <button
                  type="button"
                  className="input-clear-btn"
                  onClick={() => onOwnerChange('')}
                  title="Clear owner filter"
                  aria-label="Clear owner filter"
                >
                  ×
                </button>
              )}
            </div>
          </div>

          {/* Pull Request # Input */}
          <div className="filter-field filter-field-pr">
            <label htmlFor="filter-pr" className="form-label">
              PR #
            </label>
            <div className="input-with-icon">
              <svg
                className="input-icon"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <circle cx="18" cy="18" r="3" />
                <circle cx="6" cy="6" r="3" />
                <path d="M13 6h3a2 2 0 0 1 2 2v7" />
                <line x1="6" y1="9" x2="6" y2="21" />
              </svg>
              <input
                id="filter-pr"
                type="number"
                className="form-input"
                placeholder="e.g. 42"
                value={prFilter}
                onChange={(e) => onPrChange(e.target.value)}
                disabled={isLoading}
              />
              {prFilter && (
                <button
                  type="button"
                  className="input-clear-btn"
                  onClick={() => onPrChange('')}
                  title="Clear PR number filter"
                  aria-label="Clear PR number filter"
                >
                  ×
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Second Row: Status, Sort & Actions */}
        <div className="filter-controls-row">
          {/* Status Filter Segmented Controls */}
          <div className="filter-status-group">
            <label className="form-label">Status</label>
            <div className="status-pills-container" role="radiogroup" aria-label="Filter by review status">
              {STATUS_OPTIONS.map((opt) => {
                const isActive = statusFilter === opt.value;
                return (
                  <button
                    type="button"
                    key={opt.value}
                    role="radio"
                    aria-checked={isActive}
                    className={`status-pill-btn ${isActive ? 'active' : ''}`}
                    onClick={() => onStatusChange(opt.value)}
                    disabled={isLoading}
                  >
                    {opt.color && (
                      <span
                        className="status-pill-dot"
                        style={{ backgroundColor: opt.color }}
                        aria-hidden="true"
                      />
                    )}
                    <span>{opt.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Sort By Select */}
          <div className="filter-sort-group">
            <label htmlFor="filter-sort" className="form-label">
              Sort By
            </label>
            <div className="sort-select-wrapper">
              <select
                id="filter-sort"
                className="form-select filter-sort-select"
                value={sortFilter}
                onChange={(e) => onSortChange(e.target.value)}
                disabled={isLoading}
              >
                {SORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="filter-actions-group">
            <button
              type="submit"
              className="btn btn-primary btn-sm filter-apply-btn"
              disabled={isLoading}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <span>Search</span>
            </button>

            <button
              type="button"
              className="btn btn-secondary btn-sm filter-reset-btn"
              onClick={onReset}
              disabled={isLoading || !hasActiveFilters}
              title="Reset all filters to default"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="1 4 1 10 7 10" />
                <polyline points="23 20 23 14 17 14" />
                <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
              </svg>
              <span>Reset</span>
            </button>
          </div>
        </div>

        {/* Active Filter Chips / Badges */}
        {hasActiveFilters && (
          <div className="active-filters-row">
            <span className="active-filters-label">Active Filters:</span>
            <div className="active-filters-chips">
              {statusFilter !== 'ALL' && (
                <span className="active-filter-chip">
                  <span>Status: <strong>{statusFilter}</strong></span>
                  <button
                    type="button"
                    className="chip-remove-btn"
                    onClick={() => onStatusChange('ALL')}
                    aria-label="Remove status filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {repoFilter.trim() && (
                <span className="active-filter-chip">
                  <span>Repo: <strong>{repoFilter.trim()}</strong></span>
                  <button
                    type="button"
                    className="chip-remove-btn"
                    onClick={() => onRepoChange('')}
                    aria-label="Remove repository filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {ownerFilter.trim() && (
                <span className="active-filter-chip">
                  <span>Owner: <strong>{ownerFilter.trim()}</strong></span>
                  <button
                    type="button"
                    className="chip-remove-btn"
                    onClick={() => onOwnerChange('')}
                    aria-label="Remove owner filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {prFilter.trim() && (
                <span className="active-filter-chip">
                  <span>PR: <strong>#{prFilter.trim()}</strong></span>
                  <button
                    type="button"
                    className="chip-remove-btn"
                    onClick={() => onPrChange('')}
                    aria-label="Remove PR filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {sortFilter !== 'createdAt,desc' && (
                <span className="active-filter-chip">
                  <span>
                    Sort: <strong>{SORT_OPTIONS.find(s => s.value === sortFilter)?.label || sortFilter}</strong>
                  </span>
                  <button
                    type="button"
                    className="chip-remove-btn"
                    onClick={() => onSortChange('createdAt,desc')}
                    aria-label="Reset sort order"
                  >
                    ×
                  </button>
                </span>
              )}

              <button
                type="button"
                className="clear-all-filters-btn"
                onClick={onReset}
              >
                Clear all
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
};

export default ReviewFilters;
