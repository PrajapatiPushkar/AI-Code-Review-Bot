import React from 'react';
import SeverityBadge from '../findings/SeverityBadge';

const SEVERITY_LEVELS = [
  { key: 'CRITICAL', label: 'Critical', color: 'var(--severity-critical)' },
  { key: 'HIGH', label: 'High', color: 'var(--severity-high)' },
  { key: 'MEDIUM', label: 'Medium', color: 'var(--severity-medium)' },
  { key: 'LOW', label: 'Low', color: 'var(--severity-low)' },
  { key: 'INFO', label: 'Info', color: 'var(--severity-info)' }
];

export const SeverityBreakdown = ({ findings = [] }) => {
  const counts = {
    CRITICAL: 0,
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0,
    INFO: 0
  };

  findings.forEach((f) => {
    const sev = (f.severity || 'INFO').toUpperCase();
    if (counts[sev] !== undefined) {
      counts[sev]++;
    } else {
      counts.INFO++;
    }
  });

  const total = findings.length;

  return (
    <div className="card severity-breakdown-card" role="region" aria-label="Severity distribution">
      <div className="severity-breakdown-header">
        <h3 className="card-title severity-breakdown-title">
          <svg
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
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span>Severity Distribution</span>
        </h3>
        <span className="severity-breakdown-total" aria-label={`Total: ${total} findings`}>
          {total} {total === 1 ? 'Finding' : 'Findings'}
        </span>
      </div>

      {/* Multi-segment distribution progress bar */}
      {total > 0 ? (
        <div className="severity-multi-bar" role="meter" aria-valuenow={total} aria-valuemin="0" aria-valuemax={total} aria-label="Severity proportion bar">
          {SEVERITY_LEVELS.map((level) => {
            const count = counts[level.key];
            if (count === 0) return null;
            const percentage = ((count / total) * 100).toFixed(1);
            return (
              <div
                key={level.key}
                className="severity-bar-segment"
                style={{
                  width: `${percentage}%`,
                  backgroundColor: level.color
                }}
                title={`${level.label}: ${count} (${percentage}%)`}
              />
            );
          })}
        </div>
      ) : (
        <div className="severity-empty-bar" />
      )}

      {/* Rows for each severity tier */}
      <div className="severity-breakdown-list">
        {SEVERITY_LEVELS.map((level) => {
          const count = counts[level.key];
          const percentage = total > 0 ? Math.round((count / total) * 100) : 0;

          return (
            <div key={level.key} className="severity-breakdown-row">
              <div className="severity-row-badge-wrap">
                <SeverityBadge severity={level.key} />
              </div>

              <div className="severity-row-bar-track">
                <div
                  className="severity-row-bar-fill"
                  style={{
                    width: `${percentage}%`,
                    backgroundColor: level.color
                  }}
                  aria-hidden="true"
                />
              </div>

              <div className="severity-row-counts">
                <strong className="severity-row-number">{count}</strong>
                <span className="severity-row-percent">({percentage}%)</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default SeverityBreakdown;
