import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import SettingsSection from './SettingsSection';

export const AccountSettings = () => {
  const { user } = useAuth();
  const identity = user?.usernameOrEmail || 'Authenticated User';

  return (
    <SettingsSection
      id="account"
      title="Account"
      description="Current authenticated identity and session account details."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      }
    >
      <div className="settings-items-list">
        <div className="settings-item-row">
          <div>
            <div className="settings-item-label">Account Identity</div>
            <div className="settings-item-help">The primary username or email used to authenticate.</div>
          </div>
          <div className="settings-item-value">
            <strong>{identity}</strong>
          </div>
        </div>

        <div className="settings-item-row">
          <div>
            <div className="settings-item-label">Account Status</div>
            <div className="settings-item-help">Current authorization state in the application session.</div>
          </div>
          <div className="settings-item-value">
            <span className="badge badge-completed">
              <span className="badge-dot" aria-hidden="true" />
              Active
            </span>
          </div>
        </div>

        <div className="settings-item-row">
          <div>
            <div className="settings-item-label">Authentication Type</div>
            <div className="settings-item-help">Protocol method used to verify identity.</div>
          </div>
          <div className="settings-item-value">
            <span className="settings-item-code">Bearer Token (JWT)</span>
          </div>
        </div>
      </div>

      <div className="settings-notice-banner" role="note">
        <svg
          className="settings-notice-icon"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
        <span>
          Profile editing is not currently available. User accounts and security roles are managed via the platform backend configuration.
        </span>
      </div>
    </SettingsSection>
  );
};

export default AccountSettings;
