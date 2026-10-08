import React, { useState, useEffect, useCallback, useMemo } from 'react';
import useToast from '../../hooks/useToast';
import reviewService from '../../services/reviewService';
import LoadingSkeleton from '../common/LoadingSkeleton';

/**
 * Browser-safe download helper using Blob, URL.createObjectURL, and anchor element.
 * Ensures the temporary object URL is revoked immediately after the download triggers.
 */
const triggerBlobDownload = (response, fallbackFilename) => {
  const contentDisposition = response?.headers?.['content-disposition'] || response?.headers?.['Content-Disposition'];
  let filename = fallbackFilename;
  if (contentDisposition) {
    const match = /filename\*?=['"]?(?:UTF-\d['"]*)?([^;\r\n"']*)['"]?/i.exec(contentDisposition);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1].trim());
    }
  }
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data]);
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  window.URL.revokeObjectURL(url);
};

/**
 * Extracts human-readable error messages from error responses,
 * including Blob-wrapped JSON error responses from Axios.
 */
const parseBlobError = async (err, defaultMsg) => {
  if (err?.response?.data instanceof Blob) {
    try {
      const text = await err.response.data.text();
      const parsed = JSON.parse(text);
      if (parsed.message) return parsed.message;
    } catch {
      // ignore JSON parse failures
    }
  }
  return err?.response?.data?.message || err?.message || defaultMsg;
};

/**
 * CodeFixPreview: Developer Review Workspace around persisted AI fix proposals.
 * Provides side-by-side inspection, unified diff view, safe proposal status lifecycle,
 * and direct export/download of patch files, proposed files, and original files.
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
  const [copiedProposed, setCopiedProposed] = useState(false);
  const [isDownloadingPatch, setIsDownloadingPatch] = useState(false);
  const [isDownloadingProposed, setIsDownloadingProposed] = useState(false);
  const [isDownloadingOriginal, setIsDownloadingOriginal] = useState(false);
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
      const msg = err.response?.data?.message || err.message || 'Unable to load fix history.';
      setHistoryError(msg);
      if (toast?.error) {
        toast.error(msg, { title: 'Unable to load fix history.' });
      }
    } finally {
      setIsLoadingHistory(false);
    }
  }, [findingId, fix?.proposalId, selectedProposalId, toast]);

  useEffect(() => {
    loadProposals();
  }, [loadProposals]);

  // When a new fix is generated externally, update local selection
  useEffect(() => {
    if (fix?.proposalId) {
      setSelectedProposalId(fix.proposalId);
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
  const activeProposal = useMemo(() => {
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
  const createdAt = activeProposal?.createdAt;
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
      if (toast?.success) {
        toast.success('Unified patch copied to clipboard.', { title: 'Copied Patch' });
      }
      setTimeout(() => setCopiedPatch(false), 2000);
    } catch {
      if (toast?.error) {
        toast.error('Failed to copy patch to clipboard.', { title: 'Error' });
      }
    }
  };

  // Handle proposed content clipboard copy
  const handleCopyProposed = async () => {
    if (!proposedContent) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(proposedContent);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = proposedContent;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        const success = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (!success) throw new Error('Copy command failed');
      }

      setCopiedProposed(true);
      if (toast?.success) {
        toast.success('Proposed code copied to clipboard.', { title: 'Copied Code' });
      }
      setTimeout(() => setCopiedProposed(false), 2000);
    } catch {
      if (toast?.error) {
        toast.error('Failed to copy proposed code to clipboard.', { title: 'Error' });
      }
    }
  };

  // Download patch as .patch file
  const handleDownloadPatch = async () => {
    if (!proposalId || isDownloadingPatch) return;
    setIsDownloadingPatch(true);
    try {
      const response = await reviewService.downloadFixPatch(proposalId);
      triggerBlobDownload(response, `ai-fix-proposal-${proposalId}.patch`);
      if (toast?.success) {
        toast.success(`Patch file for proposal #${proposalId} downloaded.`, { title: 'Download Complete' });
      }
    } catch (err) {
      const msg = await parseBlobError(err, 'Unable to download the patch.');
      if (toast?.error) {
        toast.error(msg, { title: 'Unable to download the patch.' });
      }
    } finally {
      setIsDownloadingPatch(false);
    }
  };

  // Download proposed content file
  const handleDownloadProposed = async () => {
    if (!proposalId || isDownloadingProposed) return;
    setIsDownloadingProposed(true);
    try {
      const response = await reviewService.downloadProposedContent(proposalId);
      const safeFallback = filePath ? filePath.replace(/\\/g, '/').split('/').pop() : `proposed-${proposalId}.txt`;
      triggerBlobDownload(response, safeFallback);
      if (toast?.success) {
        toast.success('Proposed file content downloaded.', { title: 'Download Complete' });
      }
    } catch (err) {
      const msg = await parseBlobError(err, 'Unable to download the proposed file.');
      if (toast?.error) {
        toast.error(msg, { title: 'Unable to download the proposed file.' });
      }
    } finally {
      setIsDownloadingProposed(false);
    }
  };

  // Download original content file
  const handleDownloadOriginal = async () => {
    if (!proposalId || !originalContent || isDownloadingOriginal) return;
    setIsDownloadingOriginal(true);
    try {
      const response = await reviewService.downloadOriginalContent(proposalId);
      const safeFallback = filePath ? filePath.replace(/\\/g, '/').split('/').pop() : `original-${proposalId}.txt`;
      triggerBlobDownload(response, safeFallback);
      if (toast?.success) {
        toast.success('Original file content downloaded.', { title: 'Download Complete' });
      }
    } catch (err) {
      const msg = await parseBlobError(err, 'Unable to download the original file.');
      if (toast?.error) {
        toast.error(msg, { title: 'Unable to download the original file.' });
      }
    } finally {
      setIsDownloadingOriginal(false);
    }
  };

  // Safe status transition handler
  const handleUpdateStatus = async (newStatus) => {
    if (!proposalId || isUpdatingStatus) return;
    setIsUpdatingStatus(true);
    try {
      const updated = await reviewService.updateFixProposalStatus(proposalId, newStatus);
      setProposals((prev) =>
        prev.map((p) => (p.id === proposalId ? { ...p, status: updated.status } : p))
      );
      if (toast?.success) {
        toast.success(`Proposal #${proposalId} marked as ${newStatus.toLowerCase()}.`, {
          title: 'Status Updated'
        });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to update proposal status.';
      if (toast?.error) {
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
        year: 'numeric',
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
        return <span className="code-fix-status-badge status-proposed">● Proposed</span>;
    }
  };

  // Render unified diff lines with syntax styling
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
      aria-label="AI proposed patch preview and developer review workspace"
    >
      {/* 8. SAFETY NOTICE — COMPACT AND PROMINENT */}
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
            <strong className="code-fix-warning-title">AI-generated patch — not applied</strong>
            <p className="code-fix-warning-subtitle">
              Review and apply this change manually. This system does not modify your repository automatically.
            </p>
          </div>
        </div>

        {onClose && (
          <button
            type="button"
            className="btn btn-outline btn-sm code-fix-close-btn"
            onClick={onClose}
            aria-label="Close developer review workspace"
            title="Close developer review workspace"
          >
            ✕ Close
          </button>
        )}
      </div>

      {/* DEVELOPER REVIEW WORKSPACE */}
      {activeProposal ? (
        <div className="code-fix-workspace">
          {/* LEFT / CONTEXT COLUMN: Metadata, Explanation, Summary, Lifecycle */}
          <div className="code-fix-workspace-sidebar">
            {/* 1. AI FIX PROPOSAL METADATA */}
            <div className="code-fix-panel code-fix-meta-panel">
              <h5 className="code-fix-section-title">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
                AI Fix Proposal
              </h5>

              <div className="code-fix-meta-grid">
                <div className="code-fix-meta-field">
                  <span className="code-fix-meta-label">Status</span>
                  <div className="code-fix-meta-value">{getStatusBadge(status)}</div>
                </div>

                {proposalId && (
                  <div className="code-fix-meta-field">
                    <span className="code-fix-meta-label">Proposal ID</span>
                    <code className="code-fix-meta-id">#{proposalId}</code>
                  </div>
                )}

                <div className="code-fix-meta-field code-fix-meta-field-full">
                  <span className="code-fix-meta-label">File</span>
                  <code className="code-fix-meta-path" title={filePath}>{filePath}</code>
                </div>

                <div className="code-fix-meta-field">
                  <span className="code-fix-meta-label">Provider</span>
                  <span className="code-fix-meta-value code-fix-ai-tag">
                    {provider} {model ? `(${model})` : ''}
                  </span>
                </div>

                <div className="code-fix-meta-field">
                  <span className="code-fix-meta-label">Created</span>
                  <span className="code-fix-meta-value">{formatDateTime(createdAt)}</span>
                </div>

                {finding?.source && (
                  <div className="code-fix-meta-field">
                    <span className="code-fix-meta-label">Finding Source</span>
                    <span className="code-fix-meta-value">
                      {isRule ? 'Deterministic Rule' : 'AI Review'}
                    </span>
                  </div>
                )}
              </div>

              {instructions && (
                <div className="code-fix-instructions-callout">
                  <span className="code-fix-instructions-label">Developer Guidance:</span>
                  <span className="code-fix-instructions-text">"{instructions}"</span>
                </div>
              )}
            </div>

            {/* 2. AI EXPLANATION */}
            {explanation && (
              <div className="code-fix-panel code-fix-explanation-box">
                <h5 className="code-fix-section-title">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="16" x2="12" y2="12" />
                    <line x1="12" y1="8" x2="12.01" y2="8" />
                  </svg>
                  AI Explanation
                </h5>
                <p className="code-fix-explanation-text">{explanation}</p>
                {limitations && (
                  <p className="code-fix-limitations-text">
                    <strong>Limitations:</strong> {limitations}
                  </p>
                )}
              </div>
            )}

            {/* 3. CHANGE SUMMARY */}
            <div className="code-fix-panel code-fix-summary-panel">
              <h5 className="code-fix-section-title">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
                Change Summary
              </h5>
              <div className="code-fix-summary-badges">
                <div className={`code-fix-summary-item ${originalContent ? 'available' : 'unavailable'}`}>
                  <span className="code-fix-summary-dot" aria-hidden="true" />
                  <span className="code-fix-summary-label">Original Content:</span>
                  <strong className="code-fix-summary-val">{originalContent ? 'Available' : 'Unavailable'}</strong>
                </div>

                <div className={`code-fix-summary-item ${proposedContent ? 'available' : 'unavailable'}`}>
                  <span className="code-fix-summary-dot" aria-hidden="true" />
                  <span className="code-fix-summary-label">Proposed Content:</span>
                  <strong className="code-fix-summary-val">{proposedContent ? 'Available' : 'Unavailable'}</strong>
                </div>

                <div className={`code-fix-summary-item ${unifiedDiff ? 'available' : 'unavailable'}`}>
                  <span className="code-fix-summary-dot" aria-hidden="true" />
                  <span className="code-fix-summary-label">Unified Diff:</span>
                  <strong className="code-fix-summary-val">{unifiedDiff ? 'Available' : 'Unavailable'}</strong>
                </div>
              </div>
            </div>

            {/* 4. PROPOSAL LIFECYCLE MANAGEMENT */}
            <div className="code-fix-panel code-fix-lifecycle-panel">
              <h5 className="code-fix-section-title">Proposal Status</h5>
              <div className="code-fix-lifecycle-controls">
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
                    {status === 'REJECTED' ? '✕ Proposal Rejected (read-only)' : '⌛ Proposal Expired (read-only)'}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT / INSPECTION COLUMN: Code Comparison Tabs & Download Actions */}
          <div className="code-fix-workspace-main">
            {/* CODE COMPARISON */}
            <div className="code-fix-diff-wrapper">
              <div className="code-fix-diff-header">
                <div className="code-fix-diff-tabs" role="tablist" aria-label="Code comparison tabs">
                  <button
                    type="button"
                    role="tab"
                    id="tab-diff"
                    aria-controls="tabpanel-diff"
                    aria-selected={activeTab === 'diff'}
                    className={`code-fix-tab-btn ${activeTab === 'diff' ? 'code-fix-tab-btn-active' : ''}`}
                    onClick={() => setActiveTab('diff')}
                  >
                    1. Diff
                  </button>
                  {originalContent && (
                    <button
                      type="button"
                      role="tab"
                      id="tab-original"
                      aria-controls="tabpanel-original"
                      aria-selected={activeTab === 'original'}
                      className={`code-fix-tab-btn ${activeTab === 'original' ? 'code-fix-tab-btn-active' : ''}`}
                      onClick={() => setActiveTab('original')}
                    >
                      2. Original
                    </button>
                  )}
                  {proposedContent && (
                    <button
                      type="button"
                      role="tab"
                      id="tab-proposed"
                      aria-controls="tabpanel-proposed"
                      aria-selected={activeTab === 'proposed'}
                      className={`code-fix-tab-btn ${activeTab === 'proposed' ? 'code-fix-tab-btn-active' : ''}`}
                      onClick={() => setActiveTab('proposed')}
                    >
                      3. Proposed
                    </button>
                  )}
                </div>
                <span className="code-fix-diff-badge">Read-only preview</span>
              </div>

              <div
                className="code-fix-diff-body"
                tabIndex={0}
                role="tabpanel"
                id={`tabpanel-${activeTab}`}
                aria-labelledby={`tab-${activeTab}`}
              >
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

            {/* ACTIONS TOOLBAR */}
            <div className="code-fix-actions-toolbar" role="toolbar" aria-label="Patch export and inspection actions">
              {/* Copy Patch */}
              <button
                type="button"
                className="btn btn-sm btn-outline code-fix-btn"
                onClick={handleCopyPatch}
                disabled={!unifiedDiff}
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

              {/* Download Patch */}
              <button
                type="button"
                className="btn btn-sm btn-outline code-fix-btn"
                onClick={handleDownloadPatch}
                disabled={!proposalId || isDownloadingPatch}
                aria-label="Download patch as .patch file"
                title="Download unified diff as .patch file"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
                <span>{isDownloadingPatch ? 'Downloading...' : 'Download Patch'}</span>
              </button>

              {/* Download Proposed File */}
              <button
                type="button"
                className="btn btn-sm btn-outline code-fix-btn"
                onClick={handleDownloadProposed}
                disabled={!proposalId || isDownloadingProposed}
                aria-label="Download proposed replacement file"
                title="Download proposed replacement file"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <polyline points="10 12 12 14 14 12" />
                  <line x1="12" y1="14" x2="12" y2="8" />
                </svg>
                <span>{isDownloadingProposed ? 'Downloading...' : 'Download Proposed File'}</span>
              </button>

              {/* Copy Proposed Code */}
              <button
                type="button"
                className="btn btn-sm btn-outline code-fix-btn"
                onClick={handleCopyProposed}
                disabled={!proposedContent}
                aria-label="Copy proposed code to clipboard"
                title="Copy proposed code to clipboard"
              >
                {copiedProposed ? (
                  <>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--status-completed)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                    <span style={{ color: 'var(--status-completed)' }}>Copied Code!</span>
                  </>
                ) : (
                  <>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <polyline points="16 18 22 12 16 6" />
                      <polyline points="8 6 2 12 8 18" />
                    </svg>
                    <span>Copy Proposed Code</span>
                  </>
                )}
              </button>

              {/* Download Original File (if originalContent exists) */}
              {originalContent && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline code-fix-btn"
                  onClick={handleDownloadOriginal}
                  disabled={!proposalId || isDownloadingOriginal}
                  aria-label="Download original file"
                  title="Download original file"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                    <polyline points="7 10 12 15 17 10" />
                    <line x1="12" y1="15" x2="12" y2="3" />
                  </svg>
                  <span>{isDownloadingOriginal ? 'Downloading...' : 'Download Original File'}</span>
                </button>
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
            </div>
          </div>
        </div>
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

      {/* 10. FIX HISTORY SECTION */}
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
            <p className="code-fix-history-empty-text">No AI fix proposals yet.</p>
          </div>
        )}

        {/* Persisted Proposals List (Newest First) */}
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
                        if (selectedProposalId !== item.id) {
                          setSelectedProposalId(item.id);
                          setActiveTab('diff');
                        }
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
