import React from 'react';

export const SettingsSection = ({
  id,
  title,
  description,
  icon,
  children,
  className = ''
}) => {
  return (
    <section
      id={id}
      className={`settings-section ${className}`}
      aria-labelledby={`heading-${id}`}
    >
      <div className="settings-section-card">
        <div className="settings-section-header">
          {icon && <div className="settings-section-icon" aria-hidden="true">{icon}</div>}
          <div className="settings-section-header-text">
            <h2 id={`heading-${id}`} className="settings-section-title">
              {title}
            </h2>
            {description && (
              <p className="settings-section-desc">
                {description}
              </p>
            )}
          </div>
        </div>

        <div className="settings-section-body">
          {children}
        </div>
      </div>
    </section>
  );
};

export default SettingsSection;
