import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import ErrorMessage from '../components/ErrorMessage';
import StatCard from '../components/dashboard/StatCard';
import QuickActions from '../components/dashboard/QuickActions';
import RecentReviews from '../components/dashboard/RecentReviews';

const DashboardPage = () => {
  const [reviews, setReviews] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [completedCount, setCompletedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [failedCount, setFailedCount] = useState(0);
  const [totalFindingsSum, setTotalFindingsSum] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch recent 5 reviews
      const pageData = await reviewService.getCodeReviews({ page: 0, size: 5, sort: 'createdAt,desc' });
      const recentList = pageData.content || [];
      setReviews(recentList);
      setTotalElements(pageData.totalElements || 0);

      // Calculate total findings across recent reviews
      const findingsSum = recentList.reduce((acc, curr) => acc + (curr.totalFindings || 0), 0);
      setTotalFindingsSum(findingsSum);

      // Fetch counts for status metrics
      const [completedData, inProgressData, failedData] = await Promise.allSettled([
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'COMPLETED' }),
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'IN_PROGRESS' }),
        reviewService.getCodeReviews({ page: 0, size: 1, status: 'FAILED' })
      ]);

      if (completedData.status === 'fulfilled') setCompletedCount(completedData.value.totalElements || 0);
      if (inProgressData.status === 'fulfilled') setInProgressCount(inProgressData.value.totalElements || 0);
      if (failedData.status === 'fulfilled') setFailedCount(failedData.value.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const formatDuration = (createdStr, completedStr) => {
    if (!createdStr || !completedStr) return 'N/A';
    const created = new Date(createdStr);
    const completed = new Date(completedStr);
    const diffMs = completed - created;
    if (diffMs <= 0) return '< 1s';
    const seconds = Math.floor(diffMs / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ${seconds % 60}s`;
  };

  return (
    <div>
      {/* Dashboard Page Header */}
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Monitor automated AI code reviews, pull request throughput, and quality findings.</p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <Link to="/analytics" className="btn btn-outline btn-sm">
            Analytics & Health →
          </Link>
          <Link to="/reviews" className="btn btn-outline btn-sm">
            View All History →
          </Link>
          <Link to="/reviews/new" className="btn btn-primary btn-sm">
            + Submit New Review
          </Link>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchDashboardData} />}

      {/* Loading Skeleton State */}
      {loading ? (
        <div>
          {/* Skeleton Metrics Grid */}
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginBottom: '2rem' }}>
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="stat-card" aria-hidden="true">
                <div className="stat-card-header">
                  <LoadingSkeleton variant="text" width="55%" height="0.8125rem" />
                  <LoadingSkeleton variant="circular" width="32px" height="32px" />
                </div>
                <div className="stat-card-body">
                  <LoadingSkeleton variant="text" width="40%" height="2rem" style={{ marginBottom: '0.35rem' }} />
                  <LoadingSkeleton variant="text" width="70%" height="0.75rem" style={{ marginBottom: 0 }} />
                </div>
              </div>
            ))}
          </div>

          {/* Skeleton Layout Split */}
          <div className="dashboard-layout-grid">
            <LoadingSkeleton.Table rows={5} columns={9} />
            <LoadingSkeleton.Card height="160px" />
          </div>
        </div>
      ) : (
        /* Loaded Content State */
        <div>
          {/* Metrics Grid */}
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginBottom: '2rem' }}>
            {/* StatCard 1: Total Reviews */}
            <StatCard
              title="Total Reviews"
              value={totalElements}
              description="All submitted reviews"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                  <polyline points="10 9 9 9 8 9" />
                </svg>
              }
              iconBg="var(--primary-light)"
              iconColor="var(--primary-color)"
            />

            {/* StatCard 2: Completed Reviews */}
            <StatCard
              title="Completed Reviews"
              value={completedCount}
              description="Successfully analyzed reviews"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              }
              iconBg="var(--status-completed-bg)"
              iconColor="var(--status-completed)"
              valueColor="var(--status-completed)"
            />

            {/* StatCard 3: In Progress */}
            <StatCard
              title="In Progress"
              value={inProgressCount}
              description="Active background reviews"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
              }
              iconBg="var(--status-in-progress-bg)"
              iconColor="var(--status-in-progress)"
              valueColor="var(--status-in-progress)"
            />

            {/* StatCard 4: Failed Reviews */}
            <StatCard
              title="Failed Reviews"
              value={failedCount}
              description="Review execution errors"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
              }
              iconBg="var(--status-failed-bg)"
              iconColor="var(--status-failed)"
              valueColor="var(--status-failed)"
            />

            {/* StatCard 5: Recent Findings */}
            <StatCard
              title="Recent Findings"
              value={totalFindingsSum}
              description="Across latest reviews"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
              }
              iconBg="rgba(14, 165, 233, 0.15)"
              iconColor="var(--text-code)"
              valueColor="var(--text-code)"
            />
          </div>

          {/* Main Dashboard Layout Split: Recent Reviews (75%) + Quick Actions (25%) */}
          <div className="dashboard-layout-grid">
            <RecentReviews reviews={reviews} formatDuration={formatDuration} />
            <QuickActions />
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
