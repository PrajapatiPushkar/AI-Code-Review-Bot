import React from 'react';

const EmptyState = ({
  title = 'No Data Found',
  message = 'There are no items to display at this time.',
  icon,
  action,
  children
}) => {
  return (
    <div className="empty-card">
      <div className="empty-icon" aria-hidden="true">
        {icon || '📂'}
      </div>
      <h3 className="empty-title" style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.375rem' }}>
        {title}
      </h3>
      <p className="empty-message" style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '440px', margin: '0 auto', lineHeight: 1.5 }}>
        {message}
      </p>
      {action && <div className="empty-action" style={{ marginTop: '1.25rem', display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>{action}</div>}
      {children}
    </div>
  );
};

export default EmptyState;
