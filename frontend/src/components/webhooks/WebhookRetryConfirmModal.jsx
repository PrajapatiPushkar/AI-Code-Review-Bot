import React from 'react';

export const WebhookRetryConfirmModal = ({
  delivery,
  onConfirm,
  onCancel,
  loading = false
}) => {
  if (!delivery) return null;

  return (
    <div className="policy-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="retry-modal-title">
      <div className="policy-modal-container" style={{ maxWidth: '500px' }}>
        <div className="policy-modal-header">
          <div className="policy-modal-title-area">
            <h3 id="retry-modal-title" className="policy-modal-title">Confirm Delivery Retry</h3>
            <p className="policy-modal-subtitle">Re-run the automated review for this webhook delivery</p>
          </div>
          <button
            type="button"
            className="policy-modal-close-btn"
            onClick={onCancel}
            disabled={loading}
            aria-label="Close dialog"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="policy-modal-body">
          <div className="retry-confirm-content">
            <p className="retry-confirm-text">
              Are you sure you want to retry review processing for webhook delivery:
            </p>
            <div className="retry-target-card">
              <div className="retry-target-row">
                <span className="retry-target-label">Delivery ID:</span>
                <span className="retry-target-val mono">{delivery.deliveryId}</span>
              </div>
              <div className="retry-target-row">
                <span className="retry-target-label">Repository:</span>
                <span className="retry-target-val">{delivery.repository || 'N/A'}</span>
              </div>
              {delivery.pullRequestNumber && (
                <div className="retry-target-row">
                  <span className="retry-target-label">Pull Request:</span>
                  <span className="retry-target-val">#{delivery.pullRequestNumber}</span>
                </div>
              )}
              {delivery.attemptCount !== undefined && (
                <div className="retry-target-row">
                  <span className="retry-target-label">Attempt:</span>
                  <span className="retry-target-val">{delivery.attemptCount} / {delivery.maxAttempts || 3}</span>
                </div>
              )}
            </div>

            <p className="retry-confirm-note">
              <strong>Note:</strong> Retrying will atomically claim this delivery and re-dispatch the review to the asynchronous pipeline using the recorded event metadata. It will not create an unlinked duplicate review.
            </p>
          </div>
        </div>

        <div className="policy-modal-footer">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onCancel}
            disabled={loading}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => onConfirm(delivery)}
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                <span>Retrying...</span>
              </>
            ) : (
              <span>Confirm & Retry Delivery</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default WebhookRetryConfirmModal;
