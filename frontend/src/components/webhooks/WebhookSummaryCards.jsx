import React from 'react';

export const WebhookSummaryCards = ({ summary = {}, loading = false }) => {
  const cards = [
    {
      id: 'total',
      label: 'Total Deliveries',
      value: summary.totalDeliveries ?? 0,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
      ),
      colorClass: 'total'
    },
    {
      id: 'completed',
      label: 'Completed Reviews',
      value: summary.completedDeliveries ?? 0,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
      ),
      colorClass: 'completed'
    },
    {
      id: 'processing',
      label: 'Currently Processing',
      value: summary.processingDeliveries ?? 0,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      ),
      colorClass: 'processing'
    },
    {
      id: 'failed',
      label: 'Failed Deliveries',
      value: summary.failedDeliveries ?? 0,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
      ),
      colorClass: 'failed'
    },
    {
      id: 'ignored',
      label: 'Ignored Events',
      value: summary.ignoredDeliveries ?? 0,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
        </svg>
      ),
      colorClass: 'ignored'
    }
  ];

  return (
    <div className="webhook-summary-grid">
      {cards.map((card) => (
        <div key={card.id} className={`webhook-summary-card card-${card.colorClass}`}>
          <div className="webhook-card-header">
            <span className="webhook-card-label">{card.label}</span>
            <div className="webhook-card-icon">{card.icon}</div>
          </div>
          <div className="webhook-card-value">
            {loading ? (
              <span className="webhook-val-skeleton">--</span>
            ) : (
              <span>{card.value.toLocaleString()}</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default WebhookSummaryCards;
