import React from 'react';

export const StatCard = ({
  title,
  value,
  description,
  icon,
  iconBg = 'var(--primary-light)',
  iconColor = 'var(--primary-color)',
  valueColor,
  className = ''
}) => {
  return (
    <div className={`stat-card ${className}`}>
      <div className="stat-card-header">
        <span className="stat-card-title">{title}</span>
        {icon && (
          <div
            className="stat-card-icon"
            style={{ backgroundColor: iconBg, color: iconColor }}
            aria-hidden="true"
          >
            {icon}
          </div>
        )}
      </div>
      <div className="stat-card-body">
        <div
          className="stat-card-value"
          style={valueColor ? { color: valueColor } : undefined}
        >
          {value !== undefined && value !== null ? value : 0}
        </div>
        {description && <div className="stat-card-description">{description}</div>}
      </div>
    </div>
  );
};

export default StatCard;
