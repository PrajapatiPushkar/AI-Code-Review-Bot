import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import reviewService from '../services/reviewService';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

const ReviewFindingsPage = () => {
  const { id } = useParams();
  const [findings, setFindings] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchFindings = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await reviewService.getReviewFindings(id, { page, size, sort: 'lineNumber,asc' });
      setFindings(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || `Failed to fetch findings for review #${id}.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFindings();
  }, [id, page, size]);

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Findings for Review #{id}</h1>
          <p className="page-subtitle">Detailed AI review findings and code recommendations.</p>
        </div>
        <Link to={`/reviews/${id}`} className="btn btn-outline btn-sm">
          ← Back to Review Details
        </Link>
      </div>

      {error && <ErrorMessage message={error} onRetry={fetchFindings} />}

      {loading ? (
        <Loading message="Loading findings..." />
      ) : findings.length === 0 ? (
        <EmptyState title="No Findings Detected" message="This code review produced zero findings or suggestions." />
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Severity</th>
                  <th>Category</th>
                  <th>File Path</th>
                  <th>Line #</th>
                  <th>Title / Suggestion</th>
                </tr>
              </thead>
              <tbody>
                {findings.map((finding, idx) => (
                  <tr key={finding.id || idx}>
                    <td>
                      <span className={`badge badge-${(finding.severity || 'info').toLowerCase()}`}>
                        {finding.severity}
                      </span>
                    </td>
                    <td>{finding.category || 'GENERAL'}</td>
                    <td><code>{finding.filePath}</code></td>
                    <td>Line {finding.lineNumber}</td>
                    <td>
                      <strong>{finding.title}</strong>
                      <p style={{ marginTop: '0.25rem', color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                        {finding.description}
                      </p>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <span className="pagination-info">
              Page {page + 1} of {totalPages || 1} ({totalElements} total findings)
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
