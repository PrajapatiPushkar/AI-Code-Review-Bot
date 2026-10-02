import React from 'react';

export const StatusBadge = ({ status = 'UNKNOWN', className = '' }) => {
  const normalizedStatus = (status || '').toUpperCase();
  const lowerStatus = normalizedStatus.toLowerCase().replace('-', '_');

  const getStatusLabel = () => {
    switch (normalizedStatus) {
      case 'COMPLETED':
        return 'Completed';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'FAILED':
        return 'Failed';
      default:
        return status;
    }
  };

  return (
    <span className={`badge badge-${lowerStatus} ${className}`}>
      <span className="badge-dot" aria-hidden="true" />
      {getStatusLabel()}
    </span>
  );
};

export default StatusBadge;
