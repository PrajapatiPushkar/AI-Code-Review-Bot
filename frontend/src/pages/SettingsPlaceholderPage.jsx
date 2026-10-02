import React from 'react';
import ThemeToggle from '../components/common/ThemeToggle';
import { useAuth } from '../hooks/useAuth';

const SettingsPlaceholderPage = () => {
  const { user } = useAuth();

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Settings</h1>
        <p className="page-subtitle">Platform preferences, authentication profiles, and workspace configuration.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem', maxWidth: '960px' }}>
        {/* Appearance Settings */}
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: '1rem' }}>Appearance & Theme</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1.25rem', lineHeight: '1.5' }}>
            Customize your interface theme. Your choice is automatically persisted locally across sessions.
          </p>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.75rem 1rem', backgroundColor: 'var(--bg-surface-elevated)', borderRadius: 'var(--radius)', border: '1px solid var(--border-color)' }}>
            <div>
              <strong style={{ fontSize: '0.875rem', display: 'block' }}>Interface Theme</strong>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Toggle between Obsidian Dark and Clean Light</span>
            </div>
            <ThemeToggle />
          </div>
        </div>

        {/* User Account Details */}
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: '1rem' }}>User Profile</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1.25rem', lineHeight: '1.5' }}>
            Current authenticated session details.
          </p>
          <table className="data-table">
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Account</td>
                <td><strong>{user?.usernameOrEmail || 'Authenticated User'}</strong></td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Status</td>
                <td><span className="badge badge-completed">Active</span></td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Platform Version</td>
                <td><code>V2.0-platform</code></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default SettingsPlaceholderPage;
