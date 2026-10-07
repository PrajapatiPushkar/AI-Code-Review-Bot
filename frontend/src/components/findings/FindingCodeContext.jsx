import React, { useState } from 'react';
import useToast from '../../hooks/useToast';

/**
 * FindingCodeContext displays file path, line numbers, and available source context.
 * If actual source code context is not provided by the review API, it honestly states
 * that source context is unavailable rather than fabricating code.
 */
export const FindingCodeContext = ({
  finding,
  className = ''
}) => {
  const { toast } = useToast();
  const [copiedLocation, setCopiedLocation] = useState(false);

  if (!finding) return null;

  const { filePath, lineNumber, endLineNumber, sourceContext, codeSnippet } = finding;
  const snippet = sourceContext || codeSnippet || null;

  const getLineDisplay = () => {
    if (lineNumber && endLineNumber && endLineNumber > lineNumber) {
      return `Lines ${lineNumber}–${endLineNumber}`;
    }
    if (lineNumber) {
      return `Line ${lineNumber}`;
    }
    return 'General';
  };

  const getLocationString = () => {
    if (!filePath) return '';
    if (lineNumber && endLineNumber && endLineNumber > lineNumber) {
      return `${filePath}:${lineNumber}-${endLineNumber}`;
    }
    if (lineNumber) {
      return `${filePath}:${lineNumber}`;
    }
    return filePath;
  };

  const handleCopyLocation = async () => {
    const loc = getLocationString();
    if (!loc) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(loc);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = loc;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedLocation(true);
      if (toast && toast.success) {
        toast.success(`Copied location: ${loc}`, { title: 'Location Copied' });
      }
      setTimeout(() => setCopiedLocation(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy location to clipboard.', { title: 'Error' });
      }
    }
  };

  return (
    <div
      className={`finding-code-context-container ${className}`}
      role="region"
      aria-label="Code location and context"
    >
      <div className="finding-context-header">
        <div className="finding-context-title-group">
          <svg
            className="finding-context-icon"
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
            <polyline points="16 18 22 12 16 6" />
            <polyline points="8 6 2 12 8 18" />
          </svg>
          <span className="finding-context-heading">Code Location</span>
        </div>

        {filePath && (
          <button
            type="button"
            className="btn btn-outline btn-sm finding-location-copy-btn"
            onClick={handleCopyLocation}
            title="Copy file path and line location"
            aria-label={`Copy location ${getLocationString()}`}
          >
            {copiedLocation ? (
              <>
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="var(--status-completed)"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span style={{ color: 'var(--status-completed)' }}>Copied!</span>
              </>
            ) : (
              <>
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
                <span>Copy Location</span>
              </>
            )}
          </button>
        )}
      </div>

      <div className="finding-context-body">
        <div className="finding-context-meta-grid">
          <div className="finding-context-meta-row">
            <span className="finding-context-label">File:</span>
            <code className="finding-context-value finding-context-file">
              {filePath || 'Unspecified / Repository-wide'}
            </code>
          </div>
          <div className="finding-context-meta-row">
            <span className="finding-context-label">
              {lineNumber && endLineNumber && endLineNumber > lineNumber ? 'Lines:' : 'Line:'}
            </span>
            <code className="finding-context-value finding-context-lines">
              {getLineDisplay()}
            </code>
          </div>
        </div>

        {snippet ? (
          <div className="finding-context-snippet-wrapper">
            <pre className="finding-context-snippet">
              <code>{snippet}</code>
            </pre>
          </div>
        ) : (
          <div className="finding-context-unavailable" role="note">
            <svg
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
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span>Source context is not available from the review API.</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default FindingCodeContext;
