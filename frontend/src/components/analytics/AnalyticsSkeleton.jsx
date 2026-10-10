import React from 'react';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const AnalyticsSkeleton = () => {
  return (
    <div aria-hidden="true">
      {/* 1. Stat cards skeleton */}
      <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginBottom: '1.75rem' }}>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="stat-card">
            <div className="stat-card-header">
              <LoadingSkeleton variant="text" width="55%" height="0.8125rem" />
              <LoadingSkeleton variant="circular" width="32px" height="32px" />
            </div>
            <div className="stat-card-body">
              <LoadingSkeleton variant="text" width="40%" height="2rem" style={{ marginBottom: '0.35rem' }} />
              <LoadingSkeleton variant="text" width="70%" height="0.75rem" style={{ marginBottom: 0 }} />
            </div>
          </div>
        ))}
      </div>

      {/* 2. Distributions & Trends Skeletons */}
      <div style={{ marginBottom: '1.75rem' }}>
        <LoadingSkeleton.Card height="180px" />
      </div>

      <div style={{ marginBottom: '1.75rem' }}>
        <LoadingSkeleton.Card height="160px" />
      </div>

      {/* 3. Table Skeleton */}
      <LoadingSkeleton.Table rows={4} columns={7} />
    </div>
  );
};

export default AnalyticsSkeleton;
