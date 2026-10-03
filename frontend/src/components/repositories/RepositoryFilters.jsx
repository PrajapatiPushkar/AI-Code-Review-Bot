import React from 'react';

export const RepositoryFilters = ({
  searchTerm = '',
  onSearchChange,
  visibilityFilter = 'ALL',
  onVisibilityChange,
  sortOption = 'name-asc',
  onSortChange,
  totalCount = 0,
  filteredCount = 0,
  onReset
}) => {
  const isFiltered = Boolean(searchTerm.trim() || visibilityFilter !== 'ALL' || sortOption !== 'name-asc');

  return (
    <div className="card repository-filters-card">
      <div className="repository-filters-grid">
        {/* Search Field */}
        <div className="repository-search-field">
          <label htmlFor="repo-search-input" className="form-label">
            Search Repositories
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
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              id="repo-search-input"
              type="text"
              className="form-input"
              placeholder="Filter by repository name or owner..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              aria-label="Search repositories by name or owner"
            />
            {searchTerm && (
              <button
                type="button"
                className="input-clear-btn"
                onClick={() => onSearchChange('')}
                title="Clear search"
                aria-label="Clear repository search"
              >
                ✕
              </button>
            )}
          </div>
        </div>

        {/* Visibility Filter */}
        <div className="repository-visibility-field">
          <label className="form-label" id="visibility-label">
            Visibility
          </label>
          <div className="visibility-btn-group" role="group" aria-labelledby="visibility-label">
            <button
              type="button"
              className={`visibility-btn ${visibilityFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => onVisibilityChange('ALL')}
            >
              All
            </button>
            <button
              type="button"
              className={`visibility-btn ${visibilityFilter === 'PUBLIC' ? 'active' : ''}`}
              onClick={() => onVisibilityChange('PUBLIC')}
            >
              Public
            </button>
            <button
              type="button"
              className={`visibility-btn ${visibilityFilter === 'PRIVATE' ? 'active' : ''}`}
              onClick={() => onVisibilityChange('PRIVATE')}
            >
              Private
            </button>
          </div>
        </div>

        {/* Sort Option */}
        <div className="repository-sort-field">
          <label htmlFor="repo-sort-select" className="form-label">
            Sort Order
          </label>
          <select
            id="repo-sort-select"
            className="form-input"
            value={sortOption}
            onChange={(e) => onSortChange(e.target.value)}
            aria-label="Sort repositories"
          >
            <option value="name-asc">Name (A → Z)</option>
            <option value="name-desc">Name (Z → A)</option>
          </select>
        </div>

        {/* Reset Action */}
        <div className="repository-reset-field">
          {isFiltered && (
            <button
              type="button"
              className="btn btn-outline btn-sm repository-reset-btn"
              onClick={onReset}
              title="Reset all filters"
            >
              Reset Filters
            </button>
          )}
        </div>
      </div>

      {/* Filter Status Summary */}
      <div className="repository-filter-summary">
        <span className="repository-count-text">
          Showing <strong>{filteredCount}</strong> of <strong>{totalCount}</strong> repositories
        </span>
        {isFiltered && (
          <span className="repository-filter-indicator">
            (Filtered)
          </span>
        )}
      </div>
    </div>
  );
};

export default RepositoryFilters;
