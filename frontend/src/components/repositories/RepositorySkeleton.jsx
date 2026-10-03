import React from 'react';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const RepositorySkeleton = ({ count = 6 }) => {
  return (
    <div className="repository-grid" aria-busy="true" aria-label="Loading repositories">
      {Array.from({ length: count }).map((_, idx) => (
        <div key={idx} className="repository-card repository-card-skeleton">
          <div className="repository-card-header">
            <div className="repository-title-group" style={{ width: '100%' }}>
              <LoadingSkeleton variant="circular" width="24px" height="24px" />
              <div style={{ flex: 1 }}>
                <LoadingSkeleton variant="text" width="65%" height="1.125rem" />
                <LoadingSkeleton variant="text" width="45%" height="0.75rem" />
              </div>
            </div>
            <LoadingSkeleton variant="rounded" width="24px" height="24px" />
          </div>

          <div className="repository-meta-row" style={{ marginTop: '0.75rem' }}>
            <LoadingSkeleton variant="rounded" width="60px" height="20px" borderRadius="12px" />
            <LoadingSkeleton variant="rounded" width="75px" height="20px" borderRadius="12px" />
            <LoadingSkeleton variant="rounded" width="90px" height="20px" borderRadius="12px" />
          </div>

          <div className="repository-card-actions" style={{ marginTop: '1.25rem' }}>
            <LoadingSkeleton variant="rounded" width="48%" height="32px" borderRadius="6px" />
            <LoadingSkeleton variant="rounded" width="48%" height="32px" borderRadius="6px" />
          </div>
        </div>
      ))}
    </div>
  );
};

export default RepositorySkeleton;
