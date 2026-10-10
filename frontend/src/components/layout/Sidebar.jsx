import React from 'react';
import { NavLink } from 'react-router-dom';

const navItems = [
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
      </svg>
    )
  },
  {
    path: '/repositories',
    label: 'Repositories',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
        <circle cx="12" cy="13" r="2" />
        <path d="M14 13h3" />
        <path d="M7 13h3" />
      </svg>
    )
  },
  {
    path: '/reviews',
    label: 'Reviews',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="18" cy="18" r="3" />
        <circle cx="6" cy="6" r="3" />
        <path d="M13 6h3a2 2 0 0 1 2 2v7" />
        <line x1="6" y1="9" x2="6" y2="21" />
      </svg>
    )
  },
  {
    path: '/findings',
    label: 'Findings',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    )
  },
  {
    path: '/analytics',
    label: 'Analytics',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="10" />
        <line x1="12" y1="20" x2="12" y2="4" />
        <line x1="6" y1="20" x2="6" y2="14" />
      </svg>
    )
  },
  {
    path: '/settings',
    label: 'Settings',
    icon: (
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
    )
  }
];

export const Sidebar = ({
  collapsed = false,
  onToggle,
  mobileOpen = false,
  onMobileClose
}) => {
  return (
    <>
      {/* Mobile overlay backdrop */}
      {mobileOpen && (
        <div
          className="sidebar-backdrop"
          onClick={onMobileClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`sidebar ${collapsed ? 'collapsed' : 'expanded'} ${mobileOpen ? 'mobile-open' : ''}`}
        aria-label="Application navigation"
      >
        {/* Sidebar Brand Header */}
        <div className="sidebar-header">
          <NavLink to="/dashboard" className="sidebar-brand" onClick={onMobileClose}>
            <div className="sidebar-logo-icon" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="10" rx="2" />
                <circle cx="12" cy="5" r="2" />
                <path d="M12 7v4" />
                <line x1="8" y1="16" x2="8" y2="16" />
                <line x1="16" y1="16" x2="16" y2="16" />
              </svg>
            </div>
            {!collapsed && (
              <div className="sidebar-brand-text">
                <span className="sidebar-title">CodeReview</span>
                <span className="sidebar-badge">V2</span>
              </div>
            )}
          </NavLink>

          {/* Mobile close button */}
          <button
            type="button"
            className="sidebar-mobile-close"
            onClick={onMobileClose}
            aria-label="Close sidebar navigation"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Navigation Items */}
        <nav className="sidebar-nav">
          <ul className="sidebar-nav-list">
            {navItems.map((item) => (
              <li key={item.path} className="sidebar-nav-item-wrapper">
                <NavLink
                  to={item.path}
                  end={item.path === '/reviews'}
                  onClick={onMobileClose}
                  className={({ isActive }) =>
                    `sidebar-nav-item ${isActive ? 'active' : ''}`
                  }
                  title={collapsed ? item.label : undefined}
                  aria-label={collapsed ? item.label : undefined}
                >
                  <span className="sidebar-nav-icon" aria-hidden="true">
                    {item.icon}
                  </span>
                  {!collapsed && (
                    <span className="sidebar-nav-label">{item.label}</span>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        {/* Sidebar Footer with Collapse / Expand Toggle */}
        <div className="sidebar-footer">
          <button
            type="button"
            className="sidebar-collapse-btn"
            onClick={onToggle}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-expanded={!collapsed}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <span className="sidebar-collapse-icon" aria-hidden="true">
              {collapsed ? (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              ) : (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
              )}
            </span>
            {!collapsed && (
              <span className="sidebar-collapse-label">Collapse</span>
            )}
          </button>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
