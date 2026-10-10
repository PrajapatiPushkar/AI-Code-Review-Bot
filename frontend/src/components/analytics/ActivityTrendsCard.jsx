import React, { useState } from 'react';

export const ActivityTrendsCard = ({ trends = [] }) => {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  if (!trends || trends.length === 0) {
    return (
      <div className="card" style={{ padding: '1.5rem', marginBottom: '1.75rem', textAlign: 'center' }}>
        <h2 className="card-title" style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.75rem' }}>
          Review Activity Trends
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          No review activity recorded for the selected period.
        </p>
      </div>
    );
  }

  // Find max review count for proportional scaling
  const maxReviews = Math.max(...trends.map((t) => t.totalReviews || 0), 1);
  const maxFindings = Math.max(...trends.map((t) => t.totalFindings || 0), 1);

  // Summary counts
  const totalPeriodReviews = trends.reduce((acc, t) => acc + (t.totalReviews || 0), 0);
  const totalPeriodFindings = trends.reduce((acc, t) => acc + (t.totalFindings || 0), 0);

  // SVG dimensions
  const height = 180;
  const paddingBottom = 30;
  const chartHeight = height - paddingBottom;

  return (
    <div className="card" style={{ padding: '1.5rem', marginBottom: '1.75rem' }}>
      <div className="card-header" style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <h2 className="card-title" style={{ fontSize: '1.125rem', fontWeight: 600 }}>
            Review Activity & Findings Over Time
          </h2>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Daily review submissions and finding discovery rate
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8125rem' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--primary-color)' }} />
            Completed ({trends.reduce((acc, t) => acc + (t.completedReviews || 0), 0)})
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: 'var(--status-failed)' }} />
            Failed ({trends.reduce((acc, t) => acc + (t.failedReviews || 0), 0)})
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--severity-high)' }} />
            Findings ({totalPeriodFindings})
          </span>
        </div>
      </div>

      {totalPeriodReviews === 0 ? (
        <div style={{ padding: '2.5rem 1rem', textAlign: 'center', color: 'var(--text-secondary)', backgroundColor: 'var(--bg-surface-elevated)', borderRadius: 'var(--radius)' }}>
          <p style={{ margin: 0, fontSize: '0.875rem' }}>
            Zero reviews executed between {trends[0]?.date} and {trends[trends.length - 1]?.date}.
          </p>
        </div>
      ) : (
        <div style={{ position: 'relative', marginTop: '1rem' }}>
          {/* SVG Bar & Sparkline Trend */}
          <div style={{ width: '100%', overflowX: 'auto' }}>
            <div style={{ minWidth: `${Math.max(trends.length * 32, 400)}px`, height: `${height}px`, display: 'flex', alignItems: 'flex-end', gap: '4px', paddingBottom: `${paddingBottom}px`, position: 'relative', borderBottom: '1px solid var(--border-color)' }}>
              
              {trends.map((day, idx) => {
                const total = day.totalReviews || 0;
                const completed = day.completedReviews || 0;
                const failed = day.failedReviews || 0;
                const inProg = day.inProgressReviews || 0;
                const findings = day.totalFindings || 0;

                const barHeightPct = total > 0 ? Math.max((total / maxReviews) * 100, 8) : 0;
                const completedPct = total > 0 ? (completed / total) * 100 : 0;
                const failedPct = total > 0 ? (failed / total) * 100 : 0;
                const inProgPct = total > 0 ? (inProg / total) * 100 : 0;

                const isHovered = hoveredIndex === idx;

                // Format short date (e.g. "Oct 05")
                const dateParts = day.date.split('-');
                const shortDate = `${dateParts[1]}/${dateParts[2]}`;

                return (
                  <div
                    key={day.date}
                    style={{
                      flex: 1,
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'flex-end',
                      alignItems: 'center',
                      position: 'relative',
                      cursor: 'pointer'
                    }}
                    onMouseEnter={() => setHoveredIndex(idx)}
                    onMouseLeave={() => setHoveredIndex(null)}
                  >
                    {/* Findings Marker Dot on Top */}
                    {findings > 0 && (
                      <div
                        title={`${findings} findings`}
                        style={{
                          width: '6px',
                          height: '6px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--severity-high)',
                          marginBottom: '4px'
                        }}
                      />
                    )}

                    {/* Stacked Bar */}
                    <div
                      style={{
                        width: '75%',
                        height: `${barHeightPct}%`,
                        minHeight: total > 0 ? '8px' : '2px',
                        backgroundColor: total > 0 ? 'transparent' : 'var(--bg-surface-elevated)',
                        borderRadius: '3px 3px 0 0',
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column-reverse',
                        opacity: isHovered ? 1 : 0.85,
                        outline: isHovered ? '2px solid var(--primary-color)' : 'none',
                        transition: 'opacity 150ms ease, height 250ms ease'
                      }}
                    >
                      {/* Completed Segment */}
                      <div style={{ width: '100%', height: `${completedPct}%`, backgroundColor: 'var(--primary-color)' }} />
                      {/* In-Progress Segment */}
                      <div style={{ width: '100%', height: `${inProgPct}%`, backgroundColor: 'var(--status-in-progress)' }} />
                      {/* Failed Segment */}
                      <div style={{ width: '100%', height: `${failedPct}%`, backgroundColor: 'var(--status-failed)' }} />
                    </div>

                    {/* X-axis Date Label */}
                    <span
                      style={{
                        position: 'absolute',
                        bottom: `-${paddingBottom - 6}px`,
                        fontSize: '0.6875rem',
                        color: isHovered ? 'var(--text-primary)' : 'var(--text-muted)',
                        fontWeight: isHovered ? 600 : 400,
                        whiteSpace: 'nowrap'
                      }}
                    >
                      {shortDate}
                    </span>

                    {/* Hover Tooltip Popup */}
                    {isHovered && (
                      <div
                        style={{
                          position: 'absolute',
                          bottom: '105%',
                          backgroundColor: 'var(--bg-surface-elevated)',
                          border: '1px solid var(--border-color)',
                          boxShadow: 'var(--shadow-md)',
                          borderRadius: 'var(--radius)',
                          padding: '0.5rem 0.75rem',
                          zIndex: 10,
                          pointerEvents: 'none',
                          minWidth: '140px',
                          fontSize: '0.75rem',
                          color: 'var(--text-primary)'
                        }}
                      >
                        <div style={{ fontWeight: 600, borderBottom: '1px solid var(--border-color)', paddingBottom: '0.25rem', marginBottom: '0.25rem' }}>
                          {day.date}
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                          <span>Reviews:</span>
                          <strong>{total}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--status-completed)' }}>
                          <span>Completed:</span>
                          <strong>{completed}</strong>
                        </div>
                        {failed > 0 && (
                          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--status-failed)' }}>
                            <span>Failed:</span>
                            <strong>{failed}</strong>
                          </div>
                        )}
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--severity-high)' }}>
                          <span>Findings:</span>
                          <strong>{findings}</strong>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ActivityTrendsCard;
