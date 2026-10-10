import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export const WebhookDeliveryDetailModal = ({
  delivery,
  onClose,
  onOpenRetry
}) => {
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  if (!delivery) return null;

  const handleCopyId = () => {
    navigator.clipboard?.writeText(delivery.deliveryId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleViewReview = () => {
    if (delivery.codeReviewId) {
      navigate(`/reviews/${delivery.codeReviewId}`);
    }
  };

  const formatDate = (isoStr) => {
    if (!isoStr) return 'N/A';
    try {
      return new Date(isoStr).toLocaleString();
    } catch {
      return isoStr;
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'status-pass';
      case 'FAILED':
        return 'status-fail';
      case 'PROCESSING':
        return 'status-processing';
      case 'IGNORED':
      default:
        return 'status-not-evaluated';
    }
  };

  return (
    <div className="policy-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="delivery-detail-title">
      <div className="policy-modal-container" style={{ maxWidth: '680px' }}>
        <div className="policy-modal-header">
          <div className="policy-modal-title-area">
            <div className="policy-modal-header-top">
              <h3 id="delivery-detail-title" className="policy-modal-title">Webhook Delivery Details</h3>
              <span className={`quality-gate-status-badge ${getStatusBadgeClass(delivery.status)}`}>
                {delivery.status}
              </span>
            </div>
            <p className="policy-modal-subtitle">Delivery audit trail and error inspection</p>
          </div>
          <button
            type="button"
            className="policy-modal-close-btn"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="policy-modal-body">
          {/* Metadata Grid */}
          <div className="delivery-detail-grid">
            <div className="delivery-detail-item full-width">
              <span className="delivery-detail-label">Delivery ID</span>
              <div className="delivery-id-copy-row">
                <span className="delivery-detail-value mono">{delivery.deliveryId}</span>
                <button
                  type="button"
                  className="btn btn-outline btn-xs"
                  onClick={handleCopyId}
                  title="Copy Delivery ID"
                >
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
            </div>

            <div className="delivery-detail-item">
              <span className="delivery-detail-label">Repository</span>
              <span className="delivery-detail-value">{delivery.repository || 'N/A'}</span>
            </div>

            <div className="delivery-detail-item">
              <span className="delivery-detail-label">Event & Action</span>
              <span className="delivery-detail-value mono">
                {delivery.eventType || 'pull_request'}{delivery.action ? `.${delivery.action}` : ''}
              </span>
            </div>

            {delivery.pullRequestNumber && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Pull Request</span>
                <span className="delivery-detail-value">#{delivery.pullRequestNumber}</span>
              </div>
            )}

            {delivery.commitSha && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Head Commit SHA</span>
                <span className="delivery-detail-value mono">
                  {delivery.commitSha.substring(0, 10)}
                </span>
              </div>
            )}

            <div className="delivery-detail-item">
              <span className="delivery-detail-label">Attempts</span>
              <span className="delivery-detail-value">
                {delivery.attemptCount || 1} / {delivery.maxAttempts || 3}
              </span>
            </div>

            {delivery.installationId && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">GitHub Installation ID</span>
                <span className="delivery-detail-value mono">{delivery.installationId}</span>
              </div>
            )}

            <div className="delivery-detail-item">
              <span className="delivery-detail-label">Received At</span>
              <span className="delivery-detail-value">{formatDate(delivery.receivedAt)}</span>
            </div>

            {delivery.startedAt && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Started At</span>
                <span className="delivery-detail-value">{formatDate(delivery.startedAt)}</span>
              </div>
            )}

            {delivery.processedAt && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Processed At</span>
                <span className="delivery-detail-value">{formatDate(delivery.processedAt)}</span>
              </div>
            )}

            {delivery.durationMs !== undefined && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Duration</span>
                <span className="delivery-detail-value">{delivery.durationMs} ms</span>
              </div>
            )}

            {delivery.nextRetryAt && (
              <div className="delivery-detail-item">
                <span className="delivery-detail-label">Next Scheduled Retry</span>
                <span className="delivery-detail-value text-warning">{formatDate(delivery.nextRetryAt)}</span>
              </div>
            )}
          </div>

          {/* Failure & Error Banner */}
          {delivery.status === 'FAILED' && (
            <div className="delivery-error-box">
              <div className="delivery-error-header">
                <span className="delivery-error-tag">
                  {delivery.errorCategory || 'ERROR'}
                </span>
                <span className="delivery-retryable-tag">
                  {delivery.retryable ? 'Transient / Retry Eligible' : 'Non-Retryable / Permanent'}
                </span>
              </div>
              <p className="delivery-error-message">
                {delivery.errorMessage || 'Unknown processing error'}
              </p>
            </div>
          )}

          {/* Ignored Reason */}
          {delivery.status === 'IGNORED' && delivery.errorMessage && (
            <div className="delivery-ignored-box">
              <span className="delivery-ignored-label">Reason for ignoring:</span>
              <p className="delivery-ignored-message">{delivery.errorMessage}</p>
            </div>
          )}

          {/* Associated Code Review Banner */}
          {delivery.codeReviewId && (
            <div className="delivery-review-box">
              <div className="delivery-review-info">
                <span className="delivery-review-title">Associated Review Record</span>
                <span className="delivery-review-id mono">Review ID #{delivery.codeReviewId}</span>
              </div>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={handleViewReview}
              >
                <span>Open Code Review</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M5 12h14" />
                  <path d="m12 5 7 7-7 7" />
                </svg>
              </button>
            </div>
          )}
        </div>

        <div className="policy-modal-footer">
          <div className="delivery-modal-footer-left">
            {delivery.retryEligible && (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => {
                  onClose();
                  onOpenRetry(delivery);
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="23 4 23 10 17 10" />
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                </svg>
                <span>Retry Delivery</span>
              </button>
            )}
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onClose}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default WebhookDeliveryDetailModal;
