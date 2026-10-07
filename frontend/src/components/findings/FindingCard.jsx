import React, { useState } from 'react';
import SeverityBadge from './SeverityBadge';
import CategoryBadge from './CategoryBadge';
import FindingSourceBadge from '../review-intelligence/FindingSourceBadge';
import FindingSuggestion from './FindingSuggestion';
import FindingCodeContext from './FindingCodeContext';
import FindingActions from './FindingActions';
import CodeFixPreview from './CodeFixPreview';
import reviewService from '../../services/reviewService';
import useToast from '../../hooks/useToast';

export const FindingCard = ({
  finding,
  repoMetadata = null,
  defaultExpanded = false
}) => {
  const { toast } = useToast();
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const [copiedPath, setCopiedPath] = useState(false);

  // AI Fix Generation local state
  const [proposedFix, setProposedFix] = useState(null);
  const [isGeneratingFix, setIsGeneratingFix] = useState(false);
  const [fixError, setFixError] = useState(null);

  if (!finding) return null;

  const severity = (finding.severity || 'INFO').toUpperCase();
  const isRule = (finding.source || '').toUpperCase() === 'RULE';
  const detailsId = `finding-details-${finding.id || Math.random().toString(36).substring(2, 9)}`;

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

  const handleCopyPath = async (e) => {
    e.stopPropagation();
    if (!finding.filePath) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(finding.filePath);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = finding.filePath;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedPath(true);
      if (toast && toast.success) {
        toast.success(`Copied path: ${finding.filePath}`, { title: 'Path Copied' });
      }
      setTimeout(() => setCopiedPath(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy file path to clipboard.', { title: 'Error' });
      }
    }
  };

  const handleGenerateFix = async () => {
    if (!finding?.id || isGeneratingFix) return;
    setIsGeneratingFix(true);
    setFixError(null);
    try {
      const fixResult = await reviewService.generateFindingFix(finding.id);
      setProposedFix(fixResult);
      if (toast && toast.success) {
        toast.success('AI-generated proposed patch is ready for review.', { title: 'Fix Generated' });
      }
      setIsExpanded(true);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to generate proposed code fix.';
      setFixError(msg);
      if (toast && toast.error) {
        toast.error(msg, { title: 'Fix Generation Failed' });
      }
    } finally {
      setIsGeneratingFix(false);
    }
  };

  return (
    <article
      className={`card finding-card ${getSeverityBorderClass()} ${isExpanded ? 'finding-card-expanded' : ''}`}
      aria-label={`Finding #${finding.id || 'N/A'}: ${severity} in ${finding.filePath || 'general'}`}
    >
      {/* Finding Card Top Header */}
      <div className="finding-card-header">
        <div className="finding-header-badges">
          <SeverityBadge severity={finding.severity} />
          {finding.category && <CategoryBadge category={finding.category} />}
          <FindingSourceBadge source={finding.source} ruleId={finding.ruleId} />

          {finding.ruleId && (
            <div className="finding-rule-pill" title={`Deterministic Rule ID: ${finding.ruleId}`}>
              <span className="finding-rule-label">Rule:</span>
              <code className="finding-rule-code">{finding.ruleId}</code>
            </div>
          )}

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

      {/* Finding Message / Description Summary */}
      <div className="finding-card-body">
        <p className="finding-message">{finding.message}</p>
      </div>

      {/* Primary Actions Bar (Expand toggle, Generate Fix, Copy Fix, Copy Location, GitHub Link) */}
      <div className="finding-card-actions-row">
        <FindingActions
          finding={finding}
          repoMetadata={repoMetadata}
          isExpanded={isExpanded}
          onToggleExpand={() => setIsExpanded(!isExpanded)}
          controlsId={detailsId}
          onGenerateFix={handleGenerateFix}
          isGeneratingFix={isGeneratingFix}
          hasFix={Boolean(proposedFix)}
        />
      </div>

      {/* Fix Generation Error Alert */}
      {fixError && (
        <div className="finding-fix-error-box" role="alert">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span className="finding-fix-error-message">{fixError}</span>
          <button
            type="button"
            className="btn btn-outline btn-sm finding-fix-retry-btn"
            onClick={handleGenerateFix}
          >
            Retry
          </button>
        </div>
      )}

      {/* AI Proposed Patch Preview Component */}
      {proposedFix && (
        <CodeFixPreview
          fix={proposedFix}
          finding={finding}
          onClose={() => setProposedFix(null)}
          onRegenerate={handleGenerateFix}
          isRegenerating={isGeneratingFix}
        />
      )}

      {/* Expanded Finding Detail Section */}
      {isExpanded && (
        <div id={detailsId} className="finding-expanded-details" role="region" aria-label="Finding details">
          {/* Why This Matters / Contextual Explanation */}
          <section className="finding-detail-section finding-explanation-section" aria-labelledby={`${detailsId}-explanation`}>
            <h4 id={`${detailsId}-explanation`} className="finding-detail-heading">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>Why this matters</span>
            </h4>
            <p className="finding-explanation-text">
              {finding.message || 'No additional contextual explanation was provided.'}
            </p>
          </section>

          {/* Code Location & Source Context */}
          <section className="finding-detail-section" aria-labelledby={`${detailsId}-context`}>
            <FindingCodeContext finding={finding} />
          </section>

          {/* Finding Provenance & Source */}
          <section className="finding-detail-section finding-provenance-section" aria-labelledby={`${detailsId}-source`}>
            <div className="finding-provenance-grid">
              <div className="finding-provenance-item">
                <span className="finding-provenance-label">Finding Source:</span>
                <span className="finding-provenance-value">
                  {isRule ? 'Deterministic Rule Engine' : 'AI Review (Gemini Contextual Reasoning)'}
                </span>
              </div>
              {isRule && finding.ruleId && (
                <div className="finding-provenance-item">
                  <span className="finding-provenance-label">Rule Identifier:</span>
                  <code className="finding-provenance-code">{finding.ruleId}</code>
                </div>
              )}
            </div>
          </section>

          {/* Suggested Fix Section */}
          <section className="finding-detail-section" aria-labelledby={`${detailsId}-fix`}>
            <FindingSuggestion
              suggestion={finding.suggestion}
              lineNumber={finding.lineNumber}
              endLineNumber={finding.endLineNumber}
            />
          </section>
        </div>
      )}
    </article>
  );
};

export default FindingCard;
