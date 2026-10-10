import React from 'react';
import { useNavigate } from 'react-router-dom';

export const RepositoryCard = ({
  repository,
  installationId,
  installationAccount,
  onOpenPolicy
}) => {
  const navigate = useNavigate();

  if (!repository) return null;

  const repoName = repository.name || 'Unnamed Repository';
  const fullName = repository.full_name || repository.fullName || (installationAccount ? `${installationAccount}/${repoName}` : repoName);
  
  // Extract owner
  const owner = fullName.includes('/')
    ? fullName.split('/')[0]
    : (installationAccount || '');

  const isPrivate = repository.private ?? repository.isPrivate ?? false;
  const defaultBranch = repository.default_branch || repository.defaultBranch || 'main';
  const githubUrl = repository.html_url || repository.htmlUrl || `https://github.com/${fullName}`;
  const repoId = repository.id;

  const handleViewReviews = () => {
    navigate(`/reviews?owner=${encodeURIComponent(owner)}&repository=${encodeURIComponent(repoName)}`);
  };

  const handleStartReview = () => {
    navigate('/reviews/new', {
      state: {
        installationId: installationId ? String(installationId) : '',
        owner,
        repository: repoName
      }
    });
  };

  return (
    <article className="repository-card" aria-labelledby={`repo-title-${repoId || repoName}`}>
      <div className="repository-card-header">
        <div className="repository-title-group">
          <div className="repository-icon-wrapper" aria-hidden="true">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
              <circle cx="12" cy="13" r="2" />
              <path d="M14 13h3" />
              <path d="M7 13h3" />
            </svg>
          </div>
          <div className="repository-title-text">
            <h2 id={`repo-title-${repoId || repoName}`} className="repository-name" title={repoName}>
              {repoName}
            </h2>
            <span className="repository-full-name" title={fullName}>
              {fullName}
            </span>
          </div>
        </div>

        <a
          href={githubUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="repository-github-link"
          title={`View ${fullName} on GitHub (opens in new tab)`}
          aria-label={`View ${fullName} on GitHub (opens in new tab)`}
        >
          <svg
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
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </a>
      </div>

      <div className="repository-meta-row">
        {/* Private / Public Pill */}
        <span className={`repo-badge ${isPrivate ? 'repo-badge-private' : 'repo-badge-public'}`}>
          {isPrivate ? (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <span>Private</span>
            </>
          ) : (
            <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10" />
                <line x1="2" y1="12" x2="22" y2="12" />
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              </svg>
              <span>Public</span>
            </>
          )}
        </span>

        {/* Default Branch */}
        {defaultBranch && (
          <span className="repo-badge repo-badge-branch" title={`Default branch: ${defaultBranch}`}>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <line x1="6" y1="3" x2="6" y2="15" />
              <circle cx="18" cy="6" r="3" />
              <circle cx="6" cy="18" r="3" />
              <path d="M18 9a9 9 0 0 1-9 9" />
            </svg>
            <span>{defaultBranch}</span>
          </span>
        )}

        {/* Repository ID */}
        {repoId && (
          <span className="repo-badge repo-badge-id" title={`GitHub Repository ID: ${repoId}`}>
            ID: {repoId}
          </span>
        )}
      </div>

      <div className="repository-card-actions">
        <button
          type="button"
          onClick={handleViewReviews}
          className="btn btn-outline btn-sm repository-action-btn"
          aria-label={`View review history for ${repoName}`}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="18" cy="18" r="3" />
            <circle cx="6" cy="6" r="3" />
            <path d="M13 6h3a2 2 0 0 1 2 2v7" />
            <line x1="6" y1="9" x2="6" y2="21" />
          </svg>
          <span>View Reviews</span>
        </button>

        {onOpenPolicy && (
          <button
            type="button"
            onClick={() => onOpenPolicy(repository)}
            className="btn btn-outline btn-sm repository-action-btn"
            aria-label={`Configure review policy and quality gates for ${repoName}`}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            <span>Review Policy</span>
          </button>
        )}

        <button
          type="button"
          onClick={handleStartReview}
          className="btn btn-primary btn-sm repository-action-btn"
          aria-label={`Start new review for ${repoName}`}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>Review PR</span>
        </button>
      </div>
    </article>
  );
};

export default RepositoryCard;
