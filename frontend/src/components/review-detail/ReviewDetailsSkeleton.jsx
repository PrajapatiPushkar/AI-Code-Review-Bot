import React from 'react';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const ReviewDetailsSkeleton = () => {
  return (
    <div className="review-detail-page skeleton-page" aria-hidden="true">
      {/* Breadcrumb Skeleton */}
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '1rem' }}>
        <LoadingSkeleton variant="text" width="120px" height="14px" style={{ marginBottom: 0 }} />
        <LoadingSkeleton variant="text" width="10px" height="14px" style={{ marginBottom: 0 }} />
        <LoadingSkeleton variant="text" width="80px" height="14px" style={{ marginBottom: 0 }} />
      </div>

      {/* Header Skeleton */}
      <div className="review-detail-header-skeleton" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <LoadingSkeleton variant="text" width="180px" height="2rem" style={{ marginBottom: 0 }} />
            <LoadingSkeleton variant="rectangular" width="90px" height="24px" borderRadius="9999px" style={{ marginBottom: 0 }} />
          </div>
          <LoadingSkeleton variant="text" width="280px" height="14px" style={{ marginBottom: 0 }} />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <LoadingSkeleton variant="rectangular" width="120px" height="32px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
          <LoadingSkeleton variant="rectangular" width="110px" height="32px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
        </div>
      </div>

      {/* Metrics Grid Skeleton */}
      <div className="review-detail-metrics-grid" style={{ marginBottom: '1.5rem' }}>
        {Array.from({ length: 4 }).map((_, idx) => (
          <div key={idx} className="card review-metric-card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
              <LoadingSkeleton variant="text" width="80px" height="14px" style={{ marginBottom: 0 }} />
              <LoadingSkeleton variant="circular" width="28px" height="28px" />
            </div>
            <LoadingSkeleton variant="text" width="60px" height="2rem" style={{ marginBottom: '0.35rem' }} />
            <LoadingSkeleton variant="text" width="140px" height="12px" style={{ marginBottom: 0 }} />
          </div>
        ))}
      </div>

      {/* Two-Column Grid: Metadata & AI Summary Skeletons */}
      <div className="review-detail-content-grid">
        {/* Metadata Card Skeleton */}
        <div className="card review-metadata-card" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
            <LoadingSkeleton variant="circular" width="20px" height="20px" />
            <LoadingSkeleton variant="text" width="140px" height="1.25rem" style={{ marginBottom: 0 }} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
            {Array.from({ length: 6 }).map((_, rIdx) => (
              <div key={rIdx} style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)' }}>
                <LoadingSkeleton variant="text" width="100px" height="14px" style={{ marginBottom: 0 }} />
                <LoadingSkeleton variant="text" width="140px" height="14px" style={{ marginBottom: 0 }} />
              </div>
            ))}
          </div>
        </div>

        {/* AI Summary Card Skeleton */}
        <div className="card review-summary-card" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <LoadingSkeleton variant="circular" width="20px" height="20px" />
              <LoadingSkeleton variant="text" width="160px" height="1.25rem" style={{ marginBottom: 0 }} />
            </div>
            <LoadingSkeleton variant="rectangular" width="80px" height="24px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <LoadingSkeleton variant="text" width="100%" height="16px" />
            <LoadingSkeleton variant="text" width="95%" height="16px" />
            <LoadingSkeleton variant="text" width="85%" height="16px" />
            <LoadingSkeleton variant="text" width="90%" height="16px" />
            <LoadingSkeleton variant="text" width="60%" height="16px" style={{ marginBottom: 0 }} />
          </div>
        </div>
      </div>

      {/* Review Intelligence Card Skeleton */}
      <div className="card review-intelligence-card" style={{ padding: '1.5rem', marginTop: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <LoadingSkeleton variant="circular" width="22px" height="22px" />
            <LoadingSkeleton variant="text" width="180px" height="1.25rem" style={{ marginBottom: 0 }} />
          </div>
          <LoadingSkeleton variant="rectangular" width="100px" height="24px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
        </div>

        {/* 3 Metric Cards Skeleton */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
          {Array.from({ length: 3 }).map((_, mIdx) => (
            <div key={mIdx} style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius)' }}>
              <LoadingSkeleton variant="text" width="90px" height="12px" style={{ marginBottom: '0.5rem' }} />
              <LoadingSkeleton variant="text" width="50px" height="1.75rem" style={{ marginBottom: '0.25rem' }} />
              <LoadingSkeleton variant="text" width="120px" height="12px" style={{ marginBottom: 0 }} />
            </div>
          ))}
        </div>

        {/* Severity & Category Distributions Skeleton */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
          <div style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius)' }}>
            <LoadingSkeleton variant="text" width="140px" height="1rem" style={{ marginBottom: '1rem' }} />
            <LoadingSkeleton variant="rectangular" width="100%" height="8px" borderRadius="9999px" style={{ marginBottom: '1rem' }} />
            {Array.from({ length: 5 }).map((_, rIdx) => (
              <div key={rIdx} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <LoadingSkeleton variant="rectangular" width="70px" height="20px" borderRadius="9999px" />
                <LoadingSkeleton variant="text" width="50px" height="14px" />
              </div>
            ))}
          </div>

          <div style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius)' }}>
            <LoadingSkeleton variant="text" width="140px" height="1rem" style={{ marginBottom: '1rem' }} />
            {Array.from({ length: 5 }).map((_, rIdx) => (
              <div key={rIdx} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <LoadingSkeleton variant="rectangular" width="90px" height="20px" borderRadius="9999px" />
                <LoadingSkeleton variant="text" width="50px" height="14px" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReviewDetailsSkeleton;
