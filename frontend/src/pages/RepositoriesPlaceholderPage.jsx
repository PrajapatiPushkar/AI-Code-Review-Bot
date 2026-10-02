import React from 'react';
import { Link } from 'react-router-dom';

const RepositoriesPlaceholderPage = () => {
  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Repositories</h1>
          <p className="page-subtitle">Connected GitHub repositories and automated review configurations.</p>
        </div>
        <Link to="/reviews/new" className="btn btn-primary">
          + Submit Pull Request Review
        </Link>
      </div>

      <div className="card" style={{ maxWidth: '720px', margin: '2rem auto', textAlign: 'center', padding: '3rem 2rem' }}>
        <div style={{ fontSize: '2.5rem', marginBottom: '1rem', color: 'var(--primary-color)' }}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ margin: '0 auto', display: 'block' }}>
            <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
            <circle cx="12" cy="13" r="2" />
            <path d="M14 13h3" />
            <path d="M7 13h3" />
          </svg>
        </div>
        <h2 className="card-title" style={{ justifyContent: 'center', marginBottom: '0.75rem', fontSize: '1.25rem' }}>
          Repository Integrations
        </h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', lineHeight: '1.6', maxWidth: '540px', margin: '0 auto 1.5rem auto' }}>
          Direct GitHub App repository synchronization is scheduled for the upcoming V2 release.
          You can currently review pull requests for any authorized repository by submitting a review request.
        </p>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/reviews/new" className="btn btn-primary">
            Submit Review Request →
          </Link>
          <Link to="/reviews" className="btn btn-outline">
            Browse Review History
          </Link>
        </div>
      </div>
    </div>
  );
};

export default RepositoriesPlaceholderPage;
