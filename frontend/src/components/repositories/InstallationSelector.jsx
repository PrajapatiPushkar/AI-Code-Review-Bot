import React from 'react';

export const InstallationSelector = ({
  installations = [],
  selectedInstallationId,
  onSelectInstallation,
  loading = false,
  disabled = false
}) => {
  if (loading) {
    return (
      <div className="installation-selector-container">
        <div className="installation-selector-loading">
          <span className="installation-spinner" aria-hidden="true" />
          <span>Loading GitHub installations...</span>
        </div>
      </div>
    );
  }

  if (!installations || installations.length === 0) {
    return null;
  }

  const selectedInstallation = installations.find(
    (inst) =>
      String(inst.githubInstallationId) === String(selectedInstallationId) ||
      String(inst.id) === String(selectedInstallationId)
  ) || installations[0];

  const isSingle = installations.length === 1;

  const handleChange = (e) => {
    const value = e.target.value;
    const targetInst = installations.find(
      (inst) =>
        String(inst.githubInstallationId) === String(value) ||
        String(inst.id) === String(value)
    );
    if (targetInst && onSelectInstallation) {
      onSelectInstallation(targetInst);
    }
  };

  return (
    <div className="installation-selector-wrapper">
      <div className="installation-selector-header">
        <label htmlFor="installation-select" className="installation-selector-label">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            className="installation-icon"
          >
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
          </svg>
          <span>GitHub Installation</span>
        </label>
        {selectedInstallation && (
          <span className="installation-status-tag">
            {selectedInstallation.verified ? 'Verified' : 'Connected'}
          </span>
        )}
      </div>

      {isSingle ? (
        <div className="installation-single-card" role="region" aria-label="Active GitHub Installation">
          <div className="installation-account-info">
            <span className="installation-account-avatar" aria-hidden="true">
              {selectedInstallation.githubAccountLogin?.charAt(0)?.toUpperCase() || 'G'}
            </span>
            <div className="installation-account-text">
              <span className="installation-account-name">
                {selectedInstallation.githubAccountLogin || 'Default Account'}
              </span>
              <span className="installation-account-type">
                {selectedInstallation.githubAccountType || 'User'} • ID #{selectedInstallation.githubInstallationId || selectedInstallation.id}
              </span>
            </div>
          </div>
          <span className="installation-single-badge">Active</span>
        </div>
      ) : (
        <div className="installation-select-control">
          <select
            id="installation-select"
            className="form-input installation-select"
            value={selectedInstallation?.githubInstallationId || selectedInstallation?.id || ''}
            onChange={handleChange}
            disabled={disabled}
            aria-label="Select connected GitHub installation"
          >
            {installations.map((inst) => {
              const instId = inst.githubInstallationId || inst.id;
              const account = inst.githubAccountLogin || 'Account';
              const type = inst.githubAccountType || 'User';
              return (
                <option key={inst.id || instId} value={instId}>
                  {account} ({type}) — ID #{instId}
                </option>
              );
            })}
          </select>
        </div>
      )}
    </div>
  );
};

export default InstallationSelector;
