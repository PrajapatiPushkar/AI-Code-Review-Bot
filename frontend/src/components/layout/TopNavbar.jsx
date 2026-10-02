import React from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import ThemeToggle from '../common/ThemeToggle';

export const TopNavbar = ({ onMobileMenuToggle }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Derive human-readable page context based on route
  const getContextTitle = () => {
    const path = location.pathname;
    if (path.startsWith('/dashboard')) return 'Dashboard';
    if (path.startsWith('/repositories')) return 'Repositories';
    if (path.startsWith('/reviews/new')) return 'Submit Review';
    if (path.includes('/findings')) return 'Review Findings';
    if (path.startsWith('/reviews/')) return 'Review Details';
    if (path.startsWith('/reviews')) return 'Review History';
    if (path.startsWith('/findings')) return 'Findings Explorer';
    if (path.startsWith('/settings')) return 'Settings';
    return 'Platform';
  };

  return (
    <header className="top-navbar" aria-label="Top navigation bar">
      <div className="top-navbar-left">
        {/* Mobile Hamburger Menu Toggle */}
        <button
          type="button"
          className="mobile-menu-btn"
          onClick={onMobileMenuToggle}
          aria-label="Open sidebar navigation"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        {/* Breadcrumb Context Indicator */}
        <div className="navbar-context-indicator">
          <span className="navbar-context-root">AI Code Review</span>
          <span className="navbar-context-separator">/</span>
          <span className="navbar-context-current">{getContextTitle()}</span>
        </div>
      </div>

      <div className="top-navbar-right">
        {/* Fast Action: New Review Button */}
        <Link
          to="/reviews/new"
          className="btn btn-primary btn-sm top-navbar-action"
          title="Submit a pull request for review"
        >
          <span>+</span>
          <span className="top-navbar-action-text">New Review</span>
        </Link>

        {/* Theme Mode Switcher */}
        <ThemeToggle />

        {/* Authenticated User Identity */}
        {user && (
          <div className="top-navbar-user-badge" title={`Signed in as ${user.usernameOrEmail}`}>
            <span className="user-avatar-initial" aria-hidden="true">
              {(user.usernameOrEmail || 'U').charAt(0).toUpperCase()}
            </span>
            <span className="user-display-name">
              {user.usernameOrEmail}
            </span>
          </div>
        )}

        {/* Logout Trigger */}
        <button
          type="button"
          className="btn btn-secondary btn-sm top-navbar-logout"
          onClick={handleLogout}
          aria-label="Sign out of account"
          title="Sign out"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          <span className="logout-text">Logout</span>
        </button>
      </div>
    </header>
  );
};

export default TopNavbar;
