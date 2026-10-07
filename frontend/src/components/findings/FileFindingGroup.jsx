import React, { useState } from 'react';
import FindingCard from './FindingCard';
import SeverityBadge from './SeverityBadge';
import useToast from '../../hooks/useToast';

const SEVERITY_WEIGHT = {
  CRITICAL: 5,
  HIGH: 4,
  MEDIUM: 3,
  LOW: 2,
  INFO: 1
};

export const FileFindingGroup = ({
  filePath,
  findings = [],
  defaultExpanded = true,
  repoMetadata = null
}) => {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const { toast } = useToast();
  const [copied, setCopied] = useState(false);

  // Compute highest severity in this file
  const highestSeverity = findings.reduce((max, f) => {
    const sev = (f.severity || 'INFO').toUpperCase();
    const weight = SEVERITY_WEIGHT[sev] || 0;
    const maxWeight = SEVERITY_WEIGHT[max] || 0;
    return weight > maxWeight ? sev : max;
  }, 'INFO');

  const handleCopyPath = async (e) => {
    e.stopPropagation();
    if (!filePath) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(filePath);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = filePath;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopied(true);
      if (toast && toast.success) {
        toast.success(`Copied path: ${filePath}`, { title: 'Path Copied' });
      }
      setTimeout(() => setCopied(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy file path to clipboard.', { title: 'Error' });
      }
    }
  };

  const handleToggle = () => {
    setIsExpanded((prev) => !prev);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleToggle();
    }
  };

  return (
    <div className="file-finding-group card">
      {/* File Group Header (Accessible collapsible header) */}
      <div
        className="file-finding-group-header"
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        role="button"
        tabIndex={0}
        aria-expanded={isExpanded}
        aria-controls={`file-findings-${filePath}`}
      >
        <div className="file-group-title-wrapper">
          <svg
            className={`file-group-chevron ${isExpanded ? 'expanded' : ''}`}
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
            <polyline points="9 18 15 12 9 6" />
          </svg>

          <svg
            className="file-group-icon"
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
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </svg>

          <code className="file-group-path" title={filePath}>
            {filePath || 'General / Unspecified File'}
          </code>

          {filePath && (
            <button
              type="button"
              className="file-group-copy-btn"
              onClick={handleCopyPath}
              title="Copy file path"
              aria-label={`Copy file path ${filePath}`}
            >
              {copied ? '✓' : '⧉'}
            </button>
          )}
        </div>

        <div className="file-group-meta">
          <SeverityBadge severity={highestSeverity} />
          <span className="file-group-count-badge">
            {findings.length} {findings.length === 1 ? 'finding' : 'findings'}
          </span>
        </div>
      </div>

      {/* Collapsible Findings Body */}
      {isExpanded && (
        <div id={`file-findings-${filePath}`} className="file-finding-group-body">
          {findings.map((finding) => (
            <FindingCard
              key={finding.id}
              finding={finding}
              repoMetadata={repoMetadata}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default FileFindingGroup;
