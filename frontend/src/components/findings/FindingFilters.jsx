import React from 'react';

const SEVERITY_OPTIONS = [
  { value: 'ALL', label: 'All', color: null },
  { value: 'CRITICAL', label: 'Critical', color: 'var(--severity-critical)' },
  { value: 'HIGH', label: 'High', color: 'var(--severity-high)' },
  { value: 'MEDIUM', label: 'Medium', color: 'var(--severity-medium)' },
  { value: 'LOW', label: 'Low', color: 'var(--severity-low)' },
  { value: 'INFO', label: 'Info', color: 'var(--severity-info)' }
];

export const FindingFilters = ({
  selectedSeverity = 'ALL',
  onSeverityChange,
  selectedCategory = 'ALL',
  onCategoryChange,
  searchQuery = '',
  onSearchChange,
  viewMode = 'flat',
  onViewModeChange,
  onReset,
  stats = { total: 0, critical: 0, high: 0, medium: 0, low: 0, info: 0 },
  categoriesPresent = [],
  categoryCounts = {},
  hasActiveFilters = false
}) => {
  const getSeverityCount = (sev) => {
    switch (sev) {
      case 'ALL':
        return stats.total;
      case 'CRITICAL':
        return stats.critical;
      case 'HIGH':
        return stats.high;
      case 'MEDIUM':
        return stats.medium;
      case 'LOW':
        return stats.low;
      case 'INFO':
        return stats.info;
      default:
        return 0;
    }
  };

  return (
    <div className="card finding-filters-card" role="region" aria-label="Finding filters">
      {/* Top Filter Bar: Search, Category & View Mode */}
      <div className="finding-filters-top-row">
        {/* Search Input (filter by message or file) */}
        <div className="finding-search-field">
          <label htmlFor="finding-search-input" className="form-label">
            Search Findings
          </label>
          <div className="input-with-icon">
            <svg
              className="input-icon"
              width="14"
              height="14"
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
              id="finding-search-input"
              type="text"
              className="form-input"
              placeholder="Filter by file path or message..."
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
            />
            {searchQuery && (
              <button
                type="button"
                className="input-clear-btn"
                onClick={() => onSearchChange('')}
                title="Clear search query"
                aria-label="Clear search query"
              >
                ×
              </button>
            )}
          </div>
        </div>

        {/* Category Filter Select */}
        {categoriesPresent.length > 0 && (
          <div className="finding-category-field">
            <label htmlFor="finding-category-select" className="form-label">
              Category
            </label>
            <select
              id="finding-category-select"
              className="form-select finding-category-select"
              value={selectedCategory}
              onChange={(e) => onCategoryChange(e.target.value)}
            >
              <option value="ALL">All Categories ({stats.total})</option>
              {categoriesPresent.map((cat) => {
                const count = categoryCounts[cat] || 0;
                return (
                  <option key={cat} value={cat}>
                    {cat.replace('_', ' ')} ({count})
                  </option>
                );
              })}
            </select>
          </div>
        )}

        {/* View Mode Toggle: Flat List vs Grouped by File */}
        <div className="finding-view-mode-field">
          <label className="form-label">Grouping</label>
          <div className="view-mode-toggle" role="group" aria-label="Finding layout mode">
            <button
              type="button"
              className={`view-mode-btn ${viewMode === 'flat' ? 'active' : ''}`}
              onClick={() => onViewModeChange('flat')}
              title="View findings in flat sequential order"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <line x1="8" y1="6" x2="21" y2="6" />
                <line x1="8" y1="12" x2="21" y2="12" />
                <line x1="8" y1="18" x2="21" y2="18" />
                <line x1="3" y1="6" x2="3.01" y2="6" />
                <line x1="3" y1="12" x2="3.01" y2="12" />
                <line x1="3" y1="18" x2="3.01" y2="18" />
              </svg>
              <span>List</span>
            </button>

            <button
              type="button"
              className={`view-mode-btn ${viewMode === 'grouped' ? 'active' : ''}`}
              onClick={() => onViewModeChange('grouped')}
              title="Group findings by file path"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
              </svg>
              <span>By File</span>
            </button>
          </div>
        </div>

        {/* Clear Filters Action */}
        {hasActiveFilters && (
          <div className="finding-reset-field">
            <button
              type="button"
              className="btn btn-outline btn-sm finding-reset-btn"
              onClick={onReset}
              title="Reset all active filters"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="1 4 1 10 7 10" />
                <polyline points="23 20 23 14 17 14" />
                <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
              </svg>
              <span>Reset</span>
            </button>
          </div>
        )}
      </div>

      {/* Severity Filter Pills Row */}
      <div className="finding-severity-pills-row">
        <span className="finding-severity-filter-label">Severity:</span>
        <div className="finding-severity-pills-container" role="radiogroup" aria-label="Filter findings by severity">
          {SEVERITY_OPTIONS.map((opt) => {
            const count = getSeverityCount(opt.value);
            const isActive = selectedSeverity === opt.value;

            return (
              <button
                key={opt.value}
                type="button"
                role="radio"
                aria-checked={isActive}
                className={`finding-severity-pill ${isActive ? 'active' : ''}`}
                onClick={() => onSeverityChange(opt.value)}
              >
                {opt.color && (
                  <span
                    className="severity-pill-dot"
                    style={{ backgroundColor: opt.color }}
                    aria-hidden="true"
                  />
                )}
                <span>{opt.label}</span>
                <span className="severity-pill-count">{count}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Active Filter Chips */}
      {hasActiveFilters && (
        <div className="finding-active-chips-row">
          <span className="active-chips-label">Active:</span>
          <div className="active-chips-list">
            {selectedSeverity !== 'ALL' && (
              <span className="active-chip">
                <span>Severity: <strong>{selectedSeverity}</strong></span>
                <button
                  type="button"
                  className="active-chip-remove"
                  onClick={() => onSeverityChange('ALL')}
                  aria-label="Remove severity filter"
                >
                  ×
                </button>
              </span>
            )}

            {selectedCategory !== 'ALL' && (
              <span className="active-chip">
                <span>Category: <strong>{selectedCategory.replace('_', ' ')}</strong></span>
                <button
                  type="button"
                  className="active-chip-remove"
                  onClick={() => onCategoryChange('ALL')}
                  aria-label="Remove category filter"
                >
                  ×
                </button>
              </span>
            )}

            {searchQuery.trim() && (
              <span className="active-chip">
                <span>Search: <strong>"{searchQuery.trim()}"</strong></span>
                <button
                  type="button"
                  className="active-chip-remove"
                  onClick={() => onSearchChange('')}
                  aria-label="Remove search filter"
                >
                  ×
                </button>
              </span>
            )}

            <button
              type="button"
              className="clear-all-chips-btn"
              onClick={onReset}
            >
              Clear all
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FindingFilters;
