import React from 'react';
import StatCard from '../dashboard/StatCard';

export const OverviewCards = ({ overview = {} }) => {
  const {
    totalReviews = 0,
    completedReviews = 0,
    failedReviews = 0,
    inProgressReviews = 0,
    totalFindings = 0
  } = overview;

  const completionRate = totalReviews > 0
    ? Math.round((completedReviews / totalReviews) * 100)
    : 0;

  const failureRate = totalReviews > 0
    ? Math.round((failedReviews / totalReviews) * 100)
    : 0;

  return (
    <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginBottom: '1.75rem' }}>
      {/* 1. Total Reviews */}
      <StatCard
        title="Total Reviews"
        value={totalReviews}
        description="Across filtered range"
        icon={
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
          </svg>
        }
        iconBg="var(--primary-light)"
        iconColor="var(--primary-color)"
      />

      {/* 2. Completed Reviews */}
      <StatCard
        title="Completed Reviews"
        value={completedReviews}
        description={`${completionRate}% completion rate`}
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

      {/* 3. In Progress Reviews */}
      <StatCard
        title="In Progress"
        value={inProgressReviews}
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

      {/* 4. Failed Reviews */}
      <StatCard
        title="Failed Reviews"
        value={failedReviews}
        description={`${failureRate}% failure rate`}
        icon={
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        }
        iconBg="var(--status-failed-bg)"
        iconColor="var(--status-failed)"
        valueColor={failedReviews > 0 ? "var(--status-failed)" : undefined}
      />

      {/* 5. Total Findings */}
      <StatCard
        title="Total Findings"
        value={totalFindings}
        description="Identified issues"
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
  );
};

export default OverviewCards;
