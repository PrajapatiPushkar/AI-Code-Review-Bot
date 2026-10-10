import React, { useState, useEffect, useCallback, useMemo } from 'react';
import policyService from '../../services/policyService';
import useToast from '../../hooks/useToast';

const SEVERITY_OPTIONS = [
  { value: 'CRITICAL', label: 'CRITICAL', desc: 'Fails only on critical security vulnerabilities or severe system crashes.' },
  { value: 'HIGH', label: 'HIGH (Default)', desc: 'Fails on high and critical issues (e.g. security risks, major bugs).' },
  { value: 'MEDIUM', label: 'MEDIUM', desc: 'Fails on medium, high, and critical issues (e.g. performance bottlenecks).' },
  { value: 'LOW', label: 'LOW', desc: 'Fails on low, medium, high, and critical issues (e.g. anti-patterns, console logs).' },
  { value: 'INFO', label: 'INFO', desc: 'Fails on any finding including informational notes.' }
];

export const RepositoryPolicyModal = ({ repository, onClose }) => {
  const toast = useToast();

  const repoId = repository?.id;
  const repoName = repository?.name || 'Repository';
  const fullName = repository?.full_name || repository?.fullName || repoName;

  // State
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [error, setError] = useState(null);

  // Form State
  const [enabled, setEnabled] = useState(true);
  const [failOnSeverity, setFailOnSeverity] = useState('HIGH');
  const [enabledRuleIds, setEnabledRuleIds] = useState(new Set());
  const [availableRules, setAvailableRules] = useState([]);
  const [isCustom, setIsCustom] = useState(false);

  // Original snapshot to detect unsaved changes
  const [originalSnapshot, setOriginalSnapshot] = useState(null);

  const fetchPolicy = useCallback(async () => {
    if (!repoId) return;
    try {
      setLoading(true);
      setError(null);
      const data = await policyService.getReviewPolicy(repoId);
      
      const isEnabledVal = data.enabled ?? true;
      const severityVal = data.failOnSeverity || 'HIGH';
      const rulesSet = new Set(data.enabledRuleIds || []);
      
      setEnabled(isEnabledVal);
      setFailOnSeverity(severityVal);
      setEnabledRuleIds(rulesSet);
      setAvailableRules(data.availableRules || []);
      setIsCustom(Boolean(data.isCustom));

      setOriginalSnapshot({
        enabled: isEnabledVal,
        failOnSeverity: severityVal,
        enabledRuleIds: Array.from(rulesSet).sort().join(',')
      });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to load repository review policy.';
      setError(msg);
      toast?.error?.(msg);
    } finally {
      setLoading(false);
    }
  }, [repoId, toast]);

  useEffect(() => {
    fetchPolicy();
  }, [fetchPolicy]);

  // Compute unsaved changes
  const hasUnsavedChanges = useMemo(() => {
    if (!originalSnapshot) return false;
    const currentRules = Array.from(enabledRuleIds).sort().join(',');
    return (
      enabled !== originalSnapshot.enabled ||
      failOnSeverity !== originalSnapshot.failOnSeverity ||
      currentRules !== originalSnapshot.enabledRuleIds
    );
  }, [enabled, failOnSeverity, enabledRuleIds, originalSnapshot]);

  // ESC key listener with unsaved changes check
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') {
        if (hasUnsavedChanges) {
          if (window.confirm('You have unsaved policy changes. Discard them?')) {
            onClose();
          }
        } else {
          onClose();
        }
      }
    },
    [hasUnsavedChanges, onClose]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleClose = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved policy changes. Discard them?')) {
        onClose();
      }
    } else {
      onClose();
    }
  };

  const handleToggleRule = (ruleId) => {
    setEnabledRuleIds((prev) => {
      const next = new Set(prev);
      if (next.has(ruleId)) {
        next.delete(ruleId);
      } else {
        next.add(ruleId);
      }
      return next;
    });
  };

  const handleSelectAllRules = () => {
    const all = new Set(availableRules.map((r) => r.ruleId));
    setEnabledRuleIds(all);
  };

  const handleDeselectAllRules = () => {
    setEnabledRuleIds(new Set());
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!repoId) return;

    try {
      setSaving(true);
      setError(null);
      const payload = {
        enabled,
        failOnSeverity,
        enabledRuleIds: Array.from(enabledRuleIds)
      };

      const data = await policyService.updateReviewPolicy(repoId, payload);
      setIsCustom(Boolean(data.isCustom));
      
      const isEnabledVal = data.enabled ?? true;
      const severityVal = data.failOnSeverity || 'HIGH';
      const rulesSet = new Set(data.enabledRuleIds || []);

      setOriginalSnapshot({
        enabled: isEnabledVal,
        failOnSeverity: severityVal,
        enabledRuleIds: Array.from(rulesSet).sort().join(',')
      });

      toast?.success?.(`Review policy for "${fullName}" saved successfully.`);
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update review policy.';
      setError(msg);
      toast?.error?.(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (!repoId) return;
    if (!window.confirm(`Reset review policy for "${fullName}" to system defaults?`)) {
      return;
    }

    try {
      setResetting(true);
      setError(null);
      const data = await policyService.resetReviewPolicy(repoId);

      const isEnabledVal = data.enabled ?? true;
      const severityVal = data.failOnSeverity || 'HIGH';
      const rulesSet = new Set(data.enabledRuleIds || []);

      setEnabled(isEnabledVal);
      setFailOnSeverity(severityVal);
      setEnabledRuleIds(rulesSet);
      setIsCustom(false);

      setOriginalSnapshot({
        enabled: isEnabledVal,
        failOnSeverity: severityVal,
        enabledRuleIds: Array.from(rulesSet).sort().join(',')
      });

      toast?.success?.(`Review policy reset to system defaults.`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to reset review policy.';
      setError(msg);
      toast?.error?.(msg);
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleClose} role="presentation">
      <div
        className="modal-container policy-modal-container"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="policy-modal-title"
      >
        {/* Modal Header */}
        <div className="modal-header policy-modal-header">
          <div className="policy-modal-title-group">
            <div className="policy-shield-badge" aria-hidden="true">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </div>
            <div>
              <div className="policy-modal-header-top">
                <h2 id="policy-modal-title" className="modal-title">
                  Review Policy & Quality Gate
                </h2>
                <span className={`badge ${isCustom ? 'badge-primary' : 'badge-secondary'} policy-custom-badge`}>
                  {isCustom ? 'Custom Policy' : 'System Default'}
                </span>
              </div>
              <p className="policy-modal-repo-name" title={fullName}>
                Repository: <strong>{fullName}</strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={handleClose}
            aria-label="Close review policy modal"
          >
            ✕
          </button>
        </div>

        {/* Modal Content */}
        <div className="modal-body policy-modal-body">
          {error && (
            <div className="policy-alert policy-alert-error" role="alert">
              <span aria-hidden="true">⚠️</span>
              <span>{error}</span>
            </div>
          )}

          {loading ? (
            <div className="policy-modal-loading" aria-busy="true" aria-live="polite">
              <div className="installation-spinner" aria-hidden="true" />
              <span>Loading review policy settings...</span>
            </div>
          ) : (
            <form id="review-policy-form" onSubmit={handleSave}>
              {/* Section 1: Quality Gate Evaluation Toggle */}
              <div className="policy-section card">
                <div className="policy-toggle-row">
                  <div>
                    <label htmlFor="gate-enabled-toggle" className="policy-section-title">
                      Quality Gate Evaluation
                    </label>
                    <p className="policy-section-desc">
                      When enabled, completed code reviews evaluate detected findings against the failure threshold.
                    </p>
                  </div>
                  <label className="switch-toggle" htmlFor="gate-enabled-toggle">
                    <input
                      id="gate-enabled-toggle"
                      type="checkbox"
                      checked={enabled}
                      onChange={(e) => setEnabled(e.target.checked)}
                      aria-checked={enabled}
                    />
                    <span className="switch-slider" aria-hidden="true" />
                  </label>
                </div>
              </div>

              {/* Section 2: Failure Severity Threshold */}
              <div className="policy-section card">
                <label htmlFor="severity-threshold-select" className="policy-section-title">
                  Failure Severity Threshold
                </label>
                <p className="policy-section-desc">
                  Completed reviews fail the quality gate if any findings match or exceed this severity level.
                </p>

                <div className="policy-select-wrapper">
                  <select
                    id="severity-threshold-select"
                    value={failOnSeverity}
                    onChange={(e) => setFailOnSeverity(e.target.value)}
                    className="form-control policy-select"
                    disabled={!enabled}
                  >
                    {SEVERITY_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label} — {opt.desc}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="policy-threshold-summary">
                  <span className="policy-threshold-pill" title={`Active threshold: ${failOnSeverity}`}>
                    Configured Gate: <strong>{failOnSeverity}</strong>
                  </span>
                  <span className="policy-threshold-rule-text">
                    Violations: Findings with severity &ge; {failOnSeverity} trigger gate failure.
                  </span>
                </div>
              </div>

              {/* Section 3: Deterministic Rule Engine */}
              <div className="policy-section card">
                <div className="policy-rules-header">
                  <div>
                    <h3 className="policy-section-title">Deterministic Code-Quality Rules</h3>
                    <p className="policy-section-desc">
                      Configure deterministic rules evaluated during pull request reviews. Disabling a rule turns off its detection.
                    </p>
                  </div>
                  <div className="policy-rules-actions">
                    <button
                      type="button"
                      className="btn btn-outline btn-xs"
                      onClick={handleSelectAllRules}
                    >
                      Enable All
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline btn-xs"
                      onClick={handleDeselectAllRules}
                    >
                      Disable All
                    </button>
                  </div>
                </div>

                {availableRules.length === 0 ? (
                  <p className="text-muted" style={{ padding: '0.75rem 0' }}>
                    No deterministic rules currently registered in the review engine.
                  </p>
                ) : (
                  <div className="policy-rules-list" role="group" aria-label="Supported deterministic code quality rules">
                    {availableRules.map((rule) => {
                      const isRuleEnabled = enabledRuleIds.has(rule.ruleId);
                      return (
                        <div
                          key={rule.ruleId}
                          className={`policy-rule-item ${isRuleEnabled ? 'policy-rule-active' : 'policy-rule-inactive'}`}
                          onClick={() => handleToggleRule(rule.ruleId)}
                          role="checkbox"
                          aria-checked={isRuleEnabled}
                          tabIndex={0}
                          onKeyDown={(e) => {
                            if (e.key === ' ' || e.key === 'Enter') {
                              e.preventDefault();
                              handleToggleRule(rule.ruleId);
                            }
                          }}
                        >
                          <div className="policy-rule-checkbox-wrapper">
                            <input
                              type="checkbox"
                              checked={isRuleEnabled}
                              onChange={() => handleToggleRule(rule.ruleId)}
                              aria-labelledby={`rule-name-${rule.ruleId}`}
                              tabIndex={-1}
                            />
                          </div>
                          <div className="policy-rule-info">
                            <div className="policy-rule-title-row">
                              <span id={`rule-name-${rule.ruleId}`} className="policy-rule-name">
                                {rule.name}
                              </span>
                              <span className="badge badge-secondary policy-rule-id-pill">
                                {rule.ruleId}
                              </span>
                              {rule.category && (
                                <span className="badge badge-outline policy-rule-category-pill">
                                  {rule.category}
                                </span>
                              )}
                              {rule.defaultSeverity && (
                                <span className="badge badge-outline policy-rule-severity-pill">
                                  Default: {rule.defaultSeverity}
                                </span>
                              )}
                            </div>
                            <p className="policy-rule-desc">{rule.description}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
                {enabledRuleIds.size === 0 && (
                  <p className="policy-rule-empty-note">
                    ⚠️ All deterministic rules are disabled. Only AI-generated findings will be evaluated for this repository.
                  </p>
                )}
              </div>
            </form>
          )}
        </div>

        {/* Modal Footer */}
        <div className="modal-footer policy-modal-footer">
          <div className="policy-modal-footer-left">
            <button
              type="button"
              className="btn btn-outline policy-reset-btn"
              onClick={handleReset}
              disabled={loading || saving || resetting}
              title="Reset policy to system defaults"
            >
              {resetting ? 'Resetting...' : 'Reset to Defaults'}
            </button>
            {hasUnsavedChanges && (
              <span className="policy-unsaved-indicator" aria-live="polite">
                ● Unsaved changes
              </span>
            )}
          </div>

          <div className="policy-modal-footer-right">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleClose}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              form="review-policy-form"
              className="btn btn-primary"
              disabled={loading || saving || resetting || !hasUnsavedChanges}
            >
              {saving ? 'Saving...' : 'Save Policy'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RepositoryPolicyModal;
