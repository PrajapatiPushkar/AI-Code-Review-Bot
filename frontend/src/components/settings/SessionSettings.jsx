import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import SettingsSection from './SettingsSection';

export const SessionSettings = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [confirmSignOut, setConfirmSignOut] = useState(false);

  const handleSignOut = () => {
    logout();
    toast.info('You have signed out successfully.');
    navigate('/login');
  };

  return (
    <SettingsSection
      id="session"
      title="Security & Session"
      description="Manage your active login session and platform access."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      }
    >
      <div className="settings-items-list" style={{ marginBottom: '1.5rem' }}>
        <div className="settings-item-row">
          <div>
            <div className="settings-item-label">Current Session Identity</div>
            <div className="settings-item-help">The user identifier associated with the active session.</div>
          </div>
          <div className="settings-item-value">
            <strong>{user?.usernameOrEmail || 'Authenticated User'}</strong>
          </div>
        </div>

        <div className="settings-item-row">
          <div>
            <div className="settings-item-label">Session Protection</div>
            <div className="settings-item-help">Authorization headers are attached to API requests automatically.</div>
          </div>
          <div className="settings-item-value">
            <span className="badge badge-completed">
              <span className="badge-dot" aria-hidden="true" />
              Protected
            </span>
          </div>
        </div>
      </div>

      {/* Sign Out Card */}
      <div className="card settings-signout-card">
        <div className="settings-signout-header">
          <div>
            <h3 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 0.25rem 0' }}>
              Sign Out of Account
            </h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
              End your active browser session. Stored authorization credentials will be cleared immediately.
            </p>
          </div>

          {!confirmSignOut && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setConfirmSignOut(true)}
              style={{ color: 'var(--status-failed)', borderColor: 'rgba(239, 68, 68, 0.4)' }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              <span>Sign Out</span>
            </button>
          )}
        </div>

        {confirmSignOut && (
          <div className="settings-signout-warning" role="alert">
            <p className="settings-signout-warning-text">
              <strong>Are you sure you want to sign out?</strong> You will need to sign in with your credentials to access reviews, repositories, and findings.
            </p>
            <div className="settings-signout-actions">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setConfirmSignOut(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={handleSignOut}
                style={{ backgroundColor: 'var(--status-failed)', borderColor: 'var(--status-failed)' }}
              >
                Confirm Sign Out
              </button>
            </div>
          </div>
        )}
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
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
        <span>
          Your session is managed securely by the application. Client-side authentication tokens are used for API requests and purged upon sign out.
        </span>
      </div>
    </SettingsSection>
  );
};

export default SessionSettings;
