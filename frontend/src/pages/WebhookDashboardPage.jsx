import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import webhookService from '../services/webhookService';
import useToast from '../hooks/useToast';
import WebhookSummaryCards from '../components/webhooks/WebhookSummaryCards';
import WebhookDeliveryDetailModal from '../components/webhooks/WebhookDeliveryDetailModal';
import WebhookRetryConfirmModal from '../components/webhooks/WebhookRetryConfirmModal';
import EmptyState from '../components/EmptyState';
import ErrorMessage from '../components/ErrorMessage';

export const WebhookDashboardPage = () => {
  const toast = useToast();
  const navigate = useNavigate();

  // Summary State
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(true);

  // Deliveries List State
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter State
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Modals State
  const [selectedDetailDelivery, setSelectedDetailDelivery] = useState(null);
  const [selectedRetryDelivery, setSelectedRetryDelivery] = useState(null);
  const [retrying, setRetrying] = useState(false);

  // Stale request race protection
  const activeFetchIdRef = useRef(0);

  // Debounce search term
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(0);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Fetch Summary Metrics
  const fetchSummary = useCallback(async () => {
    try {
      setSummaryLoading(true);
      const data = await webhookService.getDeliverySummary();
      setSummary(data);
    } catch (err) {
      // Non-fatal error for summary cards
    } finally {
      setSummaryLoading(false);
    }
  }, []);

  // Fetch Deliveries
  const fetchDeliveries = useCallback(async () => {
    const requestId = ++activeFetchIdRef.current;
    try {
      setLoading(true);
      setError(null);

      const params = {
        page,
        size: pageSize,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        repository: debouncedSearch.trim() || undefined
      };

      const data = await webhookService.getDeliveries(params);

      if (requestId === activeFetchIdRef.current) {
        setDeliveries(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      }
    } catch (err) {
      if (requestId === activeFetchIdRef.current) {
        const msg = err.response?.data?.message || 'Failed to load webhook delivery history.';
        setError(msg);
        toast?.error?.(msg);
      }
    } finally {
      if (requestId === activeFetchIdRef.current) {
        setLoading(false);
      }
    }
  }, [page, pageSize, statusFilter, debouncedSearch, toast]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchDeliveries();
  }, [fetchDeliveries]);

  // Execute Retry
  const handleConfirmRetry = async (delivery) => {
    if (!delivery) return;
    try {
      setRetrying(true);
      await webhookService.retryDelivery(delivery.deliveryId);
      toast?.success?.(`Retry initiated for delivery ${delivery.deliveryId}. Automated review queued.`);
      setSelectedRetryDelivery(null);
      // Refresh list & summary
      fetchDeliveries();
      fetchSummary();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to initiate webhook retry.';
      toast?.error?.(msg);
    } finally {
      setRetrying(false);
    }
  };

  const handleOpenReview = (reviewId) => {
    if (reviewId) {
      navigate(`/reviews/${reviewId}`);
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

  const formatDate = (isoStr) => {
    if (!isoStr) return '--';
    try {
      const date = new Date(isoStr);
      return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoStr;
    }
  };

  return (
    <div className="webhook-operations-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Webhook Operations & Reliability</h1>
          <p className="page-subtitle">
            Monitor real-time GitHub webhook deliveries, audit automated Pull Request review jobs, and recover failed events.
          </p>
        </div>
        <div className="page-header-actions">
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={() => {
              fetchSummary();
              fetchDeliveries();
            }}
            disabled={loading}
            aria-label="Refresh webhook deliveries"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Summary Metrics Cards */}
      <WebhookSummaryCards summary={summary} loading={summaryLoading} />

      {/* Content Area */}
      <div className="webhook-content-card">
        {/* Filters Bar */}
        <div className="webhook-filter-bar">
          <div className="webhook-filter-status-group">
            {['ALL', 'COMPLETED', 'FAILED', 'PROCESSING', 'IGNORED'].map((status) => (
              <button
                key={status}
                type="button"
                className={`webhook-filter-pill ${statusFilter === status ? 'active' : ''}`}
                onClick={() => {
                  setStatusFilter(status);
                  setPage(0);
                }}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          <div className="webhook-search-box">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              className="webhook-search-input"
              placeholder="Filter by repository..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              aria-label="Filter deliveries by repository"
            />
            {searchTerm && (
              <button
                type="button"
                className="webhook-search-clear"
                onClick={() => setSearchTerm('')}
                aria-label="Clear search"
              >
                ×
              </button>
            )}
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div style={{ padding: '1.5rem' }}>
            <ErrorMessage message={error} onRetry={fetchDeliveries} />
          </div>
        )}

        {/* Loading Skeleton */}
        {loading && !error && (
          <div className="webhook-loading-container">
            <div className="webhook-skeleton-row" />
            <div className="webhook-skeleton-row" />
            <div className="webhook-skeleton-row" />
            <div className="webhook-skeleton-row" />
            <div className="webhook-skeleton-row" />
          </div>
        )}

        {/* Empty State: Zero Total Deliveries */}
        {!loading && !error && deliveries.length === 0 && (summary?.totalDeliveries === 0) && (
          <EmptyState
            title="No Webhook Deliveries Received"
            message="Configure your GitHub App with your webhook URL to automatically trigger AI code reviews on Pull Requests."
            icon="⚡"
            action={
              <div className="webhook-setup-guide-box">
                <p className="webhook-setup-guide-title">Setup Instructions:</p>
                <ol className="webhook-setup-steps">
                  <li>In GitHub Developer Settings $\rightarrow$ GitHub Apps, set <strong>Webhook URL</strong> to <code>/api/v1/webhooks/github</code>.</li>
                  <li>Set <strong>Webhook secret</strong> to match <code>GITHUB_WEBHOOK_SECRET</code> in your environment.</li>
                  <li>Subscribe to the <strong>Pull request</strong> event.</li>
                </ol>
              </div>
            }
          />
        )}

        {/* Empty State: Filter Matched Zero Results */}
        {!loading && !error && deliveries.length === 0 && (summary?.totalDeliveries > 0) && (
          <EmptyState
            title="No Matching Deliveries"
            message={`No webhook deliveries matched your current status filter "${statusFilter}" or search term.`}
            icon="🔍"
            action={
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setStatusFilter('ALL');
                  setSearchTerm('');
                  setPage(0);
                }}
              >
                Reset Filters
              </button>
            }
          />
        )}

        {/* Deliveries Table */}
        {!loading && !error && deliveries.length > 0 && (
          <div className="webhook-table-responsive">
            <table className="webhook-table">
              <thead>
                <tr>
                  <th>Delivery ID</th>
                  <th>Repository</th>
                  <th>Event / Action</th>
                  <th>Status</th>
                  <th>Attempts</th>
                  <th>Received</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {deliveries.map((delivery) => (
                  <tr key={delivery.id || delivery.deliveryId} className={`row-status-${delivery.status.toLowerCase()}`}>
                    {/* Delivery ID */}
                    <td>
                      <span className="webhook-id-badge mono" title={delivery.deliveryId}>
                        {delivery.deliveryId.length > 12 ? `${delivery.deliveryId.substring(0, 10)}...` : delivery.deliveryId}
                      </span>
                    </td>

                    {/* Repository & PR */}
                    <td>
                      <div className="webhook-repo-cell">
                        <span className="webhook-repo-name">{delivery.repository || 'N/A'}</span>
                        {delivery.pullRequestNumber && (
                          <span className="webhook-pr-tag">#{delivery.pullRequestNumber}</span>
                        )}
                        {delivery.commitSha && (
                          <span className="webhook-sha-tag mono">{delivery.commitSha.substring(0, 7)}</span>
                        )}
                      </div>
                    </td>

                    {/* Event & Action */}
                    <td>
                      <span className="webhook-action-tag">
                        {delivery.eventType}{delivery.action ? `.${delivery.action}` : ''}
                      </span>
                    </td>

                    {/* Status */}
                    <td>
                      <span className={`quality-gate-status-badge ${getStatusBadgeClass(delivery.status)}`}>
                        {delivery.status}
                      </span>
                    </td>

                    {/* Attempts */}
                    <td>
                      <span className="webhook-attempt-val">
                        {delivery.attemptCount || 1}/{delivery.maxAttempts || 3}
                      </span>
                    </td>

                    {/* Received At */}
                    <td>
                      <span className="webhook-time-text">
                        {formatDate(delivery.receivedAt)}
                      </span>
                    </td>

                    {/* Actions */}
                    <td>
                      <div className="webhook-actions-cell">
                        <button
                          type="button"
                          className="btn btn-ghost btn-xs"
                          onClick={() => setSelectedDetailDelivery(delivery)}
                          title="Inspect delivery details"
                        >
                          Details
                        </button>

                        {delivery.codeReviewId && (
                          <button
                            type="button"
                            className="btn btn-outline btn-xs"
                            onClick={() => handleOpenReview(delivery.codeReviewId)}
                            title={`Open Code Review #${delivery.codeReviewId}`}
                          >
                            Review #{delivery.codeReviewId}
                          </button>
                        )}

                        {delivery.retryEligible && (
                          <button
                            type="button"
                            className="btn btn-primary btn-xs"
                            onClick={() => setSelectedRetryDelivery(delivery)}
                            title="Retry this failed delivery"
                          >
                            Retry
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {!loading && !error && totalPages > 1 && (
          <div className="finding-pagination" style={{ padding: '1rem 1.5rem', margin: 0 }}>
            <span className="pagination-info">
              Page {page + 1} of {totalPages} ({totalElements} total deliveries)
            </span>
            <div className="pagination-controls">
              <button
                type="button"
                className="btn btn-outline btn-xs"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn btn-outline btn-xs"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Details Modal */}
      {selectedDetailDelivery && (
        <WebhookDeliveryDetailModal
          delivery={selectedDetailDelivery}
          onClose={() => setSelectedDetailDelivery(null)}
          onOpenRetry={(deliv) => {
            setSelectedDetailDelivery(null);
            setSelectedRetryDelivery(deliv);
          }}
        />
      )}

      {/* Retry Confirmation Modal */}
      {selectedRetryDelivery && (
        <WebhookRetryConfirmModal
          delivery={selectedRetryDelivery}
          onConfirm={handleConfirmRetry}
          onCancel={() => setSelectedRetryDelivery(null)}
          loading={retrying}
        />
      )}
    </div>
  );
};

export default WebhookDashboardPage;
