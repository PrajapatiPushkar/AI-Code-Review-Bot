import React from 'react';

const EmptyState = ({ title = 'No Data Found', message = 'There are no items to display at this time.' }) => {
  return (
    <div className="empty-card">
      <div className="empty-icon">📂</div>
      <h3>{title}</h3>
      <p>{message}</p>
    </div>
  );
};

export default EmptyState;
