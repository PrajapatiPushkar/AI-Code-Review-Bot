import React, { useState, useEffect } from 'react';
import { useToast } from '../../hooks/useToast';
import SettingsSection from './SettingsSection';

const SIDEBAR_STORAGE_KEY = 'ai-code-review-sidebar-collapsed';
const REDUCED_MOTION_KEY = 'ai-code-review-reduced-motion';

export const PreferencesSettings = () => {
  const toast = useToast();

  // Sidebar default collapsed preference
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try {
      return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });

  // Reduced motion preference
  const [reducedMotion, setReducedMotion] = useState(() => {
    try {
      return localStorage.getItem(REDUCED_MOTION_KEY) === 'true';
    } catch {
      return false;
    }
  });

  // Handle toggling sidebar startup preference
  const handleToggleSidebar = (e) => {
    const nextVal = e.target.checked;
    setSidebarCollapsed(nextVal);
    try {
      localStorage.setItem(SIDEBAR_STORAGE_KEY, String(nextVal));
      toast.success(
        nextVal
          ? 'Sidebar preference updated: Will start collapsed'
          : 'Sidebar preference updated: Will start expanded'
      );
    } catch {
      // Storage unavailable
    }
  };

  // Handle toggling reduced motion preference
  const handleToggleReducedMotion = (e) => {
    const nextVal = e.target.checked;
    setReducedMotion(nextVal);
    try {
      localStorage.setItem(REDUCED_MOTION_KEY, String(nextVal));
      if (nextVal) {
        document.documentElement.setAttribute('data-reduced-motion', 'true');
      } else {
        document.documentElement.removeAttribute('data-reduced-motion');
      }
      toast.success(
        nextVal
          ? 'Reduced motion enabled'
          : 'Standard motion enabled'
      );
    } catch {
      // Storage unavailable
    }
  };

  return (
    <SettingsSection
      id="preferences"
      title="Application Preferences"
      description="Configure workspace layout and accessibility preferences for your client session."
      icon={
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="4" y1="21" x2="4" y2="14" />
          <line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" />
          <line x1="20" y1="12" x2="20" y2="3" />
          <line x1="1" y1="14" x2="7" y2="14" />
          <line x1="9" y1="8" x2="15" y2="8" />
          <line x1="17" y1="16" x2="23" y2="16" />
        </svg>
      }
    >
      <div className="settings-pref-list">
        {/* Sidebar Startup State */}
        <div className="settings-pref-row">
          <div className="settings-pref-info">
            <label htmlFor="pref-sidebar-collapsed" className="settings-pref-title" style={{ display: 'block', cursor: 'pointer' }}>
              Start with Sidebar Collapsed
            </label>
            <p className="settings-pref-sub">
              Automatically keep navigation compact on initial page load to maximize code workspace width.
            </p>
          </div>
          <div className="settings-pref-switch-label">
            <input
              id="pref-sidebar-collapsed"
              type="checkbox"
              className="settings-pref-checkbox"
              checked={sidebarCollapsed}
              onChange={handleToggleSidebar}
              aria-label="Start with Sidebar Collapsed"
            />
          </div>
        </div>

        {/* Reduced Motion Toggle */}
        <div className="settings-pref-row">
          <div className="settings-pref-info">
            <label htmlFor="pref-reduced-motion" className="settings-pref-title" style={{ display: 'block', cursor: 'pointer' }}>
              Prefer Reduced Motion
            </label>
            <p className="settings-pref-sub">
              Minimize non-essential decorative animations and transitions across application screens.
            </p>
          </div>
          <div className="settings-pref-switch-label">
            <input
              id="pref-reduced-motion"
              type="checkbox"
              className="settings-pref-checkbox"
              checked={reducedMotion}
              onChange={handleToggleReducedMotion}
              aria-label="Prefer Reduced Motion"
            />
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
          These workspace preferences are saved directly to your browser's local storage and persist automatically.
        </span>
      </div>
    </SettingsSection>
  );
};

export default PreferencesSettings;
