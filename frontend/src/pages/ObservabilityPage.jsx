import React, { useState, useEffect, useCallback, useRef } from 'react';
import observabilityService from '../services/observabilityService';
import useToast from '../hooks/useToast';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

export const ObservabilityPage = () => {
  const toast = useToast();
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);

  const autoRefreshTimerRef = useRef(null);

  const fetchMetrics = useCallback(async (isManual = false) => {
    try {
      if (isManual) {
        setRefreshing(true);
      }
      const data = await observabilityService.getMetrics();
      setMetrics(data);
      setLastUpdated(new Date());
      setError(null);
      if (isManual) {
        toast?.success?.('Observability metrics updated');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to fetch observability metrics.';
      setError(msg);
      if (isManual) {
        toast?.error?.(msg);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [toast]);

  // Initial fetch
  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  // Auto-refresh interval (10s)
  useEffect(() => {
    if (autoRefresh) {
      autoRefreshTimerRef.current = setInterval(() => {
        fetchMetrics(false);
      }, 10000);
    } else {
      if (autoRefreshTimerRef.current) {
        clearInterval(autoRefreshTimerRef.current);
      }
    }
    return () => {
      if (autoRefreshTimerRef.current) {
        clearInterval(autoRefreshTimerRef.current);
      }
    };
  }, [autoRefresh, fetchMetrics]);

  const formatLatency = (ms) => {
    if (ms == null || isNaN(ms)) return '0.0 ms';
    if (ms >= 1000) {
      return `${(ms / 1000).toFixed(2)} s`;
    }
    return `${ms.toFixed(1)} ms`;
  };

  const getStatusBadge = (status) => {
    const isUp = status === 'UP';
    return (
      <span className={`obs-badge ${isUp ? 'obs-badge-up' : 'obs-badge-down'}`}>
        <span className={`obs-dot ${isUp ? 'obs-dot-up' : 'obs-dot-down'}`} />
        {status || 'UNKNOWN'}
      </span>
    );
  };

  const getCircuitBreakerBadge = (state) => {
    const isClosed = state === 'CLOSED';
    const isHalfOpen = state === 'HALF_OPEN';
    let badgeClass = 'obs-badge-up';
    if (isHalfOpen) badgeClass = 'obs-badge-warn';
    else if (!isClosed) badgeClass = 'obs-badge-down';

    return (
      <span className={`obs-badge ${badgeClass}`}>
        {state || 'CLOSED'}
      </span>
    );
  };

  if (loading && !metrics) {
    return (
      <div className="observability-page">
        <div className="obs-header">
          <div>
            <h1 className="page-title">Engineering Observability & Metrics</h1>
            <p className="page-subtitle">Loading operational telemetry and metrics...</p>
          </div>
        </div>
        <div className="obs-loading-grid">
          <div className="obs-skeleton-card" />
          <div className="obs-skeleton-card" />
          <div className="obs-skeleton-card" />
          <div className="obs-skeleton-card" />
        </div>
      </div>
    );
  }

  if (error && !metrics) {
    return (
      <div className="observability-page">
        <div className="obs-header">
          <div>
            <h1 className="page-title">Engineering Observability & Metrics</h1>
            <p className="page-subtitle">Telemetry monitoring dashboard</p>
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => fetchMetrics(true)}
          >
            Retry
          </button>
        </div>
        <ErrorMessage message={error} onRetry={() => fetchMetrics(true)} />
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="observability-page">
        <EmptyState
          title="No Telemetry Data Available"
          message="Could not retrieve application metrics at this time."
          actionText="Reload"
          onAction={() => fetchMetrics(true)}
        />
      </div>
    );
  }

  const { health = {}, reviewMetrics = {}, webhookMetrics = {}, externalServices = {} } = metrics;
  const gemini = externalServices.gemini || {};
  const github = externalServices.github || {};

  const totalReviews = (reviewMetrics.completed || 0) + (reviewMetrics.failed || 0);
  const successRate = totalReviews > 0
    ? ((reviewMetrics.completed / totalReviews) * 100).toFixed(1)
    : '100.0';

  const aiFindings = reviewMetrics.findingsBySource?.ai || 0;
  const ruleFindings = reviewMetrics.findingsBySource?.deterministic_rules || 0;
  const totalFindings = aiFindings + ruleFindings;

  const qgPass = reviewMetrics.qualityGates?.PASS || 0;
  const qgFail = reviewMetrics.qualityGates?.FAIL || 0;
  const qgNotEval = reviewMetrics.qualityGates?.NOT_EVALUATED || 0;

  return (
    <div className="observability-page">
      {/* Header */}
      <div className="obs-header">
        <div>
          <div className="obs-title-row">
            <h1 className="page-title">Engineering Observability & Metrics</h1>
            <span className="obs-live-indicator">
              <span className="obs-pulse-dot" /> LIVE IN-MEMORY SNAPSHOT
            </span>
          </div>
          <p className="page-subtitle">
            Micrometer instrumentation, health endpoints, review throughput, webhook reliability, and external dependency resilience.
          </p>
        </div>

        <div className="obs-actions">
          {lastUpdated && (
            <span className="obs-last-updated">
              Updated {lastUpdated.toLocaleTimeString()}
            </span>
          )}

          <button
            type="button"
            className={`btn btn-secondary obs-autorefresh-btn ${autoRefresh ? 'active' : ''}`}
            onClick={() => setAutoRefresh(!autoRefresh)}
            title={autoRefresh ? 'Disable 10s auto-refresh' : 'Enable 10s auto-refresh'}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            {autoRefresh ? 'Auto-refresh: ON' : 'Auto-refresh: OFF'}
          </button>

          <button
            type="button"
            className="btn btn-primary obs-refresh-btn"
            disabled={refreshing}
            onClick={() => fetchMetrics(true)}
          >
            <svg
              className={`refresh-icon ${refreshing ? 'spinning' : ''}`}
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {/* System Health Status Bar */}
      <div className="obs-health-banner">
        <div className="obs-health-banner-item">
          <span className="obs-item-label">System Health</span>
          <div className="obs-item-val">{getStatusBadge(health.status)}</div>
        </div>
        <div className="obs-health-banner-divider" />
        <div className="obs-health-banner-item">
          <span className="obs-item-label">Liveness Probe</span>
          <div className="obs-item-val">{getStatusBadge(health.liveness)}</div>
        </div>
        <div className="obs-health-banner-divider" />
        <div className="obs-health-banner-item">
          <span className="obs-item-label">Readiness Probe</span>
          <div className="obs-item-val">{getStatusBadge(health.readiness)}</div>
        </div>
        <div className="obs-health-banner-divider" />
        <div className="obs-health-banner-item">
          <span className="obs-item-label">PostgreSQL Database</span>
          <div className="obs-item-val">{getStatusBadge(health.database)}</div>
        </div>
        <div className="obs-health-banner-divider" />
        <div className="obs-health-banner-item">
          <span className="obs-item-label">Environment</span>
          <div className="obs-item-val">
            <span className="obs-env-tag">{metrics.environment || 'development'}</span>
          </div>
        </div>
      </div>

      {/* Stat Cards Overview Grid */}
      <div className="obs-summary-grid">
        <div className="obs-stat-card">
          <div className="obs-stat-header">
            <span className="obs-stat-title">Reviews Completed</span>
            <div className="obs-stat-icon obs-icon-indigo">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
          </div>
          <div className="obs-stat-value">{(reviewMetrics.completed || 0).toLocaleString()}</div>
          <div className="obs-stat-meta">
            <span className="obs-meta-success">{successRate}% success rate</span>
            <span className="obs-meta-sub">({reviewMetrics.failed || 0} failed)</span>
          </div>
        </div>

        <div className="obs-stat-card">
          <div className="obs-stat-header">
            <span className="obs-stat-title">In-Progress Reviews</span>
            <div className="obs-stat-icon obs-icon-amber">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
              </svg>
            </div>
          </div>
          <div className="obs-stat-value">{(reviewMetrics.inProgress || 0).toLocaleString()}</div>
          <div className="obs-stat-meta">
            <span className="obs-meta-neutral">Active async runner gauge</span>
          </div>
        </div>

        <div className="obs-stat-card">
          <div className="obs-stat-header">
            <span className="obs-stat-title">Avg Execution Duration</span>
            <div className="obs-stat-icon obs-icon-emerald">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 14 10" />
              </svg>
            </div>
          </div>
          <div className="obs-stat-value">{formatLatency(reviewMetrics.averageDurationMs)}</div>
          <div className="obs-stat-meta">
            <span className="obs-meta-neutral">Workflow lifecycle timer</span>
          </div>
        </div>

        <div className="obs-stat-card">
          <div className="obs-stat-header">
            <span className="obs-stat-title">Webhook Deliveries</span>
            <div className="obs-stat-icon obs-icon-purple">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
            </div>
          </div>
          <div className="obs-stat-value">{(webhookMetrics.received || 0).toLocaleString()}</div>
          <div className="obs-stat-meta">
            <span className="obs-meta-success">{webhookMetrics.validSignatures || 0} valid</span>
            <span className="obs-meta-sub">({webhookMetrics.invalidSignatures || 0} invalid HMAC)</span>
          </div>
        </div>
      </div>

      {/* Main Diagnostic Sections */}
      <div className="obs-grid-two-col">
        {/* Section 1: Review Workflow Intelligence */}
        <div className="obs-panel-card">
          <div className="obs-panel-header">
            <div className="obs-panel-title-group">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
              <h2>Review Pipeline Metrics</h2>
            </div>
            <span className="obs-tag">Micrometer: codereview.reviews.*</span>
          </div>

          <div className="obs-panel-body">
            {/* Total submissions & outcomes */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Lifecycle Counts</h3>
              <div className="obs-data-row">
                <span className="obs-data-label">Reviews Submitted</span>
                <span className="obs-data-value">{reviewMetrics.submitted || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Reviews Completed</span>
                <span className="obs-data-value obs-text-success">{reviewMetrics.completed || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Reviews Failed</span>
                <span className="obs-data-value obs-text-danger">{reviewMetrics.failed || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Average Execution Time</span>
                <span className="obs-data-value">{formatLatency(reviewMetrics.averageDurationMs)}</span>
              </div>
            </div>

            {/* Quality Gate Outcomes */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Quality Gate Outcomes</h3>
              <div className="obs-pill-row">
                <div className="obs-pill obs-pill-pass">
                  <span className="obs-pill-dot" />
                  <span>PASS:</span>
                  <strong>{qgPass}</strong>
                </div>
                <div className="obs-pill obs-pill-fail">
                  <span className="obs-pill-dot" />
                  <span>FAIL:</span>
                  <strong>{qgFail}</strong>
                </div>
                <div className="obs-pill obs-pill-not-eval">
                  <span className="obs-pill-dot" />
                  <span>NOT EVALUATED:</span>
                  <strong>{qgNotEval}</strong>
                </div>
              </div>
            </div>

            {/* Findings by Source */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Findings Generated by Source</h3>
              <div className="obs-findings-breakdown">
                <div className="obs-findings-metric">
                  <span className="obs-source-label">Gemini AI Engine:</span>
                  <span className="obs-source-val">{aiFindings} findings</span>
                </div>
                <div className="obs-findings-metric">
                  <span className="obs-source-label">Deterministic Rules:</span>
                  <span className="obs-source-val">{ruleFindings} findings</span>
                </div>
              </div>
              {totalFindings > 0 ? (
                <div className="obs-progress-bar-container">
                  <div
                    className="obs-progress-bar-segment obs-seg-ai"
                    style={{ width: `${(aiFindings / totalFindings) * 100}%` }}
                    title={`AI Findings: ${aiFindings}`}
                  />
                  <div
                    className="obs-progress-bar-segment obs-seg-rules"
                    style={{ width: `${(ruleFindings / totalFindings) * 100}%` }}
                    title={`Rules Findings: ${ruleFindings}`}
                  />
                </div>
              ) : (
                <div className="obs-empty-inline">No findings generated yet in current runtime.</div>
              )}
            </div>
          </div>
        </div>

        {/* Section 2: Webhook Reliability & Delivery Engine */}
        <div className="obs-panel-card">
          <div className="obs-panel-header">
            <div className="obs-panel-title-group">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              <h2>Webhook Delivery Reliability</h2>
            </div>
            <span className="obs-tag">Micrometer: webhook.*</span>
          </div>

          <div className="obs-panel-body">
            {/* Delivery Ingestion */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Ingestion & Security Verification</h3>
              <div className="obs-data-row">
                <span className="obs-data-label">Total Received Deliveries</span>
                <span className="obs-data-value">{webhookMetrics.received || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">HMAC Valid Signatures</span>
                <span className="obs-data-value obs-text-success">{webhookMetrics.validSignatures || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Invalid HMAC Signatures (Rejected)</span>
                <span className="obs-data-value obs-text-danger">{webhookMetrics.invalidSignatures || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Duplicate Deliveries Detected</span>
                <span className="obs-data-value obs-text-warning">{webhookMetrics.duplicates || 0}</span>
              </div>
            </div>

            {/* Delivery Outcomes */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Delivery Status Outcomes</h3>
              <div className="obs-pill-row">
                <div className="obs-pill obs-pill-pass">
                  <span>COMPLETED:</span>
                  <strong>{webhookMetrics.statusCounts?.COMPLETED || 0}</strong>
                </div>
                <div className="obs-pill obs-pill-fail">
                  <span>FAILED:</span>
                  <strong>{webhookMetrics.statusCounts?.FAILED || 0}</strong>
                </div>
                <div className="obs-pill obs-pill-info">
                  <span>ACCEPTED:</span>
                  <strong>{webhookMetrics.statusCounts?.ACCEPTED || 0}</strong>
                </div>
                <div className="obs-pill obs-pill-not-eval">
                  <span>IGNORED:</span>
                  <strong>{webhookMetrics.statusCounts?.IGNORED || 0}</strong>
                </div>
              </div>
            </div>

            {/* Retries and Recovery */}
            <div className="obs-subpanel">
              <h3 className="obs-section-heading">Retry Lifecycle & Self-Healing</h3>
              <div className="obs-data-row">
                <span className="obs-data-label">Retry Attempts Executed</span>
                <span className="obs-data-value">{webhookMetrics.retryAttempts || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Retries Exhausted (Terminal)</span>
                <span className="obs-data-value obs-text-danger">{webhookMetrics.exhaustedRetries || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Stale In-Flight Deliveries Recovered</span>
                <span className="obs-data-value">{webhookMetrics.staleRecoveries || 0}</span>
              </div>
              <div className="obs-data-row">
                <span className="obs-data-label">Pending Retries Processed by Scheduler</span>
                <span className="obs-data-value">{webhookMetrics.pendingRetriesScheduled || 0}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* External Dependencies & Resilience Card */}
      <div className="obs-panel-card obs-dep-panel">
        <div className="obs-panel-header">
          <div className="obs-panel-title-group">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2" />
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2" />
              <line x1="6" y1="6" x2="6.01" y2="6" />
              <line x1="6" y1="18" x2="6.01" y2="18" />
            </svg>
            <h2>External Integrations & Circuit Breakers</h2>
          </div>
          <span className="obs-tag">Resilience & Rate Limit Monitoring</span>
        </div>

        <div className="obs-dep-grid">
          {/* Gemini AI Card */}
          <div className="obs-dep-card">
            <div className="obs-dep-card-header">
              <div className="obs-dep-title">
                <span className="obs-dep-logo">✦</span>
                <strong>Gemini AI Integration</strong>
              </div>
              <div>{getCircuitBreakerBadge(gemini.circuitBreakerState)}</div>
            </div>
            <div className="obs-dep-metrics">
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Total API Calls</span>
                <span className="obs-dep-item-value">{gemini.calls || 0}</span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Average Latency</span>
                <span className="obs-dep-item-value">{formatLatency(gemini.averageLatencyMs)}</span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Total Failures</span>
                <span className={`obs-dep-item-value ${(gemini.failures || 0) > 0 ? 'obs-text-danger' : ''}`}>
                  {gemini.failures || 0}
                </span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Rate Limits (HTTP 429)</span>
                <span className={`obs-dep-item-value ${(gemini.rateLimits || 0) > 0 ? 'obs-text-warning' : ''}`}>
                  {gemini.rateLimits || 0}
                </span>
              </div>
            </div>
          </div>

          {/* GitHub API Card */}
          <div className="obs-dep-card">
            <div className="obs-dep-card-header">
              <div className="obs-dep-title">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                </svg>
                <strong>GitHub App Client</strong>
              </div>
              <div>{getCircuitBreakerBadge(github.circuitBreakerState)}</div>
            </div>
            <div className="obs-dep-metrics">
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Total API Calls</span>
                <span className="obs-dep-item-value">{github.calls || 0}</span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Average Latency</span>
                <span className="obs-dep-item-value">{formatLatency(github.averageLatencyMs)}</span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Total Failures</span>
                <span className={`obs-dep-item-value ${(github.failures || 0) > 0 ? 'obs-text-danger' : ''}`}>
                  {github.failures || 0}
                </span>
              </div>
              <div className="obs-dep-metric-item">
                <span className="obs-dep-item-label">Rate Limits (HTTP 429)</span>
                <span className={`obs-dep-item-value ${(github.rateLimits || 0) > 0 ? 'obs-text-warning' : ''}`}>
                  {github.rateLimits || 0}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Observability Architecture & Production Scrape Notice */}
      <div className="obs-infra-notice">
        <div className="obs-notice-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </div>
        <div className="obs-notice-content">
          <h4>Infrastructure Monitoring & Prometheus Configuration</h4>
          <p>
            This dashboard consumes bounded runtime metrics aggregated by Spring Boot Actuator and Micrometer.
            Prometheus scrapers can target <code>GET /actuator/prometheus</code> with standard scrape jobs.
            For details on correlation ID propagation (<code>X-Correlation-ID</code>), health probes, and Grafana dashboard setup, see{' '}
            <strong>docs/architecture/engineering-observability.md</strong>.
          </p>
        </div>
      </div>
    </div>
  );
};

export default ObservabilityPage;
