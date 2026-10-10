import React, { useState, useEffect } from 'react';

export const AnalyticsFilters = ({
  initialFilters = { from: '', to: '', repository: '' },
  availableRepositories = [],
  onFilterChange,
  disabled = false
}) => {
  const [activePreset, setActivePreset] = useState('14d'); // 'all' | '7d' | '14d' | '30d' | 'custom'
  const [from, setFrom] = useState(initialFilters.from || '');
  const [to, setTo] = useState(initialFilters.to || '');
  const [repository, setRepository] = useState(initialFilters.repository || '');

  // Calculate formatted YYYY-MM-DD in UTC
  const formatUtcDate = (date) => {
    return date.toISOString().split('T')[0];
  };

  const applyPreset = (preset) => {
    setActivePreset(preset);
    const now = new Date();
    let newFrom = '';
    let newTo = formatUtcDate(now);

    if (preset === '7d') {
      const past = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000);
      newFrom = formatUtcDate(past);
    } else if (preset === '14d') {
      const past = new Date(now.getTime() - 13 * 24 * 60 * 60 * 1000);
      newFrom = formatUtcDate(past);
    } else if (preset === '30d') {
      const past = new Date(now.getTime() - 29 * 24 * 60 * 60 * 1000);
      newFrom = formatUtcDate(past);
    } else if (preset === 'all') {
      newFrom = '';
      newTo = '';
    }

    setFrom(newFrom);
    setTo(newTo);
    onFilterChange({ from: newFrom, to: newTo, repository });
  };

  const handleCustomDateChange = (newFrom, newTo) => {
    setActivePreset('custom');
    setFrom(newFrom);
    setTo(newTo);
    onFilterChange({ from: newFrom, to: newTo, repository });
  };

  const handleRepoChange = (newRepo) => {
    setRepository(newRepo);
    onFilterChange({ from, to, repository: newRepo });
  };

  const handleClear = () => {
    setActivePreset('14d');
    const now = new Date();
    const past = new Date(now.getTime() - 13 * 24 * 60 * 60 * 1000);
    const defFrom = formatUtcDate(past);
    const defTo = formatUtcDate(now);
    setFrom(defFrom);
    setTo(defTo);
    setRepository('');
    onFilterChange({ from: defFrom, to: defTo, repository: '' });
  };

  const isFiltered = activePreset !== '14d' || repository !== '';

  return (
    <div className="card analytics-filters-card" style={{ marginBottom: '1.5rem', padding: '1.25rem 1.5rem' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', justifyContent: 'space-between' }}>
        
        {/* Preset Range Pills */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)', marginRight: '0.25rem' }}>
            Period:
          </span>
          <button
            type="button"
            className={`btn btn-sm ${activePreset === '7d' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => applyPreset('7d')}
            disabled={disabled}
          >
            Last 7 Days
          </button>
          <button
            type="button"
            className={`btn btn-sm ${activePreset === '14d' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => applyPreset('14d')}
            disabled={disabled}
          >
            Last 14 Days
          </button>
          <button
            type="button"
            className={`btn btn-sm ${activePreset === '30d' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => applyPreset('30d')}
            disabled={disabled}
          >
            Last 30 Days
          </button>
          <button
            type="button"
            className={`btn btn-sm ${activePreset === 'all' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => applyPreset('all')}
            disabled={disabled}
          >
            All Time
          </button>
        </div>

        {/* Repository Filter & Clear Action */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          {/* Repository Selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label htmlFor="analytics-repo-select" style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Repository:
            </label>
            <select
              id="analytics-repo-select"
              className="form-control"
              value={repository}
              onChange={(e) => handleRepoChange(e.target.value)}
              disabled={disabled}
              style={{ minWidth: '180px', padding: '0.35rem 0.75rem', fontSize: '0.8125rem', height: '32px' }}
            >
              <option value="">All Repositories</option>
              {availableRepositories.map((repo) => {
                const name = typeof repo === 'string' ? repo : (repo.fullName || repo.name || repo.repository);
                return (
                  <option key={name} value={name}>
                    {name}
                  </option>
                );
              })}
            </select>
          </div>

          {/* Clear Filters Button */}
          {isFiltered && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleClear}
              disabled={disabled}
              title="Reset to default 14-day view"
            >
              Clear Filters
            </button>
          )}
        </div>
      </div>

      {/* Date Range Sub-row (Custom Inputs) */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)', flexWrap: 'wrap' }}>
        <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
          Date Range (UTC):
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label htmlFor="analytics-from-date" style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>From:</label>
          <input
            id="analytics-from-date"
            type="date"
            className="form-control"
            value={from}
            onChange={(e) => handleCustomDateChange(e.target.value, to)}
            disabled={disabled}
            style={{ padding: '0.25rem 0.5rem', fontSize: '0.8125rem', height: '30px' }}
          />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label htmlFor="analytics-to-date" style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>To:</label>
          <input
            id="analytics-to-date"
            type="date"
            className="form-control"
            value={to}
            onChange={(e) => handleCustomDateChange(from, e.target.value)}
            disabled={disabled}
            style={{ padding: '0.25rem 0.5rem', fontSize: '0.8125rem', height: '30px' }}
          />
        </div>
        {from && to && from > to && (
          <span style={{ color: 'var(--status-failed)', fontSize: '0.75rem', fontWeight: 600 }}>
            ⚠️ 'From' date must not be after 'To' date
          </span>
        )}
      </div>
    </div>
  );
};

export default AnalyticsFilters;
