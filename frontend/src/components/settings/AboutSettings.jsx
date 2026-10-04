import React from 'react';
import { Link } from 'react-router-dom';
import SettingsSection from './SettingsSection';

export const AboutSettings = () => {
  return (
    <SettingsSection
      id="about"
      title="About Platform"
      description="System architectural details, environment configuration, and technology stack."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      }
    >
      <div className="settings-about-table">
        <div className="settings-about-row">
          <span className="settings-about-label">Application</span>
          <span className="settings-about-value">AI Code Review Bot</span>
        </div>

        <div className="settings-about-row">
          <span className="settings-about-label">Platform Edition</span>
          <span className="settings-about-value">
            <span className="badge badge-in_progress" style={{ backgroundColor: 'var(--primary-light)', color: 'var(--primary-color)' }}>
              V2 Platform
            </span>
          </span>
        </div>

        <div className="settings-about-row">
          <span className="settings-about-label">Frontend Framework</span>
          <span className="settings-about-value">React 18 + Vite + React Router 6</span>
        </div>

        <div className="settings-about-row">
          <span className="settings-about-label">Backend Architecture</span>
          <span className="settings-about-value">Spring Boot 3 + Java 21 + PostgreSQL</span>
        </div>

        <div className="settings-about-row">
          <span className="settings-about-label">Review Execution</span>
          <span className="settings-about-value">Asynchronous AI Pipeline via GitHub App</span>
        </div>

        <div className="settings-about-row">
          <span className="settings-about-label">Design Engine</span>
          <span className="settings-about-value">Vanilla CSS Token System (Dark & Light)</span>
        </div>
      </div>

      <div style={{ marginTop: '1.5rem', display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
        <Link to="/dashboard" className="btn btn-outline btn-sm">
          Platform Dashboard →
        </Link>
        <Link to="/repositories" className="btn btn-outline btn-sm">
          Repository Explorer →
        </Link>
        <Link to="/reviews" className="btn btn-outline btn-sm">
          Review History →
        </Link>
      </div>
    </SettingsSection>
  );
};

export default AboutSettings;
