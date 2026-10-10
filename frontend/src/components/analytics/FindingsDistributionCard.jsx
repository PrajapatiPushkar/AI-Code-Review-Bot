import React from 'react';

const SEVERITY_CONFIG = {
  CRITICAL: { label: 'Critical', color: 'var(--severity-critical, #ef4444)', bg: 'rgba(239, 68, 68, 0.15)' },
  HIGH: { label: 'High', color: 'var(--severity-high, #f97316)', bg: 'rgba(249, 115, 22, 0.15)' },
  MEDIUM: { label: 'Medium', color: 'var(--severity-medium, #eab308)', bg: 'rgba(234, 179, 8, 0.15)' },
  LOW: { label: 'Low', color: 'var(--severity-low, #3b82f6)', bg: 'rgba(59, 130, 246, 0.15)' },
  INFO: { label: 'Info', color: 'var(--severity-info, #64748b)', bg: 'rgba(100, 116, 139, 0.15)' }
};

const CATEGORY_LABELS = {
  BUG: 'Bug Risk',
  SECURITY: 'Security',
  PERFORMANCE: 'Performance',
  CODE_STYLE: 'Code Style',
  MAINTAINABILITY: 'Maintainability',
  OTHER: 'Other'
};

export const FindingsDistributionCard = ({ findings = {} }) => {
  const {
    totalFindings = 0,
    severityBreakdown = {},
    categoryBreakdown = {},
    sourceBreakdown = {}
  } = findings;

  const aiCount = sourceBreakdown?.AI || 0;
  const ruleCount = sourceBreakdown?.RULE || 0;
  const aiPct = totalFindings > 0 ? Math.round((aiCount / totalFindings) * 100) : 0;
  const rulePct = totalFindings > 0 ? Math.round((ruleCount / totalFindings) * 100) : 0;

  return (
    <div className="card" style={{ padding: '1.5rem', marginBottom: '1.75rem' }}>
      <div className="card-header" style={{ marginBottom: '1.25rem' }}>
        <h2 className="card-title" style={{ fontSize: '1.125rem', fontWeight: 600 }}>
          Finding Intelligence & Distributions
        </h2>
        <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
          {totalFindings} total findings recorded
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.75rem' }}>
        
        {/* 1. Severity Distribution */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span>Severity Breakdown</span>
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {Object.entries(SEVERITY_CONFIG).map(([sevKey, config]) => {
              const count = severityBreakdown?.[sevKey] || 0;
              const pct = totalFindings > 0 ? ((count / totalFindings) * 100).toFixed(1) : 0;
              return (
                <div key={sevKey} style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8125rem' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-primary)', fontWeight: 500 }}>
                      <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: config.color }} />
                      {config.label}
                    </span>
                    <span style={{ color: 'var(--text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                      <strong>{count}</strong> ({pct}%)
                    </span>
                  </div>
                  {/* Progress Meter */}
                  <div style={{ width: '100%', height: '6px', backgroundColor: 'var(--bg-surface-elevated)', borderRadius: '3px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${pct}%`,
                        height: '100%',
                        backgroundColor: config.color,
                        borderRadius: '3px',
                        transition: 'width 300ms ease'
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 2. Category Distribution */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem' }}>
            Category Distribution
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {Object.entries(CATEGORY_LABELS).map(([catKey, label]) => {
              const count = categoryBreakdown?.[catKey] || 0;
              const pct = totalFindings > 0 ? ((count / totalFindings) * 100).toFixed(1) : 0;
              return (
                <div key={catKey} style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8125rem' }}>
                    <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
                      {label}
                    </span>
                    <span style={{ color: 'var(--text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                      <strong>{count}</strong> ({pct}%)
                    </span>
                  </div>
                  <div style={{ width: '100%', height: '6px', backgroundColor: 'var(--bg-surface-elevated)', borderRadius: '3px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${pct}%`,
                        height: '100%',
                        backgroundColor: 'var(--primary-color)',
                        borderRadius: '3px',
                        transition: 'width 300ms ease'
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 3. Hybrid Source Distribution (AI vs RULE) */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem' }}>
            Hybrid Engine Split (AI vs Rule)
          </h3>

          {/* Dual Segmented Bar */}
          <div style={{ marginBottom: '1.25rem' }}>
            <div style={{ width: '100%', height: '10px', backgroundColor: 'var(--bg-surface-elevated)', borderRadius: '5px', overflow: 'hidden', display: 'flex' }}>
              <div
                style={{
                  width: `${aiPct}%`,
                  height: '100%',
                  backgroundColor: '#8b5cf6',
                  title: `AI: ${aiCount}`
                }}
              />
              <div
                style={{
                  width: `${rulePct}%`,
                  height: '100%',
                  backgroundColor: '#06b6d4',
                  title: `Rule: ${ruleCount}`
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.35rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              <span>AI Reasoning ({aiPct}%)</span>
              <span>Deterministic Rules ({rulePct}%)</span>
            </div>
          </div>

          {/* Cards for AI and Rule */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div style={{ padding: '0.875rem', borderRadius: 'var(--radius)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-surface-elevated)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#8b5cf6' }} />
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>AI REASONING</span>
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#a78bfa' }}>
                {aiCount}
              </div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                Gemini LLM semantic reasoning
              </div>
            </div>

            <div style={{ padding: '0.875rem', borderRadius: 'var(--radius)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-surface-elevated)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#06b6d4' }} />
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>STATIC RULES</span>
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#22d3ee' }}>
                {ruleCount}
              </div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                Deterministic pattern engine
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default FindingsDistributionCard;
