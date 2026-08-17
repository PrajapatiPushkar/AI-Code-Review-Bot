import React from 'react';

const FindingCard = ({ finding }) => {
  if (!finding) return null;

  const severity = (finding.severity || 'INFO').toUpperCase();
  const category = (finding.category || 'OTHER').toUpperCase();

  const getLineDisplay = () => {
    if (finding.lineNumber && finding.endLineNumber && finding.endLineNumber > finding.lineNumber) {
      return `Lines ${finding.lineNumber}-${finding.endLineNumber}`;
    }
    if (finding.lineNumber) {
      return `Line ${finding.lineNumber}`;
    }
    return 'General';
  };

  const getBorderColor = () => {
    switch (severity) {
      case 'CRITICAL':
        return 'var(--severity-critical)';
      case 'HIGH':
        return 'var(--severity-high)';
      case 'MEDIUM':
        return 'var(--severity-medium)';
      case 'LOW':
        return 'var(--severity-low)';
      default:
        return 'var(--border-color)';
    }
  };

  return (
    <div
      className="card"
      style={{
        marginBottom: '1.25rem',
        borderLeft: `4px solid ${getBorderColor()}`,
        transition: 'transform 0.2s, box-shadow 0.2s'
      }}
    >
      <div className="card-header" style={{ marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <span className={`badge badge-${severity.toLowerCase()}`}>{severity}</span>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: '600',
              color: 'var(--text-secondary)',
              backgroundColor: 'var(--bg-surface-hover)',
              padding: '0.2rem 0.5rem',
              borderRadius: '4px',
              border: '1px solid var(--border-color)'
            }}
          >
            {category}
          </span>
          <code style={{ fontSize: '0.875rem', color: 'var(--primary-color)', fontWeight: '600' }}>
            {finding.filePath}
          </code>
        </div>
        <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontWeight: '500' }}>
          📍 {getLineDisplay()}
        </span>
      </div>

      <div style={{ marginBottom: '1rem' }}>
        <p style={{ color: 'var(--text-primary)', fontSize: '0.9375rem', lineHeight: '1.6' }}>
          {finding.message}
        </p>
      </div>

      {finding.suggestion && (
        <div
          style={{
            backgroundColor: 'var(--bg-color)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius)',
            padding: '1rem',
            marginTop: '0.75rem'
          }}
        >
          <div style={{ fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem', letterSpacing: '0.05em' }}>
            💡 AI Recommendation / Code Fix
          </div>
          <pre
            style={{
              fontFamily: 'monospace',
              fontSize: '0.875rem',
              color: '#a5f3fc',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              margin: 0
            }}
          >
            {finding.suggestion}
          </pre>
        </div>
      )}
    </div>
  );
};

export default FindingCard;
