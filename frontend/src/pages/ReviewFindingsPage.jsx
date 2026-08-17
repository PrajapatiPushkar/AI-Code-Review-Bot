import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import FindingCard from '../components/FindingCard';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

const ReviewFindingsPage = () => {
  const { id } = useParams();

  const [allFindings, setAllFindings] = useState([]);
  const [displayedFindings, setDisplayedFindings] = useState([]);
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Statistics counters
  const [stats, setStats] = useState({
    total: 0,
    critical: 0,
    high: 0,
    medium: 0,
    low: 0,
    info: 0
  });

  const fetchFindings = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch paginated findings from backend
      const data = await reviewService.getReviewFindings(id, { page: 0, size: 200, sort: 'lineNumber,asc' });
      const items = data.content || [];

      setAllFindings(items);
      setTotalElements(data.totalElements || items.length);

      // Compute statistics
      const counts = {
        total: items.length,
        critical: 0,
        high: 0,
        medium: 0,
        low: 0,
        info: 0
      };

      items.forEach((item) => {
        const sev = (item.severity || 'INFO').toUpperCase();
        if (sev === 'CRITICAL') counts.critical++;
        else if (sev === 'HIGH') counts.high++;
        else if (sev === 'MEDIUM') counts.medium++;
        else if (sev === 'LOW') counts.low++;
        else if (sev === 'INFO') counts.info++;
      });

      setStats(counts);
    } catch (err) {
      setError(err.response?.data?.message || `Failed to fetch findings for code review #${id}.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFindings();
  }, [id]);

  // Apply severity filter and pagination locally
  useEffect(() => {
    let filtered = allFindings;
    if (selectedSeverity !== 'ALL') {
      filtered = allFindings.filter(
        (f) => (f.severity || '').toUpperCase() === selectedSeverity
      );
    }

    const calculatedTotalPages = Math.ceil(filtered.length / size) || 1;
    setTotalPages(calculatedTotalPages);

    const startIndex = page * size;
    const paginated = filtered.slice(startIndex, startIndex + size);
    setDisplayedFindings(paginated);
  }, [allFindings, selectedSeverity, page, size]);

  const handleSeverityChange = (sev) => {
    setSelectedSeverity(sev);
    setPage(0);
  };

  if (loading) return <Loading message={`Loading code review findings for #${id}...`} />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Findings for Review #{id}</h1>
          <p className="page-subtitle">AI-generated code quality, security, and performance recommendations.</p>
        </div>
        <Link to={`/reviews/${id}`} className="btn btn-outline btn-sm">
          ← Back to Review Details
        </Link>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchFindings} />}

      {/* Findings Statistics Bar */}
      <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', marginBottom: '1.5rem' }}>
        <div className="metric-card">
          <span className="metric-label">Total Findings</span>
          <span className="metric-value">{stats.total}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Critical</span>
          <span className="metric-value" style={{ color: 'var(--severity-critical)' }}>
            {stats.critical}
          </span>
        </div>
        <div className="metric-card">
          <span className="metric-label">High</span>
          <span className="metric-value" style={{ color: 'var(--severity-high)' }}>
            {stats.high}
          </span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Medium</span>
          <span className="metric-value" style={{ color: 'var(--severity-medium)' }}>
            {stats.medium}
          </span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Low / Info</span>
          <span className="metric-value" style={{ color: 'var(--severity-low)' }}>
            {stats.low + stats.info}
          </span>
        </div>
      </div>

      {/* Severity Filter Tabs */}
      <div
        className="filters-bar"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          flexWrap: 'wrap',
          marginBottom: '1.5rem'
        }}
      >
        <span style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--text-secondary)', marginRight: '0.5rem' }}>
          Filter Severity:
        </span>
        {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'].map((sev) => {
          const isActive = selectedSeverity === sev;
          return (
            <button
              key={sev}
              className={`btn btn-sm ${isActive ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => handleSeverityChange(sev)}
              style={{
                borderRadius: '9999px',
                padding: '0.25rem 0.875rem',
                fontSize: '0.8125rem'
              }}
            >
              {sev}
              {sev === 'ALL' && ` (${stats.total})`}
              {sev === 'CRITICAL' && ` (${stats.critical})`}
              {sev === 'HIGH' && ` (${stats.high})`}
              {sev === 'MEDIUM' && ` (${stats.medium})`}
              {sev === 'LOW' && ` (${stats.low})`}
              {sev === 'INFO' && ` (${stats.info})`}
            </button>
          );
        })}
      </div>

      {/* Findings Card List */}
      {displayedFindings.length === 0 ? (
        <EmptyState
          title="No Findings Match Filter"
          message={
            selectedSeverity === 'ALL'
              ? 'This code review produced zero findings or suggestions.'
              : `No findings found with severity "${selectedSeverity}".`
          }
        />
      ) : (
        <div>
          {displayedFindings.map((finding) => (
            <FindingCard key={finding.id} finding={finding} />
          ))}

          {/* Pagination Controls */}
          <div className="pagination" style={{ marginTop: '1.5rem' }}>
            <span className="pagination-info">
              Showing page {page + 1} of {totalPages} ({displayedFindings.length} findings displayed)
            </span>
            <div className="pagination-controls">
              <button
                className="btn btn-outline btn-sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <button
                className="btn btn-outline btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReviewFindingsPage;
