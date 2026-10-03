import React, { useState } from 'react';
import SeverityBadge from './SeverityBadge';
import CategoryBadge from './CategoryBadge';
import FindingCodeBlock from './FindingCodeBlock';
import useToast from '../../hooks/useToast';

export const FindingCard = ({ finding }) => {
  const { toast } = useToast();
  const [copiedPath, setCopiedPath] = useState(false);

  if (!finding) return null;

  const severity = (finding.severity || 'INFO').toUpperCase();

  const getLineDisplay = () => {
    if (finding.lineNumber && finding.endLineNumber && finding.endLineNumber > finding.lineNumber) {
      return `Lines ${finding.lineNumber}–${finding.endLineNumber}`;
    }
    if (finding.lineNumber) {
      return `Line ${finding.lineNumber}`;
    }
    return 'General';
  };

  const getSeverityBorderClass = () => {
    switch (severity) {
      case 'CRITICAL':
        return 'severity-border-critical';
      case 'HIGH':
        return 'severity-border-high';
      case 'MEDIUM':
        return 'severity-border-medium';
      case 'LOW':
        return 'severity-border-low';
      case 'INFO':
      default:
        return 'severity-border-info';
    }
  };

  const handleCopyPath = async () => {
    if (!finding.filePath) return;
    try {
      await navigator.clipboard.writeText(finding.filePath);
      setCopiedPath(true);
      if (toast && toast.success) {
        toast.success(`Copied path: ${finding.filePath}`, { title: 'Path Copied' });
      }
      setTimeout(() => setCopiedPath(false), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <article
      className={`card finding-card ${getSeverityBorderClass()}`}
      aria-label={`Finding #${finding.id || 'N/A'}: ${severity} in ${finding.filePath || 'general'}`}
    >
      {/* Finding Card Top Header */}
      <div className="finding-card-header">
        <div className="finding-header-badges">
          <SeverityBadge severity={finding.severity} />
          {finding.category && <CategoryBadge category={finding.category} />}

          {finding.filePath && (
            <div className="finding-file-pill" title={`File: ${finding.filePath}`}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <code className="finding-file-path">{finding.filePath}</code>
              <button
                type="button"
                className="finding-file-copy-btn"
                onClick={handleCopyPath}
                title="Copy file path"
                aria-label={`Copy file path ${finding.filePath}`}
              >
                {copiedPath ? '✓' : '⧉'}
              </button>
            </div>
          )}
        </div>

        <div className="finding-location-pill" title="Code Location">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <line x1="4" y1="9" x2="20" y2="9" />
            <line x1="4" y1="15" x2="20" y2="15" />
            <line x1="10" y1="3" x2="8" y2="21" />
            <line x1="16" y1="3" x2="14" y2="21" />
          </svg>
          <span>{getLineDisplay()}</span>
        </div>
      </div>

      {/* Finding Message / Description */}
      <div className="finding-card-body">
        <p className="finding-message">{finding.message}</p>
      </div>

      {/* Code Context / Suggested Fix Code Block */}
      {finding.suggestion && (
        <FindingCodeBlock
          suggestion={finding.suggestion}
          lineNumber={finding.lineNumber}
          endLineNumber={finding.endLineNumber}
          filePath={finding.filePath}
        />
      )}
    </article>
  );
};

export default FindingCard;
