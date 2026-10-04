import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';
import FindingCard from '../components/findings/FindingCard';
import FileFindingGroup from '../components/findings/FileFindingGroup';
import FindingFilters from '../components/findings/FindingFilters';
import FindingPagination from '../components/findings/FindingPagination';
import FindingDetailsSkeleton from '../components/findings/FindingDetailsSkeleton';

const ReviewFindingsPage = () => {
  const { id } = useParams();

  const [allFindings, setAllFindings] = useState([]);
  const [review, setReview] = useState(null);
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedSource, setSelectedSource] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState('flat'); // 'flat' | 'grouped'

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch findings and review context
  const fetchFindings = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch findings (preserving existing request contract)
      const [findingsData, reviewData] = await Promise.allSettled([
        reviewService.getReviewFindings(id, { page: 0, size: 200, sort: 'lineNumber,asc' }),
        reviewService.getReviewById(id)
      ]);

      if (findingsData.status === 'fulfilled') {
        setAllFindings(findingsData.value?.content || []);
      } else {
        throw findingsData.reason;
      }

      if (reviewData.status === 'fulfilled') {
        setReview(reviewData.value);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to fetch findings for code review #${id}.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFindings();
  }, [id]);

  // Compute severity statistics, source provenance counts, and category counts
  const { stats, sourceStats, categoriesPresent, categoryCounts } = useMemo(() => {
    const s = {
      total: allFindings.length,
      critical: 0,
      high: 0,
      medium: 0,
      low: 0,
      info: 0
    };

    const src = {
      total: allFindings.length,
      ai: 0,
      rule: 0
    };

    const catCounts = {};

    allFindings.forEach((item) => {
      const sev = (item.severity || 'INFO').toUpperCase();
      if (sev === 'CRITICAL') s.critical++;
      else if (sev === 'HIGH') s.high++;
      else if (sev === 'MEDIUM') s.medium++;
      else if (sev === 'LOW') s.low++;
      else if (sev === 'INFO') s.info++;

      const isRule = (item.source || '').toUpperCase() === 'RULE';
      if (isRule) {
        src.rule++;
      } else {
        src.ai++;
      }

      if (item.category) {
        const cat = item.category.toUpperCase();
        catCounts[cat] = (catCounts[cat] || 0) + 1;
      }
    });

    return {
      stats: s,
      sourceStats: src,
      categoriesPresent: Object.keys(catCounts).sort(),
      categoryCounts: catCounts
    };
  }, [allFindings]);

  // Filter findings locally
  const filteredFindings = useMemo(() => {
    return allFindings.filter((item) => {
      // Source filter
      if (selectedSource !== 'ALL') {
        const isRule = (item.source || '').toUpperCase() === 'RULE';
        if (selectedSource === 'RULE' && !isRule) return false;
        if (selectedSource === 'AI' && isRule) return false;
      }

      // Severity filter
      if (selectedSeverity !== 'ALL') {
        const sev = (item.severity || 'INFO').toUpperCase();
        if (sev !== selectedSeverity) return false;
      }

      // Category filter
      if (selectedCategory !== 'ALL') {
        const cat = (item.category || '').toUpperCase();
        if (cat !== selectedCategory) return false;
      }

      // Search query (matches file path or message)
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const msg = (item.message || '').toLowerCase();
        const file = (item.filePath || '').toLowerCase();
        if (!msg.includes(q) && !file.includes(q)) return false;
      }

      return true;
    });
  }, [allFindings, selectedSource, selectedSeverity, selectedCategory, searchQuery]);

  // Pagination calculation
  const totalPages = Math.ceil(filteredFindings.length / size) || 1;
  const paginatedFindings = useMemo(() => {
    const start = page * size;
    return filteredFindings.slice(start, start + size);
  }, [filteredFindings, page, size]);

  // Group paginated findings by file for grouped view
  const groupedByFile = useMemo(() => {
    const groups = {};
    paginatedFindings.forEach((f) => {
      const path = f.filePath || 'General';
      if (!groups[path]) {
        groups[path] = [];
      }
      groups[path].push(f);
    });
    return groups;
  }, [paginatedFindings]);

  const handleSourceChange = (src) => {
    setSelectedSource(src);
    setPage(0);
  };

  const handleSeverityChange = (sev) => {
    setSelectedSeverity(sev);
    setPage(0);
  };

  const handleCategoryChange = (cat) => {
    setSelectedCategory(cat);
    setPage(0);
  };

  const handleSearchChange = (query) => {
    setSearchQuery(query);
    setPage(0);
  };

  const handleResetFilters = () => {
    setSelectedSeverity('ALL');
    setSelectedCategory('ALL');
    setSelectedSource('ALL');
    setSearchQuery('');
    setPage(0);
  };

  const hasActiveFilters =
    selectedSeverity !== 'ALL' ||
    selectedCategory !== 'ALL' ||
    selectedSource !== 'ALL' ||
    searchQuery.trim() !== '';

  if (loading) {
    return <FindingDetailsSkeleton />;
  }

  const repoName = review ? review.repository || review.repositoryName : null;
  const fullRepo = review ? (review.owner ? `${review.owner}/${repoName}` : repoName) : null;

  return (
    <div className="findings-page">
      {/* Breadcrumb Navigation */}
      <nav aria-label="Breadcrumb" className="review-detail-breadcrumb">
        <Link to="/reviews" className="review-back-link">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          <span>Reviews</span>
        </Link>
        <span className="breadcrumb-separator" aria-hidden="true">/</span>
        <Link to={`/reviews/${id}`} className="review-back-link">
          Review #{id}
        </Link>
        <span className="breadcrumb-separator" aria-hidden="true">/</span>
        <span className="breadcrumb-current">Findings</span>
      </nav>

      {/* Page Header */}
      <div className="page-header findings-page-header">
        <div className="findings-header-info">
          <div className="findings-title-row">
            <h1 className="page-title">Findings</h1>
            <span className="badge badge-info findings-count-badge">
              {stats.total} {stats.total === 1 ? 'finding' : 'findings'}
            </span>
          </div>
          <p className="page-subtitle">
            Review findings detected during automated code analysis.
            {fullRepo && (
              <span className="findings-header-context">
                {' '}• <strong>{fullRepo}</strong> (PR #{review?.pullRequestNumber})
              </span>
            )}
          </p>
        </div>

        <div className="findings-header-actions">
          <Link to={`/reviews/${id}`} className="btn btn-outline btn-sm">
            ← Back to Review #{id}
          </Link>
          <Link to="/reviews/new" className="btn btn-primary btn-sm">
            + New Review
          </Link>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchFindings} />}

      {/* Findings Provenance & Severity Statistics Bar */}
      <div className="metrics-grid findings-metrics-grid" role="region" aria-label="Findings intelligence summary">
        <div className="metric-card finding-metric-card">
          <span className="metric-label">Total Findings</span>
          <span className="metric-value">{stats.total}</span>
        </div>
        <div className="metric-card finding-metric-card">
          <span className="metric-label">AI Findings</span>
          <span className="metric-value" style={{ color: 'var(--primary-color)' }}>
            {sourceStats.ai}
          </span>
        </div>
        <div className="metric-card finding-metric-card">
          <span className="metric-label">Rule Findings</span>
          <span className="metric-value" style={{ color: 'var(--status-in-progress)' }}>
            {sourceStats.rule}
          </span>
        </div>
        <div className="metric-card finding-metric-card">
          <span className="metric-label">Critical</span>
          <span className="metric-value" style={{ color: 'var(--severity-critical)' }}>
            {stats.critical}
          </span>
        </div>
        <div className="metric-card finding-metric-card">
          <span className="metric-label">High / Med</span>
          <span className="metric-value" style={{ color: 'var(--severity-high)' }}>
            {stats.high + stats.medium}
          </span>
        </div>
        <div className="metric-card finding-metric-card">
          <span className="metric-label">Low / Info</span>
          <span className="metric-value" style={{ color: 'var(--severity-low)' }}>
            {stats.low + stats.info}
          </span>
        </div>
      </div>

      {/* Source, Severity, Category & Search Filters Bar */}
      <FindingFilters
        selectedSource={selectedSource}
        onSourceChange={handleSourceChange}
        sourceStats={sourceStats}
        selectedSeverity={selectedSeverity}
        onSeverityChange={handleSeverityChange}
        selectedCategory={selectedCategory}
        onCategoryChange={handleCategoryChange}
        searchQuery={searchQuery}
        onSearchChange={handleSearchChange}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onReset={handleResetFilters}
        stats={stats}
        categoriesPresent={categoriesPresent}
        categoryCounts={categoryCounts}
        hasActiveFilters={hasActiveFilters}
      />

      {/* Findings Results Area */}
      {filteredFindings.length === 0 ? (
        <EmptyState
          title={hasActiveFilters ? "No matching findings" : "No findings detected"}
          message={
            hasActiveFilters
              ? "No code findings match your selected source, severity, category, or search filters."
              : "This automated pull request review produced zero findings or suggestions."
          }
          icon={
            <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          }
          action={
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
              {hasActiveFilters ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleResetFilters}
                >
                  Clear Filters
                </button>
              ) : (
                <Link to={`/reviews/${id}`} className="btn btn-primary btn-sm">
                  ← Back to Review Details
                </Link>
              )}
            </div>
          }
        />
      ) : (
        <div className="findings-list-wrapper">
          {/* Grouped by File View */}
          {viewMode === 'grouped' ? (
            <div className="findings-grouped-container">
              {Object.entries(groupedByFile).map(([filePath, fileFindings]) => (
                <FileFindingGroup
                  key={filePath}
                  filePath={filePath}
                  findings={fileFindings}
                />
              ))}
            </div>
          ) : (
            /* Flat List View */
            <div className="findings-flat-container">
              {paginatedFindings.map((finding) => (
                <FindingCard key={finding.id} finding={finding} />
              ))}
            </div>
          )}

          {/* Client-side Pagination */}
          <FindingPagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={filteredFindings.length}
            onPageChange={setPage}
            onSizeChange={(newSize) => {
              setSize(newSize);
              setPage(0);
            }}
          />
        </div>
      )}
    </div>
  );
};

export default ReviewFindingsPage;
