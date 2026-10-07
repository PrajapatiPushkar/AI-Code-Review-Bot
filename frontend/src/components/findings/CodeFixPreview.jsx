import React, { useState, useEffect, useCallback } from 'react';
import useToast from '../../hooks/useToast';
import reviewService from '../../services/reviewService';
import LoadingSkeleton from '../common/LoadingSkeleton';

/**
 * CodeFixPreview displays an AI-generated patch proposal for a finding,
 * its persistent history, safe status lifecycle transitions, and patch inspection.
 *
 * Prominently communicates that fixes are NOT applied to GitHub automatically.
 */
export const CodeFixPreview = ({
  fix = null,
  finding,
  onClose,
  onRegenerate,
  isRegenerating = false,
  className = ''
}) => {
  const { toast } = useToast();

  const [proposals, setProposals] = useState([]);
  const [selectedProposalId, setSelectedProposalId] = useState(fix?.proposalId || null);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [copiedPatch, setCopiedPatch] = useState(false);
  const [activeTab, setActiveTab] = useState('diff'); // 'diff' | 'original' | 'proposed'

  const findingId = finding?.id || fix?.findingId;

  // Load persisted fix proposals for this finding
  const loadProposals = useCallback(async () => {
    if (!findingId) return;
    setIsLoadingHistory(true);
    setHistoryError(null);
    try {
      const data = await reviewService.getFindingFixProposals(findingId);
      const list = Array.isArray(data) ? data : [];
      setProposals(list);

      // If user has a newly generated fix with proposalId, ensure it is selected
      if (fix?.proposalId) {
        setSelectedProposalId(fix.proposalId);
      } else if (list.length > 0 && !selectedProposalId) {
        setSelectedProposalId(list[0].id);
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to load fix history.';
      setHistoryError(msg);
    } finally {
      setIsLoadingHistory(false);
    }
  }, [findingId, fix?.proposalId, selectedProposalId]);

  useEffect(() => {
    loadProposals();
  }, [loadProposals]);

  // When a new fix is generated externally, update local selection
  useEffect(() => {
    if (fix?.proposalId) {
      setSelectedProposalId(fix.proposalId);
      // Prepend or refresh proposal in list if not present
      setProposals((prev) => {
        const exists = prev.some((p) => p.id === fix.proposalId);
        if (!exists) {
          const newProp = {
            id: fix.proposalId,
            findingId: fix.findingId,
            filePath: fix.filePath,
            explanation: fix.explanation,
            unifiedDiff: fix.unifiedDiff,
            originalContent: fix.originalContent,
            proposedContent: fix.proposedContent,
            provider: fix.provider,
            model: fix.model || 'gemini-3.6-flash',
            developerInstructions: fix.developerInstructions,
            status: fix.status || 'PROPOSED',
            createdAt: fix.generatedAt || new Date().toISOString(),
            limitations: fix.limitations
          };
          return [newProp, ...prev];
        }
        return prev;
      });
    }
  }, [fix]);

  // Resolve currently active proposal to inspect
  const activeProposal = React.useMemo(() => {
    if (selectedProposalId && proposals.length > 0) {
      const found = proposals.find((p) => p.id === selectedProposalId);
      if (found) return found;
    }
    if (fix) return fix;
    if (proposals.length > 0) return proposals[0];
    return null;
  }, [selectedProposalId, proposals, fix]);

  const proposalId = activeProposal?.id || activeProposal?.proposalId;
  const status = (activeProposal?.status || 'PROPOSED').toUpperCase();
  const unifiedDiff = activeProposal?.unifiedDiff;
  const originalContent = activeProposal?.originalContent;
  const proposedContent = activeProposal?.proposedContent;
  const explanation = activeProposal?.explanation;
  const limitations = activeProposal?.limitations;
  const filePath = activeProposal?.filePath || finding?.filePath;
  const provider = activeProposal?.provider || 'Gemini';
  const model = activeProposal?.model || 'gemini-3.6-flash';
  const instructions = activeProposal?.developerInstructions;
  const isRule = (finding?.source || '').toUpperCase() === 'RULE';

  // Handle patch clipboard copy
  const handleCopyPatch = async () => {
    if (!unifiedDiff) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(unifiedDiff);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = unifiedDiff;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedPatch(true);
      if (toast && toast.success) {
        toast.success('Unified patch copied to clipboard.', { title: 'Copied' });
      }
      setTimeout(() => setCopiedPatch(false), 2000);
    } catch {
      if (toast && toast.error) {
        toast.error('Failed to copy patch to clipboard.', { title: 'Error' });
      }
    }
  };

  // Safe status transition handler
  const handleUpdateStatus = async (newStatus) => {
    if (!proposalId || isUpdatingStatus) return;
    setIsUpdatingStatus(true);
    try {
      const updated = await reviewService.updateFixProposalStatus(proposalId, newStatus);
      // Update in local proposals list
      setProposals((prev) =>
        prev.map((p) => (p.id === proposalId ? { ...p, status: updated.status } : p))
      );
      if (toast && toast.success) {
        toast.success(`Proposal #${proposalId} marked as ${newStatus.toLowerCase()}.`, {
          title: 'Status Updated'
        });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to update proposal status.';
      if (toast && toast.error) {
        toast.error(msg, { title: 'Status Transition Error' });
      }
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  // Format ISO timestamp nicely
  const formatDateTime = (timestamp) => {
    if (!timestamp) return 'Just now';
    try {
      const d = new Date(timestamp);
      return d.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return timestamp;
    }
  };

  // Status badge styling helper
  const getStatusBadge = (st) => {
    const s = (st || 'PROPOSED').toUpperCase();
    switch (s) {
      case 'REVIEWED':
        return <span className="code-fix-status-badge status-reviewed">✓ Reviewed</span>;
      case 'REJECTED':
        return <span className="code-fix-status-badge status-rejected">✕ Rejected</span>;
      case 'EXPIRED':
        return <span className="code-fix-status-badge status-expired">⌛ Expired</span>;
      case 'PROPOSED':
      default:
        return <span className="code-fix-status-badge status-proposed">Proposed</span>;
    }
  };

  // Render unified diff lines with explicit symbols and styles
  const renderDiffLines = () => {
    if (!unifiedDiff) {
      return (
        <div className="diff-empty-state">
          <span>No diff content available for preview.</span>
        </div>
      );
    }

    const lines = unifiedDiff.split('\n');
    return lines.map((line, idx) => {
      let lineType = 'context';
      if (line.startsWith('+++') || line.startsWith('---')) {
        lineType = 'header';
      } else if (line.startsWith('@@')) {
        lineType = 'hunk';
      } else if (line.startsWith('+')) {
        lineType = 'addition';
      } else if (line.startsWith('-')) {
        lineType = 'deletion';
      }

      return (
        <div key={idx} className={`diff-line diff-line-${lineType}`}>
          <span className="diff-line-number" aria-hidden="true">{idx + 1}</span>
          <span className="diff-line-indicator" aria-hidden="true">
            {lineType === 'addition' ? '+' : lineType === 'deletion' ? '-' : lineType === 'hunk' ? '@' : ' '}
          </span>
          <span className="diff-line-content">{line}</span>
        </div>
      );
    });
  };

  return (
    <div
      className={`code-fix-preview-container ${className}`}
      role="region"
      aria-label="AI proposed patch preview and fix history"
    >
      {/* Safety Warning Banner — ALWAYS CLEAR THAT NO WRITE HAS OCCURRED */}
      <div className="code-fix-warning-banner" role="alert">
        <div className="code-fix-warning-icon-group">
          <svg
            className="code-fix-warning-icon"
            width="18"
            height="18"
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
          <div className="code-fix-warning-text">
            <strong className="code-fix-warning-title">Proposed fix — not applied</strong>
            <p className="code-fix-warning-subtitle">
              This is an AI-generated proposal stored for review. It has NOT been applied to your repository or pull request.
            </p>
          </div>
        </div>

        {onClose && (
          <button
            type="button"
            className="btn btn-outline btn-sm code-fix-close-btn"
            onClick={onClose}
            aria-label="Close patch preview"
            title="Close patch preview"
          >
            ✕ Close
          </button>
        )}
      </div>

      {/* Active Proposal View (if available) */}
      {activeProposal ? (
        <>
          {/* Provenance & Target File Info */}
          <div className="code-fix-meta-bar">
            {proposalId && (
              <div className="code-fix-meta-item">
                <span className="code-fix-meta-label">Proposal:</span>
                <code className="code-fix-meta-id">#{proposalId}</code>
              </div>
            )}
            <div className="code-fix-meta-item">
              <span className="code-fix-meta-label">Status:</span>
              {getStatusBadge(status)}
            </div>
            <div className="code-fix-meta-item">
              <span className="code-fix-meta-label">Target File:</span>
              <code className="code-fix-meta-path">{filePath}</code>
            </div>
            <div className="code-fix-meta-item">
              <span className="code-fix-meta-label">Finding Source:</span>
              <span className="code-fix-meta-value">
                {isRule ? 'Deterministic Rule' : 'AI Review'}
              </span>
            </div>
            <div className="code-fix-meta-item">
              <span className="code-fix-meta-label">Provider:</span>
              <span className="code-fix-meta-value code-fix-ai-tag">
                {provider} ({model})
              </span>
            </div>
          </div>

          {/* Developer Instructions Notice */}
          {instructions && (
            <div className="code-fix-instructions-callout">
              <span className="code-fix-instructions-label">Developer Guidance:</span>
              <span className="code-fix-instructions-text">"{instructions}"</span>
            </div>
          )}

          {/* AI Explanation Section */}
          {explanation && (
            <div className="code-fix-explanation-box">
              <h5 className="code-fix-section-title">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="16" x2="12" y2="12" />
                  <line x1="12" y1="8" x2="12.01" y2="8" />
                </svg>
                Proposed Fix Explanation
              </h5>
              <p className="code-fix-explanation-text">{explanation}</p>
              {limitations && (
                <p className="code-fix-limitations-text">
                  <strong>Limitations:</strong> {limitations}
                </p>
              )}
            </div>
          )}

          {/* Content Inspection Tabs (Unified Diff / Original / Proposed) */}
          <div className="code-fix-diff-wrapper">
            <div className="code-fix-diff-header">
              <div className="code-fix-diff-tabs" role="tablist" aria-label="Patch content tabs">
                <button
                  type="button"
                  role="tab"
                  aria-selected={activeTab === 'diff'}
                  className={`code-fix-tab-btn ${activeTab === 'diff' ? 'code-fix-tab-btn-active' : ''}`}
                  onClick={() => setActiveTab('diff')}
                >
                  Unified Diff
                </button>
                {originalContent && (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'original'}
                    className={`code-fix-tab-btn ${activeTab === 'original' ? 'code-fix-tab-btn-active' : ''}`}
                    onClick={() => setActiveTab('original')}
                  >
                    Original Content
                  </button>
                )}
                {proposedContent && (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'proposed'}
                    className={`code-fix-tab-btn ${activeTab === 'proposed' ? 'code-fix-tab-btn-active' : ''}`}
                    onClick={() => setActiveTab('proposed')}
                  >
                    Proposed Content
                  </button>
                )}
              </div>
              <span className="code-fix-diff-badge">Read-only preview</span>
            </div>

            <div className="code-fix-diff-body" tabIndex={0} role="region" aria-label="Proposal code block">
              {activeTab === 'diff' && renderDiffLines()}
              {activeTab === 'original' && (
                <pre className="code-fix-raw-code">
                  <code>{originalContent}</code>
                </pre>
              )}
              {activeTab === 'proposed' && (
                <pre className="code-fix-raw-code">
                  <code>{proposedContent}</code>
                </pre>
              )}
            </div>
          </div>

          {/* Proposal Action Controls with Safe Lifecycle Transitions */}
          <div className="code-fix-actions-toolbar" role="toolbar" aria-label="Patch review actions">
            <button
              type="button"
              className="btn btn-sm btn-outline code-fix-btn"
              onClick={handleCopyPatch}
              aria-label="Copy unified patch diff to clipboard"
              title="Copy unified diff to clipboard"
            >
              {copiedPatch ? (
                <>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <span style={{ color: 'var(--status-completed)' }}>Copied Patch!</span>
                </>
              ) : (
                <>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                  </svg>
                  <span>Copy Patch</span>
                </>
              )}
            </button>

            {/* Allowed Transition Actions based on Proposal Status */}
            {proposalId && status === 'PROPOSED' && (
              <>
                <button
                  type="button"
                  className="btn btn-sm code-fix-btn code-fix-btn-review"
                  onClick={() => handleUpdateStatus('REVIEWED')}
                  disabled={isUpdatingStatus}
                  aria-label="Mark proposed patch as reviewed"
                >
                  ✓ Mark as Reviewed
                </button>
                <button
                  type="button"
                  className="btn btn-sm code-fix-btn code-fix-btn-reject"
                  onClick={() => handleUpdateStatus('REJECTED')}
                  disabled={isUpdatingStatus}
                  aria-label="Reject proposed patch"
                >
                  ✕ Reject
                </button>
              </>
            )}

            {proposalId && status === 'REVIEWED' && (
              <button
                type="button"
                className="btn btn-sm code-fix-btn code-fix-btn-expire"
                onClick={() => handleUpdateStatus('EXPIRED')}
                disabled={isUpdatingStatus}
                aria-label="Mark reviewed patch as expired"
              >
                ⌛ Mark as Expired
              </button>
            )}

            {proposalId && (status === 'REJECTED' || status === 'EXPIRED') && (
              <span className="code-fix-readonly-indicator" title="This proposal status is final and read-only">
                {status === 'REJECTED' ? '✕ Proposal Rejected' : '⌛ Proposal Expired'}
              </span>
            )}

            {onRegenerate && (
              <button
                type="button"
                className="btn btn-sm btn-outline code-fix-btn"
                onClick={onRegenerate}
                disabled={isRegenerating}
                aria-label="Regenerate proposed patch with AI"
              >
                {isRegenerating ? 'Regenerating...' : '↻ Review Again'}
              </button>
            )}

            {onClose && (
              <button
                type="button"
                className="btn btn-sm btn-secondary code-fix-btn"
                onClick={onClose}
                aria-label="Close diff preview"
              >
                Close Preview
              </button>
            )}
          </div>
        </>
      ) : (
        /* Empty State when no proposal is active or generated yet */
        <div className="code-fix-empty-state">
          <div className="code-fix-empty-icon" aria-hidden="true">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
          </div>
          <h4 className="code-fix-empty-title">No AI fix proposals yet.</h4>
          <p className="code-fix-empty-subtitle">
            Generate a fix from a finding to create the first proposal.
          </p>
          {onRegenerate && (
            <button
              type="button"
              className="btn btn-primary btn-sm code-fix-empty-action"
              onClick={onRegenerate}
              disabled={isRegenerating}
            >
              {isRegenerating ? 'Generating Fix...' : 'Generate Fix Now'}
            </button>
          )}
        </div>
      )}

      {/* Fix History Section */}
      <section className="code-fix-history-section" aria-label="Proposal History">
        <div className="code-fix-history-header">
          <div className="code-fix-history-header-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <h5>Fix History</h5>
            <span className="code-fix-history-count">{proposals.length}</span>
          </div>

          <button
            type="button"
            className="btn btn-outline btn-xs code-fix-history-refresh-btn"
            onClick={loadProposals}
            disabled={isLoadingHistory}
            title="Refresh proposal history"
            aria-label="Refresh proposal history"
          >
            {isLoadingHistory ? 'Refreshing...' : '↻ Refresh'}
          </button>
        </div>

        {/* Loading State */}
        {isLoadingHistory && proposals.length === 0 && (
          <div className="code-fix-history-loading" aria-live="polite">
            <LoadingSkeleton variant="rectangular" height="48px" count={2} />
          </div>
        )}

        {/* Error State */}
        {historyError && (
          <div className="code-fix-history-error" role="alert">
            <span>{historyError}</span>
            <button
              type="button"
              className="btn btn-outline btn-xs"
              onClick={loadProposals}
            >
              Retry
            </button>
          </div>
        )}

        {/* Empty History State */}
        {!isLoadingHistory && proposals.length === 0 && !historyError && (
          <div className="code-fix-history-empty">
            <p className="code-fix-history-empty-text">No previous proposals found for this finding.</p>
          </div>
        )}

        {/* Persisted Proposals List */}
        {proposals.length > 0 && (
          <div className="code-fix-history-list" role="list">
            {proposals.map((item) => {
              const isSelected = item.id === proposalId;
              return (
                <div
                  key={item.id}
                  className={`code-fix-history-item ${isSelected ? 'code-fix-history-item-active' : ''}`}
                  role="listitem"
                >
                  <div className="code-fix-history-item-main">
                    <div className="code-fix-history-item-meta">
                      <span className="code-fix-history-id">#{item.id}</span>
                      {getStatusBadge(item.status)}
                      <span className="code-fix-history-time">{formatDateTime(item.createdAt)}</span>
                      {item.provider && (
                        <span className="code-fix-history-provider">
                          {item.provider}{item.model ? ` · ${item.model}` : ''}
                        </span>
                      )}
                    </div>

                    <div className="code-fix-history-file">
                      <code>{item.filePath}</code>
                    </div>

                    {item.explanation && (
                      <p className="code-fix-history-explanation">
                        {item.explanation.length > 120
                          ? `${item.explanation.substring(0, 120)}...`
                          : item.explanation}
                      </p>
                    )}
                  </div>

                  <div className="code-fix-history-item-action">
                    <button
                      type="button"
                      className={`btn btn-xs ${isSelected ? 'btn-primary' : 'btn-outline'}`}
                      onClick={() => {
                        setSelectedProposalId(item.id);
                        setActiveTab('diff');
                      }}
                      aria-label={`View proposal #${item.id}`}
                    >
                      {isSelected ? 'Viewing' : 'View'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default CodeFixPreview;
