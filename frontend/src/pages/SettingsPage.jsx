import React, { useState, useEffect } from 'react';
import SettingsNav from '../components/settings/SettingsNav';
import AccountSettings from '../components/settings/AccountSettings';
import AppearanceSettings from '../components/settings/AppearanceSettings';
import PreferencesSettings from '../components/settings/PreferencesSettings';
import GitHubSettings from '../components/settings/GitHubSettings';
import SessionSettings from '../components/settings/SessionSettings';
import AboutSettings from '../components/settings/AboutSettings';

export const SettingsPage = () => {
  const [activeSection, setActiveSection] = useState('account');

  // Smooth scroll to section when nav tab is selected
  const handleSelectSection = (sectionId) => {
    setActiveSection(sectionId);
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  // Observe scroll position to update active nav tab dynamically
  useEffect(() => {
    const sectionIds = ['account', 'appearance', 'preferences', 'github', 'session', 'about'];
    const handleScroll = () => {
      const scrollPosition = window.scrollY + 120;
      for (let i = sectionIds.length - 1; i >= 0; i--) {
        const id = sectionIds[i];
        const el = document.getElementById(id);
        if (el && el.offsetTop <= scrollPosition) {
          setActiveSection(id);
          break;
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="settings-container">
      {/* Page Header */}
      <div className="page-header settings-page-header">
        <h1 className="page-title">Settings</h1>
        <p className="page-subtitle">
          Manage your account, appearance, and application preferences.
        </p>
      </div>

      {/* Main Settings Layout: Left Nav + Right Sections */}
      <div className="settings-layout">
        <aside>
          <SettingsNav
            activeSection={activeSection}
            onSelectSection={handleSelectSection}
          />
        </aside>

        <div className="settings-content-area">
          <AccountSettings />
          <AppearanceSettings />
          <PreferencesSettings />
          <GitHubSettings />
          <SessionSettings />
          <AboutSettings />
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
