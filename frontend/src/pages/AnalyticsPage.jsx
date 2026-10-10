import React, { useState, useEffect, useCallback, useRef } from 'react';
import analyticsService from '../services/analyticsService';
import repositoryService from '../services/repositoryService';
import useToast from '../hooks/useToast';
import AnalyticsFilters from '../components/analytics/AnalyticsFilters';
import OverviewCards from '../components/analytics/OverviewCards';
import FindingsDistributionCard from '../components/analytics/FindingsDistributionCard';
import ActivityTrendsCard from '../components/analytics/ActivityTrendsCard';
import RepositoryHealthTable from '../components/analytics/RepositoryHealthTable';
import AnalyticsSkeleton from '../components/analytics/AnalyticsSkeleton';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

export const AnalyticsPage = () => {
  const toast = useToast();

  // Filters State
  const [filters, setFilters] = useState(() => {
    // Default to last 14 days
    const now = new Date();
    const past = new Date(now.getTime() - 13 * 24 * 60 * 60 * 1000);
    return {
      from: past.toISOString().split('T')[0],
      to: now.toISOString().split('T')[0],
      repository: ''
    };
  });

  // Repositories for dropdown
  const [availableRepositories, setAvailableRepositories] = useState([]);

  // Data State
  const [overview, setOverview] = useState(null);
  const [findings, setFindings] = useState(null);
  const [trends, setTrends] = useState([]);
  const [repositories, setRepositories] = useState([]);

  // Loading & Error States
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Stale request race condition prevention
  const activeRequestIdRef = useRef(0);

  // Load connected repositories for dropdown
  useEffect(() => {
    const fetchConnectedRepos = async () => {
      try {
        const installations = await repositoryService.getInstallations();
        if (Array.isArray(installations) && installations.length > 0) {
          const firstInst = installations[0];
          const instId = firstInst.githubInstallationId || firstInst.id;
          const repos = await repositoryService.getRepositories(instId);
          if (Array.isArray(repos)) {
            setAvailableRepositories(repos);
          }
        }
      } catch (err) {
        // Silent catch for dropdown list
      }
    };
    fetchConnectedRepos();
  }, []);

  // Fetch all analytics in parallel
  const fetchAnalytics = useCallback(async (currentFilters) => {
    const requestId = ++activeRequestIdRef.current;

    try {
      setLoading(true);
      setError(null);

      const params = {};
      if (currentFilters.from) params.from = currentFilters.from;
      if (currentFilters.to) params.to = currentFilters.to;
      if (currentFilters.repository) params.repository = currentFilters.repository;

      const [overviewData, findingsData, trendsData, reposData] = await Promise.all([
        analyticsService.getOverview(params),
        analyticsService.getFindings(params),
        analyticsService.getTrends(params),
        analyticsService.getRepositories(params)
      ]);

      // Only update state if this is still the active request
      if (requestId === activeRequestIdRef.current) {
        setOverview(overviewData);
        setFindings(findingsData);
        setTrends(Array.isArray(trendsData) ? trendsData : []);
        setRepositories(Array.isArray(reposData) ? reposData : []);
      }
    } catch (err) {
      if (requestId === activeRequestIdRef.current) {
        const msg = err.response?.data?.message || 'Failed to load review analytics.';
        setError(msg);
        toast?.error?.(msg);
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [toast]);

  useEffect(() => {
    fetchAnalytics(filters);
  }, [filters, fetchAnalytics]);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
  };

  const handleSelectRepository = (repoIdentifier) => {
    const updated = { ...filters, repository: repoIdentifier };
    setFilters(updated);
  };

  // Determine if total dataset is empty
  const isCompletelyEmpty =
    !loading &&
    !error &&
    overview?.totalReviews === 0 &&
    overview?.totalFindings === 0 &&
    repositories.length === 0;

  return (
    <div className="analytics-page">
      {/* Header */}
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 className="page-title">Review Analytics & Health</h1>
          <p className="page-subtitle">
            Inspect automated review throughput, rule-based vs AI findings, and factual repository health.
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <AnalyticsFilters
        initialFilters={filters}
        availableRepositories={availableRepositories}
        onFilterChange={handleFilterChange}
        disabled={loading}
      />

      {/* Error state */}
      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorMessage
            message={error}
            onRetry={() => fetchAnalytics(filters)}
          />
        </div>
      )}

      {/* Loading Skeleton */}
      {loading ? (
        <AnalyticsSkeleton />
      ) : isCompletelyEmpty ? (
        /* Empty State */
        <EmptyState
          title="No Analytics Data Recorded"
          message="No reviews or findings match the selected filters. Submit a code review to begin capturing quality analytics and repository health."
          icon={
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 3v18h18" />
              <path d="m19 9-5 5-4-4-3 3" />
            </svg>
          }
          action={
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => handleFilterChange({ from: '', to: '', repository: '' })}
            >
              Clear Filters
            </button>
          }
        />
      ) : (
        /* Loaded Content */
        <div>
          {/* 1. Overview Stat Cards */}
          <OverviewCards overview={overview || {}} />

          {/* 2. Finding Distributions & AI/Rule split */}
          <FindingsDistributionCard findings={findings || {}} />

          {/* 3. Review Activity Trends over time */}
          <ActivityTrendsCard trends={trends || []} />

          {/* 4. Repository Health & Review Summaries Table */}
          <RepositoryHealthTable
            repositories={repositories || []}
            onSelectRepository={handleSelectRepository}
          />
        </div>
      )}
    </div>
  );
};

export default AnalyticsPage;
