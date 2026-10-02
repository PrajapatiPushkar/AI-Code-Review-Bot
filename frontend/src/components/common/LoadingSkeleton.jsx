import React from 'react';

/**
 * Reusable LoadingSkeleton component supporting shimmer animation,
 * varied variants, responsive sizing, and reduced-motion preferences.
 */
export const LoadingSkeleton = ({
  variant = 'rectangular', // 'text' | 'circular' | 'rounded' | 'rectangular'
  width,
  height,
  borderRadius,
  className = '',
  style = {},
  count = 1,
  ...restProps
}) => {
  const getVariantStyles = () => {
    switch (variant) {
      case 'text':
        return {
          height: height || '0.875rem',
          width: width || '100%',
          borderRadius: borderRadius || 'var(--radius-sm, 4px)',
          marginBottom: '0.5rem'
        };
      case 'circular': {
        const size = width || height || '2.5rem';
        return {
          width: size,
          height: size,
          borderRadius: '50%'
        };
      }
      case 'rounded':
        return {
          width: width || '100%',
          height: height || '2.5rem',
          borderRadius: borderRadius || 'var(--radius-lg, 12px)'
        };
      case 'rectangular':
      default:
        return {
          width: width || '100%',
          height: height || '1.25rem',
          borderRadius: borderRadius || 'var(--radius, 8px)'
        };
    }
  };

  const computedStyle = {
    ...getVariantStyles(),
    ...style
  };

  // Render multiple items if count > 1
  if (count > 1) {
    return (
      <div className="skeleton-group" aria-hidden="true">
        {Array.from({ length: count }).map((_, idx) => (
          <div
            key={idx}
            className={`skeleton-shimmer skeleton-${variant} ${className}`}
            style={computedStyle}
            {...restProps}
          />
        ))}
      </div>
    );
  }

  return (
    <div
      className={`skeleton-shimmer skeleton-${variant} ${className}`}
      style={computedStyle}
      aria-hidden="true"
      {...restProps}
    />
  );
};

/**
 * Convenient compound components for common SaaS layout patterns
 */
export const SkeletonCard = ({ height = '120px', className = '', ...props }) => (
  <div className={`card skeleton-card-container ${className}`} aria-hidden="true" {...props}>
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
      <LoadingSkeleton variant="circular" width="32px" height="32px" />
      <div style={{ flex: 1 }}>
        <LoadingSkeleton variant="text" width="45%" height="0.875rem" />
        <LoadingSkeleton variant="text" width="25%" height="0.75rem" style={{ marginBottom: 0 }} />
      </div>
    </div>
    <LoadingSkeleton variant="rectangular" height={height} borderRadius="var(--radius)" />
  </div>
);

export const SkeletonRow = ({ columns = 5, className = '', ...props }) => (
  <tr className={`skeleton-table-row ${className}`} aria-hidden="true" {...props}>
    {Array.from({ length: columns }).map((_, colIdx) => (
      <td key={colIdx} style={{ padding: '1rem' }}>
        <LoadingSkeleton
          variant="text"
          width={colIdx === 0 ? '40%' : colIdx === 1 ? '70%' : '55%'}
          style={{ marginBottom: 0 }}
        />
      </td>
    ))}
  </tr>
);

export const SkeletonTable = ({ rows = 4, columns = 5, className = '', ...props }) => (
  <div className={`card ${className}`} aria-hidden="true" {...props}>
    <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <LoadingSkeleton variant="text" width="160px" height="1.25rem" style={{ marginBottom: 0 }} />
      <LoadingSkeleton variant="rectangular" width="80px" height="30px" borderRadius="var(--radius)" />
    </div>
    <div className="table-responsive">
      <table className="data-table">
        <thead>
          <tr>
            {Array.from({ length: columns }).map((_, idx) => (
              <th key={idx}>
                <LoadingSkeleton variant="text" width="60%" height="0.75rem" style={{ marginBottom: 0 }} />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }).map((_, rowIdx) => (
            <SkeletonRow key={rowIdx} columns={columns} />
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

LoadingSkeleton.Card = SkeletonCard;
LoadingSkeleton.Row = SkeletonRow;
LoadingSkeleton.Table = SkeletonTable;

export default LoadingSkeleton;
