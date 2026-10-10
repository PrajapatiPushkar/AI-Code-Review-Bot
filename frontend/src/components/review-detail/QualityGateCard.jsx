import React, { useState, useEffect } from 'react';
import reviewService from '../../services/reviewService';

export const QualityGateCard = ({ reviewId, reviewStatus }) => {
  const [gate, setGate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchQualityGate = async () => {
    if (!reviewId) return;
    try {
      setLoading(true);
      setError(null);
      const data = await reviewService.getQualityGate(reviewId);
      setGate(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load quality-gate evaluation.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQualityGate();
  }, [reviewId, reviewStatus]);

  if (loading) {
    return (
      <div className="card quality-gate-card quality-gate-loading-card" aria-busy="true" aria-live="polite">
        <div className="quality-gate-header-row">
          <div className="skeleton-line" style={{ width: '160px', height: '22px' }} />
          <div className="skeleton-line" style={{ width: '100px', height: '28px', borderRadius: '14px' }} />
        </div>
        <div className="skeleton-line" style={{ width: '85%', height: '16px', marginTop: '12px' }} />
        <div className="skeleton-line" style={{ width: '50%', height: '14px', marginTop: '8px' }} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="card quality-gate-card quality-gate-error-card" role="region" aria-label="Quality Gate Evaluation">
        <div className="quality-gate-error-content">
          <span className="quality-gate-error-icon" aria-hidden="true">⚠️</span>
          <div className="quality-gate-error-text">
            <strong>Quality Gate Status Unavailable</strong>
            <p>{error}</p>
          </div>
          <button
            type="button"
            onClick={fetchQualityGate}
            className="btn btn-outline btn-sm"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!gate) return null;

  const status = gate.status || 'NOT_EVALUATED';
  const isPassed = status === 'PASS';
  const isFailed = status === 'FAIL';
  const isNotEvaluated = status === 'NOT_EVALUATED';

  // Configured severity threshold
  const threshold = gate.failOnSeverity || 'HIGH';
  const failureCount = gate.failureCount ?? 0;
  const reason = gate.reason || (isPassed
    ? `Quality gate passed: 0 findings met or exceeded the ${threshold} threshold.`
    : `Quality gate not evaluated for this review.`);

  let statusBadgeClass = 'badge-secondary';
  let statusText = 'NOT EVALUATED';
  let statusIcon = (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="16" x2="12" y2="12" />
      <line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
  );

  if (isPassed) {
    statusBadgeClass = 'badge-success quality-gate-badge-pass';
    statusText = 'PASSED';
    statusIcon = (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M20 6L9 17l-5-5" />
      </svg>
    );
  } else if (isFailed) {
    statusBadgeClass = 'badge-danger quality-gate-badge-fail';
    statusText = 'FAILED';
    statusIcon = (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <line x1="15" y1="9" x2="9" y2="15" />
        <line x1="9" y1="9" x2="15" y2="15" />
      </svg>
    );
  }

  return (
    <div
      className={`card quality-gate-card ${isPassed ? 'quality-gate-card-pass' : isFailed ? 'quality-gate-card-fail' : 'quality-gate-card-neutral'}`}
      role="region"
      aria-labelledby={`quality-gate-title-${reviewId}`}
    >
      <div className="quality-gate-header-row">
        <div className="quality-gate-title-group">
          <div className="quality-gate-shield-icon" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </div>
          <div>
            <h2 id={`quality-gate-title-${reviewId}`} className="quality-gate-title">
              Repository Quality Gate
            </h2>
            <span className="quality-gate-subtitle">
              Deterministic threshold evaluation based on repository review policy
            </span>
          </div>
        </div>

        <div className={`badge ${statusBadgeClass} quality-gate-status-pill`}>
          {statusIcon}
          <span className="quality-gate-status-text">{statusText}</span>
        </div>
      </div>

      <div className="quality-gate-body">
        <p className="quality-gate-reason">{reason}</p>

        <div className="quality-gate-metrics-grid">
          <div className="quality-gate-metric-box">
            <span className="quality-gate-metric-label">Status</span>
            <span className={`quality-gate-metric-value ${isPassed ? 'text-success' : isFailed ? 'text-danger' : 'text-muted'}`}>
              {statusText}
            </span>
          </div>

          <div className="quality-gate-metric-box">
            <span className="quality-gate-metric-label">Failure Threshold</span>
            <span className="quality-gate-metric-value">
              {threshold}
            </span>
          </div>

          <div className="quality-gate-metric-box">
            <span className="quality-gate-metric-label">Violating Findings</span>
            <span className={`quality-gate-metric-value ${failureCount > 0 ? 'text-danger' : 'text-success'}`}>
              {failureCount}
            </span>
          </div>

          <div className="quality-gate-metric-box">
            <span className="quality-gate-metric-label">Gate Policy</span>
            <span className="quality-gate-metric-value">
              {gate.enabled ? 'Enabled' : 'Disabled'}
            </span>
          </div>
        </div>

        <div className="quality-gate-disclaimer">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            Quality-gate evaluation applies configured repository rules and thresholds against detected findings. A passing gate does not guarantee that code is free of all defects or vulnerabilities.
          </span>
        </div>
      </div>
    </div>
  );
};

export default QualityGateCard;
