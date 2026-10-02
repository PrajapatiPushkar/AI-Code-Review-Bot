import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopNavbar from './TopNavbar';

const SIDEBAR_STORAGE_KEY = 'ai-code-review-sidebar-collapsed';

export const AppShell = ({ children }) => {
  const [collapsed, setCollapsed] = useState(() => {
    try {
      const saved = localStorage.getItem(SIDEBAR_STORAGE_KEY);
      return saved === 'true';
    } catch {
      return false;
    }
  });

  const [mobileOpen, setMobileOpen] = useState(false);

  // Synchronize collapsed state with localStorage
  const toggleSidebar = () => {
    setCollapsed((prev) => {
      const nextState = !prev;
      try {
        localStorage.setItem(SIDEBAR_STORAGE_KEY, String(nextState));
      } catch {
        // Ignore storage errors
      }
      return nextState;
    });
  };

  // Close mobile drawer on escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && mobileOpen) {
        setMobileOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileOpen]);

  return (
    <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : 'sidebar-expanded'}`}>
      <Sidebar
        collapsed={collapsed}
        onToggle={toggleSidebar}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
      />
      <div className="app-main">
        <TopNavbar onMobileMenuToggle={() => setMobileOpen((prev) => !prev)} />
        <main className="main-content" id="main-content" tabIndex="-1">
          {children || <Outlet />}
        </main>
      </div>
    </div>
  );
};

export default AppShell;
