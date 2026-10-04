import React from 'react';
import CategoryBadge from '../findings/CategoryBadge';

const ORDERED_CATEGORIES = [
  'BUG',
  'SECURITY',
  'PERFORMANCE',
  'CODE_STYLE',
  'MAINTAINABILITY',
  'OTHER'
];

export const CategoryBreakdown = ({ findings = [] }) => {
  const counts = {};
  ORDERED_CATEGORIES.forEach((cat) => {
    counts[cat] = 0;
  });

  findings.forEach((f) => {
    const rawCat = (f.category || 'OTHER').toUpperCase().replace('-', '_');
    if (counts[rawCat] !== undefined) {
      counts[rawCat]++;
    } else {
      counts.OTHER = (counts.OTHER || 0) + 1;
    }
  });

  const total = findings.length;

  // Filter to show active categories first, but ensure at least non-zero or all standard categories
  const activeCategories = ORDERED_CATEGORIES.filter((cat) => counts[cat] > 0);
  const displayedCategories = activeCategories.length > 0 ? activeCategories : ORDERED_CATEGORIES;

  return (
    <div className="card category-breakdown-card" role="region" aria-label="Category distribution">
      <div className="category-breakdown-header">
        <h3 className="card-title category-breakdown-title">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <rect width="7" height="7" x="3" y="3" rx="1" />
            <rect width="7" height="7" x="14" y="3" rx="1" />
            <rect width="7" height="7" x="14" y="14" rx="1" />
            <rect width="7" height="7" x="3" y="14" rx="1" />
          </svg>
          <span>Category Distribution</span>
        </h3>
        <span className="category-breakdown-count" aria-label={`${displayedCategories.length} categories represented`}>
          {activeCategories.length} Active {activeCategories.length === 1 ? 'Category' : 'Categories'}
        </span>
      </div>

      <div className="category-breakdown-list">
        {displayedCategories.map((catKey) => {
          const count = counts[catKey] || 0;
          const percentage = total > 0 ? Math.round((count / total) * 100) : 0;

          return (
            <div key={catKey} className="category-breakdown-row">
              <div className="category-row-badge-wrap">
                <CategoryBadge category={catKey} />
              </div>

              <div className="category-row-bar-track">
                <div
                  className="category-row-bar-fill"
                  style={{
                    width: `${percentage}%`
                  }}
                  aria-hidden="true"
                />
              </div>

              <div className="category-row-counts">
                <strong className="category-row-number">{count}</strong>
                <span className="category-row-percent">({percentage}%)</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default CategoryBreakdown;
