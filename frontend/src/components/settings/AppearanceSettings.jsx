import React from 'react';
import { useTheme } from '../../hooks/useTheme';
import { useToast } from '../../hooks/useToast';
import SettingsSection from './SettingsSection';

export const AppearanceSettings = () => {
  const { theme, setTheme, isDark } = useTheme();
  const toast = useToast();

  const handleSelectTheme = (newTheme) => {
    if (newTheme === theme) return;
    setTheme(newTheme);
    toast.success(`Theme preference updated to ${newTheme === 'dark' ? 'Dark' : 'Light'}`);
  };

  return (
    <SettingsSection
      id="appearance"
      title="Appearance & Theme"
      description="Select your preferred interface color mode. Your preference is saved locally across sessions."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
        </svg>
      }
    >
      <div className="settings-theme-grid" role="radiogroup" aria-label="Interface theme options">
        {/* Dark Theme Card */}
        <div
          role="radio"
          aria-checked={isDark}
          tabIndex={0}
          className={`settings-theme-card ${isDark ? 'active' : ''}`}
          onClick={() => handleSelectTheme('dark')}
          onKeyDown={(e) => {
            if (e.key === ' ' || e.key === 'Enter') {
              e.preventDefault();
              handleSelectTheme('dark');
            }
          }}
          aria-label="Dark Theme (Obsidian slate developer tool palette)"
        >
          <div className="settings-theme-preview-box preview-dark" aria-hidden="true">
            <div className="preview-sidebar" />
            <div className="preview-main">
              <div className="preview-line preview-line-title" />
              <div className="preview-line preview-line-sub" />
            </div>
          </div>

          <div className="settings-theme-header-row">
            <span className="settings-theme-name">Dark Theme</span>
            {isDark && (
              <span className="settings-theme-check" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </span>
            )}
          </div>
          <p className="settings-theme-desc">
            Deep slate palette optimized for long development sessions.
          </p>
        </div>

        {/* Light Theme Card */}
        <div
          role="radio"
          aria-checked={!isDark}
          tabIndex={0}
          className={`settings-theme-card ${!isDark ? 'active' : ''}`}
          onClick={() => handleSelectTheme('light')}
          onKeyDown={(e) => {
            if (e.key === ' ' || e.key === 'Enter') {
              e.preventDefault();
              handleSelectTheme('light');
            }
          }}
          aria-label="Light Theme (Clean high-contrast palette)"
        >
          <div className="settings-theme-preview-box preview-light" aria-hidden="true">
            <div className="preview-sidebar" />
            <div className="preview-main">
              <div className="preview-line preview-line-title" />
              <div className="preview-line preview-line-sub" />
            </div>
          </div>

          <div className="settings-theme-header-row">
            <span className="settings-theme-name">Light Theme</span>
            {!isDark && (
              <span className="settings-theme-check" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </span>
            )}
          </div>
          <p className="settings-theme-desc">
            High-contrast clean daylight palette for bright environments.
          </p>
        </div>
      </div>
    </SettingsSection>
  );
};

export default AppearanceSettings;
