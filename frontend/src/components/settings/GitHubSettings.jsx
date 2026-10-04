import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import repositoryService from '../../services/repositoryService';
import LoadingSkeleton from '../common/LoadingSkeleton';
import ErrorMessage from '../ErrorMessage';
import SettingsSection from './SettingsSection';

export const GitHubSettings = () => {
  const [installations, setInstallations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchInstallations = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await repositoryService.getInstallations();
      setInstallations(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch connected GitHub App installations.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchInstallations();
  }, [fetchInstallations]);

  return (
    <SettingsSection
      id="github"
      title="GitHub Integration"
      description="View connected GitHub App installations and authorized organization access."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
        </svg>
      }
    >
      {error && (
        <div style={{ marginBottom: '1.25rem' }}>
          <ErrorMessage message={error} onRetry={fetchInstallations} />
        </div>
      )}

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }} aria-busy="true" aria-label="Loading connected installations">
          <LoadingSkeleton variant="rounded" height="68px" borderRadius="8px" />
          <LoadingSkeleton variant="rounded" height="68px" borderRadius="8px" />
        </div>
      ) : installations.length === 0 ? (
        <div className="empty-card" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
          <div className="empty-icon" aria-hidden="true" style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>
            🐙
          </div>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.375rem' }}>
            No GitHub App Installations Found
          </h3>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', maxWidth: '420px', margin: '0 auto 1.25rem auto', lineHeight: '1.45' }}>
            No authorized GitHub App installations are currently associated with your environment. Install the app on your GitHub account or organization to enable automated PR reviews.
          </p>
          <Link to="/repositories" className="btn btn-secondary btn-sm">
            View Repositories Explorer →
          </Link>
        </div>
      ) : (
        <div className="settings-installation-list">
          {installations.map((inst) => {
            const instId = String(inst.githubInstallationId || inst.id);
            const accountLogin = inst.githubAccountLogin || 'Connected Account';
            const targetType = inst.targetType || 'User / Org';

            return (
              <div key={instId} className="settings-installation-item">
                <div className="settings-installation-identity">
                  <div className="settings-installation-icon" aria-hidden="true">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
                    </svg>
                  </div>
                  <div>
                    <h3 className="settings-installation-name">{accountLogin}</h3>
                    <div className="settings-installation-meta">
                      <span><strong>Type:</strong> {targetType}</span>
                      <span><strong>Installation ID:</strong> {instId}</span>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <span className="badge badge-completed">
                    <span className="badge-dot" aria-hidden="true" />
                    Connected
                  </span>
                  <Link
                    to="/repositories"
                    className="btn btn-outline btn-sm"
                    title={`View repositories for ${accountLogin}`}
                  >
                    Repositories →
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

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
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        <span>
          GitHub App authentication credentials and private keys are stored securely on the backend server and are never exposed to the browser client.
        </span>
      </div>
    </SettingsSection>
  );
};

export default GitHubSettings;
