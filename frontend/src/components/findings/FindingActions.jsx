import React, { useState } from 'react';
import useToast from '../../hooks/useToast';

/**
 * FindingActions provides safe developer-assistance actions for a review finding:
 * - Copy Fix Suggestion
 * - Copy Location
 * - Open file on GitHub (only if owner, repository, commitSha, filePath, and lineNumber are all genuinely available)
 * - View / Hide Details expansion toggle
 */
export const FindingActions = ({
  finding,
  repoMetadata = null,
  isExpanded = false,
  onToggleExpand,
  controlsId,
  onGenerateFix,
  isGeneratingFix = false,
  hasFix = false,
  onToggleHistory,
  isHistoryOpen = false,
  className = ''
}) => {
  const { toast } = useToast();
  const [copiedSuggestion, setCopiedSuggestion] = useState(false);
  const [copiedLocation, setCopiedLocation] = useState(false);

  if (!finding) return null;

  const hasSuggestion = Boolean(finding.suggestion && finding.suggestion.trim().length > 0);
  const hasFilePath = Boolean(finding.filePath && finding.filePath.trim().length > 0);

  // Construct GitHub URL only if all required metadata is genuinely available
  const getGitHubUrl = () => {
    if (!repoMetadata || !finding) return null;
    const { owner, repository, commitSha } = repoMetadata;
    const { filePath, lineNumber, endLineNumber } = finding;

    if (!owner || !repository || !commitSha || !filePath || !lineNumber) {
      return null;
    }

    const cleanOwner = encodeURIComponent(owner.trim());
    const cleanRepo = encodeURIComponent(repository.trim());
    const cleanPath = filePath.trim().replace(/^\/+/, '');

    let lineFragment = `#L${lineNumber}`;
    if (endLineNumber && endLineNumber > lineNumber) {
      lineFragment = `#L${lineNumber}-L${endLineNumber}`;
    }

    return `https://github.com/${cleanOwner}/${cleanRepo}/blob/${commitSha}/${cleanPath}${lineFragment}`;
  };

  const githubUrl = getGitHubUrl();

  const handleCopySuggestion = async () => {
    if (!hasSuggestion) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(finding.suggestion);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = finding.suggestion;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedSuggestion(true);
      if (toast && toast.success) {
        toast.success('Fix suggestion copied to clipboard.', { title: 'Copied' });
      }
      setTimeout(() => setCopiedSuggestion(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy suggestion to clipboard.', { title: 'Error' });
      }
    }
  };

  const handleCopyLocation = async () => {
    if (!hasFilePath) return;
    let loc = finding.filePath;
    if (finding.lineNumber && finding.endLineNumber && finding.endLineNumber > finding.lineNumber) {
      loc = `${finding.filePath}:${finding.lineNumber}-${finding.endLineNumber}`;
    } else if (finding.lineNumber) {
      loc = `${finding.filePath}:${finding.lineNumber}`;
    }

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
      className={`finding-actions ${className}`}
      role="toolbar"
      aria-label="Finding actions"
    >
      {/* Generate Proposed AI Fix */}
      {onGenerateFix && (
        <button
          type="button"
          className="btn btn-sm btn-primary finding-action-btn finding-action-fix"
          onClick={onGenerateFix}
          disabled={isGeneratingFix || !hasFilePath}
          title={!hasFilePath ? 'Fix generation requires available file path and context.' : (hasFix ? 'Regenerate proposed patch with AI' : 'Request AI-generated proposed code fix')}
          aria-label={isGeneratingFix ? 'Generating proposed fix...' : (hasFix ? 'Regenerate proposed code fix with AI' : 'Generate proposed code fix with AI')}
        >
          {isGeneratingFix ? (
            <>
              <svg
                className="finding-action-spinner"
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <line x1="12" y1="2" x2="12" y2="6" />
                <line x1="12" y1="18" x2="12" y2="22" />
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76" />
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07" />
                <line x1="2" y1="12" x2="6" y2="12" />
                <line x1="18" y1="12" x2="22" y2="12" />
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24" />
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93" />
              </svg>
              <span>Generating proposed fix...</span>
            </>
          ) : (
            <>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
              </svg>
              <span>{hasFix ? 'Regenerate Fix' : 'Generate Fix'}</span>
            </>
          )}
        </button>
      )}

      {/* View Fix History */}
      {onToggleHistory && (
        <button
          type="button"
          className={`btn btn-sm ${isHistoryOpen ? 'btn-secondary' : 'btn-outline'} finding-action-btn finding-action-history`}
          onClick={onToggleHistory}
          title="View previous AI fix proposals for this finding"
          aria-label={isHistoryOpen ? 'Hide fix history' : 'View fix history'}
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span>Fix History</span>
        </button>
      )}

      {/* Toggle Details Action */}
      {onToggleExpand && (
        <button
          type="button"
          className="btn btn-sm btn-outline finding-action-btn finding-action-toggle"
          onClick={onToggleExpand}
          aria-expanded={isExpanded}
          aria-controls={controlsId}
        >
          <svg
            className={`finding-action-chevron ${isExpanded ? 'expanded' : ''}`}
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
            <polyline points="6 9 12 15 18 9" />
          </svg>
          <span>{isExpanded ? 'Hide Details' : 'View Details'}</span>
        </button>
      )}

      {/* Copy Fix Suggestion */}
      {hasSuggestion && (
        <button
          type="button"
          className="btn btn-sm btn-outline finding-action-btn"
          onClick={handleCopySuggestion}
          title="Copy suggested code fix to clipboard"
          aria-label="Copy fix suggestion to clipboard"
        >
          {copiedSuggestion ? (
            <>
              <svg
                width="13"
                height="13"
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
              <span style={{ color: 'var(--status-completed)' }}>Copied Fix</span>
            </>
          ) : (
            <>
              <svg
                width="13"
                height="13"
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
              <span>Copy Suggestion</span>
            </>
          )}
        </button>
      )}

      {/* Copy Location */}
      {hasFilePath && (
        <button
          type="button"
          className="btn btn-sm btn-outline finding-action-btn"
          onClick={handleCopyLocation}
          title="Copy file path and line number"
          aria-label="Copy file location to clipboard"
        >
          {copiedLocation ? (
            <>
              <svg
                width="13"
                height="13"
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
              <span style={{ color: 'var(--status-completed)' }}>Copied Location</span>
            </>
          ) : (
            <>
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <line x1="4" y1="9" x2="20" y2="9" />
                <line x1="4" y1="15" x2="20" y2="15" />
                <line x1="10" y1="3" x2="8" y2="21" />
                <line x1="16" y1="3" x2="14" y2="21" />
              </svg>
              <span>Copy Location</span>
            </>
          )}
        </button>
      )}

      {/* Open on GitHub (only if valid URL can be constructed) */}
      {githubUrl && (
        <a
          href={githubUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-sm btn-outline finding-action-btn finding-action-github"
          aria-label={`Open file on GitHub at line ${finding.lineNumber} (opens in new tab)`}
          title="View file and line on GitHub"
        >
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
          </svg>
          <span>Open on GitHub</span>
          <svg
            width="11"
            height="11"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            style={{ opacity: 0.7 }}
          >
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </a>
      )}
    </div>
  );
};

export default FindingActions;
