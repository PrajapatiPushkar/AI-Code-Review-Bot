import React from 'react';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const FindingDetailsSkeleton = () => {
  return (
    <div className="findings-page skeleton-page" aria-hidden="true">
      {/* Breadcrumb Skeleton */}
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '1rem' }}>
        <LoadingSkeleton variant="text" width="120px" height="14px" style={{ marginBottom: 0 }} />
        <LoadingSkeleton variant="text" width="10px" height="14px" style={{ marginBottom: 0 }} />
        <LoadingSkeleton variant="text" width="140px" height="14px" style={{ marginBottom: 0 }} />
      </div>

      {/* Header Skeleton */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <LoadingSkeleton variant="text" width="260px" height="2rem" style={{ marginBottom: '0.5rem' }} />
          <LoadingSkeleton variant="text" width="340px" height="14px" style={{ marginBottom: 0 }} />
        </div>
        <LoadingSkeleton variant="rectangular" width="160px" height="36px" borderRadius="var(--radius)" style={{ marginBottom: 0 }} />
      </div>

      {/* Metrics Row Skeleton (5 cards) */}
      <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', marginBottom: '1.5rem' }}>
        {Array.from({ length: 5 }).map((_, idx) => (
          <div key={idx} className="card" style={{ padding: '1.25rem' }}>
            <LoadingSkeleton variant="text" width="70px" height="14px" style={{ marginBottom: '0.5rem' }} />
            <LoadingSkeleton variant="text" width="45px" height="1.75rem" style={{ marginBottom: 0 }} />
          </div>
        ))}
      </div>

      {/* Filters Card Skeleton */}
      <div className="card" style={{ padding: '1.25rem 1.5rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
          <LoadingSkeleton variant="rectangular" width="280px" height="38px" borderRadius="var(--radius)" />
          <LoadingSkeleton variant="rectangular" width="160px" height="38px" borderRadius="var(--radius)" />
          <LoadingSkeleton variant="rectangular" width="140px" height="38px" borderRadius="var(--radius)" />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {Array.from({ length: 6 }).map((_, idx) => (
            <LoadingSkeleton key={idx} variant="rectangular" width="80px" height="28px" borderRadius="9999px" />
          ))}
        </div>
      </div>

      {/* Finding Cards Skeletons (3 cards) */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {Array.from({ length: 3 }).map((_, idx) => (
          <div key={idx} className="card" style={{ padding: '1.5rem', borderLeft: '4px solid var(--border-color)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <LoadingSkeleton variant="rectangular" width="75px" height="22px" borderRadius="9999px" />
                <LoadingSkeleton variant="rectangular" width="90px" height="22px" borderRadius="var(--radius-sm)" />
                <LoadingSkeleton variant="text" width="180px" height="18px" />
              </div>
              <LoadingSkeleton variant="text" width="70px" height="16px" />
            </div>
            <LoadingSkeleton variant="text" width="90%" height="16px" />
            <LoadingSkeleton variant="text" width="75%" height="16px" style={{ marginBottom: '1rem' }} />
            <LoadingSkeleton variant="rectangular" height="75px" borderRadius="var(--radius)" />
          </div>
        ))}
      </div>
    </div>
  );
};

export default FindingDetailsSkeleton;
