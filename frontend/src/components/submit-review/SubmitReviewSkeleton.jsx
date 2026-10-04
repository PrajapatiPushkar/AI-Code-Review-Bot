import React from 'react';
import LoadingSkeleton from '../common/LoadingSkeleton';

export const SubmitReviewSkeleton = () => {
  return (
    <div className="submit-review-container" aria-busy="true" aria-label="Loading review submission form">
      {/* Header Skeleton */}
      <div className="submit-review-header" style={{ marginBottom: '2rem' }}>
        <div style={{ flex: 1 }}>
          <LoadingSkeleton variant="text" width="260px" height="2rem" />
          <LoadingSkeleton variant="text" width="420px" height="1rem" style={{ marginTop: '0.5rem' }} />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <LoadingSkeleton variant="rounded" width="110px" height="36px" borderRadius="6px" />
          <LoadingSkeleton variant="rounded" width="120px" height="36px" borderRadius="6px" />
        </div>
      </div>

      {/* Grid Skeleton */}
      <div className="submit-review-grid">
        {/* Main Column */}
        <div className="submit-main-col">
          {/* Target Card Skeleton */}
          <div className="review-target-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <LoadingSkeleton variant="text" width="140px" height="0.875rem" />
              <LoadingSkeleton variant="rounded" width="90px" height="20px" borderRadius="10px" />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.875rem' }}>
              <LoadingSkeleton variant="rounded" width="40px" height="40px" borderRadius="8px" />
              <div style={{ flex: 1 }}>
                <LoadingSkeleton variant="text" width="60%" height="1.25rem" />
                <LoadingSkeleton variant="text" width="40%" height="0.875rem" style={{ marginTop: '0.375rem' }} />
              </div>
            </div>
          </div>

          {/* Form Card Skeleton */}
          <div className="submit-form-card">
            <LoadingSkeleton variant="text" width="180px" height="1.125rem" />
            <LoadingSkeleton variant="text" width="300px" height="0.75rem" style={{ marginBottom: '1.25rem' }} />

            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem' }}>
              <LoadingSkeleton variant="rounded" width="50%" height="36px" borderRadius="6px" />
              <LoadingSkeleton variant="rounded" width="50%" height="36px" borderRadius="6px" />
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <LoadingSkeleton variant="text" width="130px" height="0.875rem" style={{ marginBottom: '0.5rem' }} />
              <LoadingSkeleton variant="rounded" width="100%" height="38px" borderRadius="6px" />
            </div>

            <div>
              <LoadingSkeleton variant="text" width="100px" height="0.875rem" style={{ marginBottom: '0.5rem' }} />
              <LoadingSkeleton variant="rounded" width="100%" height="38px" borderRadius="6px" />
            </div>
          </div>

          {/* PR Details Skeleton */}
          <div className="submit-form-card">
            <LoadingSkeleton variant="text" width="160px" height="1.125rem" />
            <LoadingSkeleton variant="text" width="280px" height="0.75rem" style={{ marginBottom: '1.25rem' }} />

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <LoadingSkeleton variant="text" width="140px" height="0.875rem" style={{ marginBottom: '0.5rem' }} />
                <LoadingSkeleton variant="rounded" width="100%" height="38px" borderRadius="6px" />
              </div>
              <div>
                <LoadingSkeleton variant="text" width="120px" height="0.875rem" style={{ marginBottom: '0.5rem' }} />
                <LoadingSkeleton variant="rounded" width="100%" height="38px" borderRadius="6px" />
              </div>
            </div>
          </div>
        </div>

        {/* Side Column */}
        <div className="submit-side-col">
          <div className="submission-summary-card">
            <LoadingSkeleton variant="text" width="160px" height="1.125rem" style={{ marginBottom: '1.25rem' }} />

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem' }}>
              <div>
                <LoadingSkeleton variant="text" width="70px" height="0.75rem" />
                <LoadingSkeleton variant="text" width="100%" height="1rem" />
              </div>
              <div>
                <LoadingSkeleton variant="text" width="70px" height="0.75rem" />
                <LoadingSkeleton variant="text" width="60px" height="1rem" />
              </div>
              <div>
                <LoadingSkeleton variant="text" width="70px" height="0.75rem" />
                <LoadingSkeleton variant="text" width="140px" height="1rem" />
              </div>
              <div>
                <LoadingSkeleton variant="text" width="70px" height="0.75rem" />
                <LoadingSkeleton variant="text" width="100px" height="1rem" />
              </div>
            </div>

            <LoadingSkeleton variant="rounded" width="100%" height="60px" borderRadius="8px" style={{ marginBottom: '1.25rem' }} />
            <LoadingSkeleton variant="rounded" width="100%" height="42px" borderRadius="8px" style={{ marginBottom: '0.75rem' }} />
            <LoadingSkeleton variant="rounded" width="100%" height="36px" borderRadius="8px" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default SubmitReviewSkeleton;
