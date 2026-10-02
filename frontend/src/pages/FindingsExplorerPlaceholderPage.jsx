import React from 'react';
import { Link } from 'react-router-dom';

const FindingsExplorerPlaceholderPage = () => {
  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Findings Explorer</h1>
          <p className="page-subtitle">Inspect code quality, security vulnerabilities, and bug recommendations across reviews.</p>
        </div>
        <Link to="/reviews" className="btn btn-outline btn-sm">
          ← View Review History
        </Link>
      </div>

      <div className="card" style={{ maxWidth: '720px', margin: '2rem auto', textAlign: 'center', padding: '3rem 2rem' }}>
        <div style={{ fontSize: '2.5rem', marginBottom: '1rem', color: 'var(--primary-color)' }}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ margin: '0 auto', display: 'block' }}>
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h2 className="card-title" style={{ justifyContent: 'center', marginBottom: '0.75rem', fontSize: '1.25rem' }}>
          Explore Review Findings
        </h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', lineHeight: '1.6', maxWidth: '540px', margin: '0 auto 1.5rem auto' }}>
          AI code review findings are linked directly to each analyzed pull request.
          Select any completed review from the Review History to explore its findings, severity metrics, and AI recommendations.
        </p>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/reviews" className="btn btn-primary">
            Go to Review History →
          </Link>
          <Link to="/reviews/new" className="btn btn-outline">
            Submit New Review
          </Link>
        </div>
      </div>
    </div>
  );
};

export default FindingsExplorerPlaceholderPage;
