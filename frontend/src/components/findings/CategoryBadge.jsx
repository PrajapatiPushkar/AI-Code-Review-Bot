import React from 'react';

export const CategoryBadge = ({ category = 'OTHER', className = '' }) => {
  const normalized = (category || 'OTHER').toUpperCase().replace('-', '_');

  const getCategoryDetails = () => {
    switch (normalized) {
      case 'SECURITY':
        return {
          label: 'Security',
          className: 'category-badge-security',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          )
        };
      case 'BUG':
        return {
          label: 'Bug',
          className: 'category-badge-bug',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect width="8" height="14" x="8" y="6" rx="4" />
              <path d="m19 7-3 2" />
              <path d="m5 7 3 2" />
              <path d="m19 19-3-2" />
              <path d="m5 19 3-2" />
              <path d="M20 13h-4" />
              <path d="M4 13h4" />
              <path d="m10 4 1 2" />
              <path d="m14 4-1 2" />
            </svg>
          )
        };
      case 'PERFORMANCE':
        return {
          label: 'Performance',
          className: 'category-badge-performance',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          )
        };
      case 'CODE_STYLE':
        return {
          label: 'Code Style',
          className: 'category-badge-codestyle',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
          )
        };
      case 'MAINTAINABILITY':
        return {
          label: 'Maintainability',
          className: 'category-badge-maintainability',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
            </svg>
          )
        };
      case 'OTHER':
      default:
        return {
          label: category.replace('_', ' '),
          className: 'category-badge-other',
          icon: (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
              <line x1="7" y1="7" x2="7.01" y2="7" />
            </svg>
          )
        };
    }
  };

  const { label, className: catClass, icon } = getCategoryDetails();

  return (
    <span className={`category-badge ${catClass} ${className}`}>
      <span className="category-badge-icon" aria-hidden="true">
        {icon}
      </span>
      <span>{label}</span>
    </span>
  );
};

export default CategoryBadge;
