import React from 'react';
import SeverityBreakdown from './SeverityBreakdown';
import CategoryBreakdown from './CategoryBreakdown';

export const ReviewIntelligenceSummary = ({ findings = [], isInProgress = false }) => {
  const totalFindings = findings.length;

  let aiCount = 0;
  let ruleCount = 0;
  const ruleCountsById = {};

  findings.forEach((f) => {
    const isRule = (f.source || '').toUpperCase() === 'RULE';
    if (isRule) {
      ruleCount++;
      const rid = f.ruleId || 'RULE-STATIC-CHECK';
      ruleCountsById[rid] = (ruleCountsById[rid] || 0) + 1;
    } else {
      aiCount++;
    }
  });

  const aiPercentage = totalFindings > 0 ? Math.round((aiCount / totalFindings) * 100) : 0;
  const rulePercentage = totalFindings > 0 ? Math.round((ruleCount / totalFindings) * 100) : 0;
  const triggeredRules = Object.entries(ruleCountsById);

  return (
    <div className="card review-intelligence-card" role="region" aria-label="Review Intelligence Summary">
      <div className="card-header review-intelligence-header">
        <div className="review-intelligence-title-wrap">
          <div className="review-intelligence-icon" aria-hidden="true">
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
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </div>
          <div>
            <h2 className="card-title">Review Intelligence</h2>
            <p className="card-subtitle">
              Hybrid code quality architecture: contextual Gemini AI reasoning &amp; deterministic rules.
            </p>
          </div>
        </div>

        <div className="review-intelligence-badge-wrap">
          <span className="badge badge-primary review-hybrid-badge">Hybrid Engine</span>
        </div>
      </div>

      <div className="review-intelligence-body">
        {isInProgress ? (
          <div className="review-intelligence-in-progress">
            <div className="review-summary-loading-spinner" aria-hidden="true" />
            <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.875rem' }}>
              Calculating hybrid review findings and distributions...
            </p>
          </div>
        ) : (
          <>
            {/* Top 3-Stat Provenance Metric Cards */}
            <div className="review-provenance-grid" role="region" aria-label="Finding provenance breakdown">
              {/* Card 1: Total Findings */}
              <div className="review-provenance-card">
                <span className="provenance-card-label">Total Findings</span>
                <div className="provenance-card-value">{totalFindings}</div>
                <span className="provenance-card-desc">Combined hybrid issues</span>
              </div>

              {/* Card 2: AI Findings */}
              <div className="review-provenance-card provenance-card-ai">
                <div className="provenance-card-header">
                  <span className="provenance-card-label">AI Findings</span>
                  <span className="badge badge-info provenance-pill">Gemini</span>
                </div>
                <div className="provenance-card-value">{aiCount}</div>
                <span className="provenance-card-desc">
                  {totalFindings > 0 ? `${aiPercentage}% of total findings` : 'Contextual reasoning'}
                </span>
              </div>

              {/* Card 3: Rule Findings */}
              <div className="review-provenance-card provenance-card-rule">
                <div className="provenance-card-header">
                  <span className="provenance-card-label">Rule Findings</span>
                  <span className="badge badge-primary provenance-pill">Deterministic</span>
                </div>
                <div className="provenance-card-value">{ruleCount}</div>
                <span className="provenance-card-desc">
                  {totalFindings > 0 ? `${rulePercentage}% of total findings` : 'Pattern rules'}
                </span>
              </div>
            </div>

            {/* Triggered Deterministic Rules List (if any) */}
            {triggeredRules.length > 0 && (
              <div className="review-triggered-rules-section">
                <span className="triggered-rules-label">Triggered Deterministic Rules:</span>
                <div className="triggered-rules-chips">
                  {triggeredRules.map(([ruleId, count]) => (
                    <span key={ruleId} className="triggered-rule-chip">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <polyline points="16 18 22 12 16 6" />
                        <polyline points="8 6 2 12 8 18" />
                      </svg>
                      <code>{ruleId}</code>
                      <span className="triggered-rule-count">({count})</span>
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Side-by-side Severity and Category Distributions */}
            <div className="review-intelligence-distributions-grid">
              <SeverityBreakdown findings={findings} />
              <CategoryBreakdown findings={findings} />
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ReviewIntelligenceSummary;
