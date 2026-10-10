import React, { useState } from 'react';
import { Link } from 'react-router-dom';

const getHealthBadgeStyle = (status) => {
  switch (status) {
    case 'NO_REVIEWS':
      return {
        bg: 'rgba(100, 116, 139, 0.15)',
        color: 'var(--text-muted, #94a3b8)',
        border: '1px solid rgba(100, 116, 139, 0.3)'
      };
    case 'NO_FINDINGS_RECORDED':
      return {
        bg: 'var(--status-completed-bg, rgba(16, 185, 129, 0.15))',
        color: 'var(--status-completed, #10b981)',
        border: '1px solid rgba(16, 185, 129, 0.3)'
      };
    case 'COMPLETED_WITH_FINDINGS':
    default:
      return {
        bg: 'rgba(249, 115, 22, 0.15)',
        color: 'var(--severity-high, #f97316)',
        border: '1px solid rgba(249, 115, 22, 0.3)'
      };
  }
};

export const RepositoryHealthTable = ({
  repositories = [],
  onSelectRepository
}) => {
  const [search, setSearch] = useState('');

  const filtered = repositories.filter((r) => {
    if (!search.trim()) return true;
    const term = search.trim().toLowerCase();
    const full = (r.repository || `${r.owner}/${r.name}`).toLowerCase();
    return full.includes(term);
  });

  const formatDate = (isoStr) => {
    if (!isoStr) return 'Never reviewed';
    const date = new Date(isoStr);
    return date.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="card" style={{ padding: '1.5rem' }}>
      <div className="card-header" style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 className="card-title" style={{ fontSize: '1.125rem', fontWeight: 600 }}>
            Repository Health & Review Activity
          </h2>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Factual repository metrics, review volume, and finding distribution
          </span>
        </div>

        {/* Search input */}
        <div style={{ minWidth: '220px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="Filter repositories..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', height: '32px' }}
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <div style={{ padding: '2.5rem 1rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          {repositories.length === 0
            ? 'No repositories available for the selected filters.'
            : `No repositories match "${search}".`}
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ minWidth: '180px' }}>Repository</th>
                <th style={{ minWidth: '170px' }}>Health Status</th>
                <th>Reviews</th>
                <th>Total Findings</th>
                <th style={{ minWidth: '160px' }}>Findings by Severity</th>
                <th style={{ minWidth: '140px' }}>Last Reviewed</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((repo) => {
                const badgeStyle = getHealthBadgeStyle(repo.healthStatus);
                const repoIdentifier = repo.repository || `${repo.owner}/${repo.name}`;

                return (
                  <tr key={repoIdentifier}>
                    {/* Repository Name */}
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                          {repoIdentifier}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {repo.owner}
                        </span>
                      </div>
                    </td>

                    {/* Health Status Badge */}
                    <td>
                      <span
                        className="badge"
                        style={{
                          backgroundColor: badgeStyle.bg,
                          color: badgeStyle.color,
                          border: badgeStyle.border,
                          fontWeight: 600,
                          fontSize: '0.75rem',
                          padding: '0.25rem 0.6rem',
                          borderRadius: 'var(--radius-full)'
                        }}
                        title={repo.healthStatusDescription}
                      >
                        {repo.healthStatusDescription || repo.healthStatus}
                      </span>
                    </td>

                    {/* Review Counts */}
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', fontSize: '0.8125rem' }}>
                        <strong style={{ color: 'var(--text-primary)' }}>{repo.totalReviews}</strong>
                        <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                          {repo.completedReviews} completed{repo.failedReviews > 0 ? `, ${repo.failedReviews} failed` : ''}
                        </span>
                      </div>
                    </td>

                    {/* Total Findings */}
                    <td>
                      <span style={{ fontSize: '0.875rem', fontWeight: 600, color: repo.totalFindings > 0 ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                        {repo.totalFindings}
                      </span>
                    </td>

                    {/* Severity Pills */}
                    <td>
                      {repo.totalFindings > 0 ? (
                        <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', alignItems: 'center' }}>
                          {repo.criticalFindings > 0 && (
                            <span
                              className="badge"
                              style={{ backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--severity-critical)', fontSize: '0.6875rem', padding: '0.15rem 0.4rem' }}
                              title="Critical Findings"
                            >
                              Crit: {repo.criticalFindings}
                            </span>
                          )}
                          {repo.highFindings > 0 && (
                            <span
                              className="badge"
                              style={{ backgroundColor: 'rgba(249, 115, 22, 0.2)', color: 'var(--severity-high)', fontSize: '0.6875rem', padding: '0.15rem 0.4rem' }}
                              title="High Findings"
                            >
                              High: {repo.highFindings}
                            </span>
                          )}
                          {repo.mediumFindings > 0 && (
                            <span
                              className="badge"
                              style={{ backgroundColor: 'rgba(234, 179, 8, 0.2)', color: 'var(--severity-medium)', fontSize: '0.6875rem', padding: '0.15rem 0.4rem' }}
                              title="Medium Findings"
                            >
                              Med: {repo.mediumFindings}
                            </span>
                          )}
                          {repo.lowFindings > 0 && (
                            <span
                              className="badge"
                              style={{ backgroundColor: 'rgba(59, 130, 246, 0.2)', color: 'var(--severity-low)', fontSize: '0.6875rem', padding: '0.15rem 0.4rem' }}
                              title="Low Findings"
                            >
                              Low: {repo.lowFindings}
                            </span>
                          )}
                          {repo.infoFindings > 0 && (
                            <span
                              className="badge"
                              style={{ backgroundColor: 'rgba(100, 116, 139, 0.2)', color: 'var(--severity-info)', fontSize: '0.6875rem', padding: '0.15rem 0.4rem' }}
                              title="Info Findings"
                            >
                              Info: {repo.infoFindings}
                            </span>
                          )}
                        </div>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>—</span>
                      )}
                    </td>

                    {/* Last Reviewed */}
                    <td style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                      {formatDate(repo.lastReviewAt)}
                    </td>

                    {/* Actions */}
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', alignItems: 'center' }}>
                        {onSelectRepository && (
                          <button
                            type="button"
                            className="btn btn-outline btn-xs"
                            onClick={() => onSelectRepository(repoIdentifier)}
                            title="Filter analytics by this repository"
                          >
                            Filter
                          </button>
                        )}
                        <Link
                          to={`/reviews?repository=${encodeURIComponent(repo.name || repoIdentifier)}`}
                          className="btn btn-secondary btn-xs"
                          title="View review history for this repository"
                        >
                          Reviews →
                        </Link>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default RepositoryHealthTable;
