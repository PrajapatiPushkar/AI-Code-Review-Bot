import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import repositoryService from '../services/repositoryService';
import useToast from '../hooks/useToast';
import InstallationSelector from '../components/repositories/InstallationSelector';
import RepositoryFilters from '../components/repositories/RepositoryFilters';
import RepositoryGrid from '../components/repositories/RepositoryGrid';
import RepositorySkeleton from '../components/repositories/RepositorySkeleton';
import EmptyState from '../components/EmptyState';
import ErrorMessage from '../components/ErrorMessage';

export const RepositoriesPage = () => {
  const toast = useToast();

  // Installations state
  const [installations, setInstallations] = useState([]);
  const [selectedInstallation, setSelectedInstallation] = useState(null);
  const [loadingInstallations, setLoadingInstallations] = useState(true);
  const [installationError, setInstallationError] = useState(null);

  // Repositories state
  const [repositories, setRepositories] = useState([]);
  const [loadingRepositories, setLoadingRepositories] = useState(false);
  const [repositoryError, setRepositoryError] = useState(null);

  // Client-side filtering state
  const [searchTerm, setSearchTerm] = useState('');
  const [visibilityFilter, setVisibilityFilter] = useState('ALL'); // 'ALL' | 'PUBLIC' | 'PRIVATE'
  const [sortOption, setSortOption] = useState('name-asc'); // 'name-asc' | 'name-desc'

  // Ref to track latest requested installation to prevent stale race conditions
  const activeFetchIdRef = useRef(null);

  // Fetch installations on mount
  const fetchInstallations = useCallback(async () => {
    try {
      setLoadingInstallations(true);
      setInstallationError(null);

      const data = await repositoryService.getInstallations();
      const list = Array.isArray(data) ? data : [];
      setInstallations(list);

      if (list.length > 0) {
        // Auto-select first installation if none selected or current selection not in list
        setSelectedInstallation((prev) => {
          if (!prev) return list[0];
          const exists = list.find(
            (item) =>
              (item.githubInstallationId && item.githubInstallationId === prev.githubInstallationId) ||
              (item.id && item.id === prev.id)
          );
          return exists || list[0];
        });
      } else {
        setSelectedInstallation(null);
        setRepositories([]);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to load GitHub installations.';
      setInstallationError(msg);
      toast?.error?.(msg);
    } finally {
      setLoadingInstallations(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchInstallations();
  }, [fetchInstallations]);

  // Fetch repositories whenever selectedInstallation changes
  const fetchRepositories = useCallback(async (installation) => {
    if (!installation) {
      setRepositories([]);
      return;
    }

    const targetId = installation.githubInstallationId || installation.id;
    activeFetchIdRef.current = targetId;

    try {
      setLoadingRepositories(true);
      setRepositoryError(null);

      const data = await repositoryService.getRepositories(targetId, { page: 1, perPage: 100 });
      
      // Check if this request is still the active one
      if (activeFetchIdRef.current === targetId) {
        setRepositories(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      if (activeFetchIdRef.current === targetId) {
        const msg = err.response?.data?.message || 'Failed to load repositories for the selected installation.';
        setRepositoryError(msg);
        toast?.error?.(msg);
      }
    } finally {
      if (activeFetchIdRef.current === targetId) {
        setLoadingRepositories(false);
      }
    }
  }, [toast]);

  useEffect(() => {
    if (selectedInstallation) {
      fetchRepositories(selectedInstallation);
    }
  }, [selectedInstallation, fetchRepositories]);

  // Handle switching installations
  const handleSelectInstallation = (installation) => {
    if (!installation) return;
    if (
      selectedInstallation &&
      (selectedInstallation.githubInstallationId || selectedInstallation.id) ===
        (installation.githubInstallationId || installation.id)
    ) {
      return;
    }
    // Clear search and reset filters on installation switch
    setSearchTerm('');
    setVisibilityFilter('ALL');
    setSortOption('name-asc');
    setSelectedInstallation(installation);
  };

  // Client-side filtering & sorting
  const filteredRepositories = useMemo(() => {
    let result = [...repositories];

    // Search filter
    if (searchTerm.trim()) {
      const term = searchTerm.trim().toLowerCase();
      result = result.filter((repo) => {
        const name = (repo.name || '').toLowerCase();
        const fullName = (repo.full_name || repo.fullName || '').toLowerCase();
        return name.includes(term) || fullName.includes(term);
      });
    }

    // Visibility filter
    if (visibilityFilter !== 'ALL') {
      const wantPrivate = visibilityFilter === 'PRIVATE';
      result = result.filter((repo) => {
        const isPriv = repo.private ?? repo.isPrivate ?? false;
        return isPriv === wantPrivate;
      });
    }

    // Sorting
    result.sort((a, b) => {
      const nameA = (a.name || '').toLowerCase();
      const nameB = (b.name || '').toLowerCase();
      if (sortOption === 'name-desc') {
        return nameB.localeCompare(nameA);
      }
      return nameA.localeCompare(nameB);
    });

    return result;
  }, [repositories, searchTerm, visibilityFilter, sortOption]);

  const handleResetFilters = () => {
    setSearchTerm('');
    setVisibilityFilter('ALL');
    setSortOption('name-asc');
  };

  const activeInstallationId = selectedInstallation?.githubInstallationId || selectedInstallation?.id;
  const activeInstallationAccount = selectedInstallation?.githubAccountLogin;

  return (
    <div className="repositories-page-container">
      {/* Page Header */}
      <div className="page-header repositories-page-header">
        <div className="page-header-content">
          <div className="page-header-title-row">
            <h1 className="page-title">Repositories</h1>
            {!loadingInstallations && installations.length > 0 && (
              <div className="header-badges-row">
                <span className="page-header-count-pill" title="Connected GitHub App Installations">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
                  </svg>
                  <span>
                    {installations.length} {installations.length === 1 ? 'Installation' : 'Installations'}
                  </span>
                </span>
                {!loadingRepositories && !repositoryError && repositories.length > 0 && (
                  <span className="page-header-count-pill" title="Repositories accessible in active installation">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
                      <circle cx="12" cy="13" r="2" />
                      <path d="M14 13h3" />
                      <path d="M7 13h3" />
                    </svg>
                    <span>
                      {repositories.length} {repositories.length === 1 ? 'Repository' : 'Repositories'}
                    </span>
                  </span>
                )}
              </div>
            )}
          </div>
          <p className="page-subtitle">
            GitHub repositories available through your connected installations.
          </p>
        </div>

        <div className="page-header-actions">
          <Link to="/reviews/new" className="btn btn-primary repositories-action-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Submit Review</span>
          </Link>
          <Link to="/reviews" className="btn btn-outline repositories-action-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="18" cy="18" r="3" />
              <circle cx="6" cy="6" r="3" />
              <path d="M13 6h3a2 2 0 0 1 2 2v7" />
              <line x1="6" y1="9" x2="6" y2="21" />
            </svg>
            <span>Review History</span>
          </Link>
        </div>
      </div>

      {/* Installation Error */}
      {installationError && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorMessage
            message={installationError}
            onRetry={fetchInstallations}
          />
        </div>
      )}

      {/* Main Content Area */}
      {loadingInstallations ? (
        <div className="card repositories-loading-card">
          <div className="installation-selector-loading">
            <span className="installation-spinner" aria-hidden="true" />
            <span>Connecting to GitHub App installations...</span>
          </div>
          <div style={{ marginTop: '1.5rem' }}>
            <RepositorySkeleton count={3} />
          </div>
        </div>
      ) : installations.length === 0 ? (
        /* Empty State A: No GitHub installations connected */
        <EmptyState
          title="No GitHub App Installations Found"
          message="No GitHub App installation is currently connected to your account. Authorize the GitHub App to access your organizations and repositories."
          icon={
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
            </svg>
          }
          action={
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={fetchInstallations}
              >
                Check Again
              </button>
              <Link to="/reviews/new" className="btn btn-primary">
                Manual Review Submission →
              </Link>
            </div>
          }
        />
      ) : (
        <>
          {/* Installation Selector Bar */}
          <div className="card installation-bar-card">
            <InstallationSelector
              installations={installations}
              selectedInstallationId={activeInstallationId}
              onSelectInstallation={handleSelectInstallation}
              disabled={loadingRepositories}
            />
          </div>

          {/* Repository Error */}
          {repositoryError && (
            <div style={{ marginBottom: '1.5rem' }}>
              <ErrorMessage
                message={repositoryError}
                onRetry={() => fetchRepositories(selectedInstallation)}
              />
            </div>
          )}

          {/* Repositories Section */}
          {loadingRepositories ? (
            <div className="repositories-content-section">
              <RepositorySkeleton count={6} />
            </div>
          ) : repositories.length === 0 ? (
            /* Empty State B: Installation has no repositories */
            <EmptyState
              title="No Repositories Available"
              message={`The GitHub installation "${activeInstallationAccount || 'active account'}" does not currently have any authorized repositories.`}
              icon={
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
                  <circle cx="12" cy="13" r="2" />
                  <path d="M14 13h3" />
                  <path d="M7 13h3" />
                </svg>
              }
              action={
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => fetchRepositories(selectedInstallation)}
                >
                  Reload Repositories
                </button>
              }
            />
          ) : (
            <div className="repositories-content-section">
              {/* Search & Filters */}
              <RepositoryFilters
                searchTerm={searchTerm}
                onSearchChange={setSearchTerm}
                visibilityFilter={visibilityFilter}
                onVisibilityChange={setVisibilityFilter}
                sortOption={sortOption}
                onSortChange={setSortOption}
                totalCount={repositories.length}
                filteredCount={filteredRepositories.length}
                onReset={handleResetFilters}
              />

              {/* Repositories Grid or Search Empty State */}
              {filteredRepositories.length === 0 ? (
                /* Empty State C: Search returned zero results */
                <EmptyState
                  title="No Matching Repositories"
                  message={`No repositories match "${searchTerm || visibilityFilter}". Try adjusting your search query or visibility filter.`}
                  icon="🔍"
                  action={
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={handleResetFilters}
                    >
                      Clear All Filters
                    </button>
                  }
                />
              ) : (
                <RepositoryGrid
                  repositories={filteredRepositories}
                  installationId={activeInstallationId}
                  installationAccount={activeInstallationAccount}
                />
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default RepositoriesPage;
