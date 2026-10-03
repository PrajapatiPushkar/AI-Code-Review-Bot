import React from 'react';

export const SeverityBadge = ({ severity = 'INFO', className = '' }) => {
  const normalized = (severity || 'INFO').toUpperCase();

  const getLabel = () => {
    switch (normalized) {
      case 'CRITICAL':
        return 'Critical';
      case 'HIGH':
        return 'High';
      case 'MEDIUM':
        return 'Medium';
      case 'LOW':
        return 'Low';
      case 'INFO':
        return 'Info';
      default:
        return severity;
    }
  };

  const getBadgeClass = () => {
    switch (normalized) {
      case 'CRITICAL':
        return 'badge-critical';
      case 'HIGH':
        return 'badge-high';
      case 'MEDIUM':
        return 'badge-medium';
      case 'LOW':
        return 'badge-low';
      case 'INFO':
      default:
        return 'badge-info';
    }
  };

  return (
    <span className={`badge ${getBadgeClass()} severity-badge ${className}`}>
      <span className="badge-dot" aria-hidden="true" />
      {getLabel()}
    </span>
  );
};

export default SeverityBadge;
