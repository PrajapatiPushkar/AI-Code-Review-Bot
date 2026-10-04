import React, { useState } from 'react';

export const ReviewTargetForm = ({
  installations = [],
  loadingInstallations = false,
  selectedInstallationId = '',
  onSelectInstallation,
  repositories = [],
  loadingRepositories = false,
  selectedRepository = '',
  onSelectRepository,
  owner = '',
  onChangeOwner,
  repositoryName = '',
  onChangeRepositoryName,
  manualInstallationId = '',
  onChangeManualInstallationId,
  isManualMode = false,
  onToggleManualMode,
  errors = {},
  disabled = false
}) => {
  return (
    <div className="submit-form-card" aria-labelledby="target-selection-title">
      <div className="submit-card-section-title" id="target-selection-title">
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z" />
          <circle cx="12" cy="13" r="2" />
        </svg>
        <span>Repository Target Configuration</span>
      </div>
      <p className="submit-card-section-subtitle">
        Select from your authorized GitHub App installations or enter manual repository credentials.
      </p>

      {/* Mode Switch Tabs */}
      <div className="form-mode-tabs" role="tablist" aria-label="Target selection mode">
        <button
          type="button"
          role="tab"
          aria-selected={!isManualMode}
          className={`form-mode-tab ${!isManualMode ? 'active' : ''}`}
          onClick={() => onToggleManualMode(false)}
          disabled={disabled || loadingInstallations}
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
          </svg>
          <span>Connected GitHub App</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={isManualMode}
          className={`form-mode-tab ${isManualMode ? 'active' : ''}`}
          onClick={() => onToggleManualMode(true)}
          disabled={disabled}
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
          </svg>
          <span>Manual Entry</span>
        </button>
      </div>

      {!isManualMode ? (
        /* Connected GitHub App Dropdown Mode */
        <div className="connected-mode-fields">
          {/* Installation Selector */}
          <div className="form-group">
            <label className="form-label" htmlFor="installation-select">
              GitHub Installation
              <span className="input-required-star" aria-hidden="true">*</span>
            </label>
            <select
              id="installation-select"
              className={`form-select ${errors.installationId ? 'form-input-error' : ''}`}
              value={selectedInstallationId}
              onChange={(e) => onSelectInstallation(e.target.value)}
              disabled={disabled || loadingInstallations || installations.length === 0}
              aria-describedby={errors.installationId ? 'installation-error-msg' : 'installation-helper-note'}
              aria-invalid={Boolean(errors.installationId)}
            >
              {loadingInstallations ? (
                <option value="">Loading connected installations...</option>
              ) : installations.length === 0 ? (
                <option value="">No installations found (use Manual Entry)</option>
              ) : (
                <>
                  <option value="">-- Choose GitHub Installation --</option>
                  {installations.map((inst) => {
                    const id = String(inst.githubInstallationId || inst.id);
                    const name = inst.githubAccountLogin || `Installation #${id}`;
                    return (
                      <option key={id} value={id}>
                        {name} (Installation ID: {id})
                      </option>
                    );
                  })}
                </>
              )}
            </select>
            {errors.installationId && (
              <div id="installation-error-msg" className="input-error-msg" role="alert">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{errors.installationId}</span>
              </div>
            )}
            <p id="installation-helper-note" className="input-helper-note">
              Select the GitHub App installation authorized for your target repository.
            </p>
          </div>

          {/* Repository Selector */}
          <div className="form-group">
            <label className="form-label" htmlFor="repository-select">
              Repository
              <span className="input-required-star" aria-hidden="true">*</span>
            </label>
            <select
              id="repository-select"
              className={`form-select ${errors.repository ? 'form-input-error' : ''}`}
              value={selectedRepository}
              onChange={(e) => onSelectRepository(e.target.value)}
              disabled={disabled || !selectedInstallationId || loadingRepositories}
              aria-describedby={errors.repository ? 'repository-error-msg' : 'repository-helper-note'}
              aria-invalid={Boolean(errors.repository)}
            >
              {!selectedInstallationId ? (
                <option value="">Select an installation first</option>
              ) : loadingRepositories ? (
                <option value="">Loading repositories...</option>
              ) : repositories.length === 0 ? (
                <option value="">No repositories available for this installation</option>
              ) : (
                <>
                  <option value="">-- Choose Repository --</option>
                  {repositories.map((repo) => {
                    const fullName = repo.full_name || repo.fullName || repo.name;
                    return (
                      <option key={repo.id || fullName} value={fullName}>
                        {fullName}
                      </option>
                    );
                  })}
                </>
              )}
            </select>

            {loadingRepositories && (
              <div className="input-loading-status" aria-live="polite">
                <span className="submit-spinner" style={{ width: '12px', height: '12px' }} aria-hidden="true" />
                <span>Loading repositories from GitHub...</span>
              </div>
            )}

            {errors.repository && (
              <div id="repository-error-msg" className="input-error-msg" role="alert">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{errors.repository}</span>
              </div>
            )}

            <p id="repository-helper-note" className="input-helper-note">
              Repositories accessible via the selected GitHub App installation.
            </p>
          </div>
        </div>
      ) : (
        /* Manual Entry Mode */
        <div className="manual-mode-fields">
          <div className="form-group">
            <label className="form-label" htmlFor="manual-installation-id">
              GitHub App Installation ID
              <span className="input-required-star" aria-hidden="true">*</span>
            </label>
            <input
              id="manual-installation-id"
              type="number"
              min="1"
              step="1"
              className={`form-input ${errors.installationId ? 'form-input-error' : ''}`}
              placeholder="e.g. 154790187"
              value={manualInstallationId}
              onChange={(e) => onChangeManualInstallationId(e.target.value)}
              disabled={disabled}
              required
              aria-describedby={errors.installationId ? 'manual-inst-error' : 'manual-inst-note'}
              aria-invalid={Boolean(errors.installationId)}
            />
            {errors.installationId && (
              <div id="manual-inst-error" className="input-error-msg" role="alert">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{errors.installationId}</span>
              </div>
            )}
            <p id="manual-inst-note" className="input-helper-note">
              Numerical GitHub App installation ID authorized for this repository.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label" htmlFor="manual-owner">
                Repository Owner / Org
                <span className="input-required-star" aria-hidden="true">*</span>
              </label>
              <input
                id="manual-owner"
                type="text"
                className={`form-input ${errors.owner ? 'form-input-error' : ''}`}
                placeholder="e.g. PrajapatiPushkar"
                value={owner}
                onChange={(e) => onChangeOwner(e.target.value)}
                disabled={disabled}
                required
                aria-describedby={errors.owner ? 'manual-owner-error' : undefined}
                aria-invalid={Boolean(errors.owner)}
              />
              {errors.owner && (
                <div id="manual-owner-error" className="input-error-msg" role="alert">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{errors.owner}</span>
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="manual-repo">
                Repository Name
                <span className="input-required-star" aria-hidden="true">*</span>
              </label>
              <input
                id="manual-repo"
                type="text"
                className={`form-input ${errors.repository ? 'form-input-error' : ''}`}
                placeholder="e.g. fitness-monolith"
                value={repositoryName}
                onChange={(e) => onChangeRepositoryName(e.target.value)}
                disabled={disabled}
                required
                aria-describedby={errors.repository ? 'manual-repo-error' : undefined}
                aria-invalid={Boolean(errors.repository)}
              />
              {errors.repository && (
                <div id="manual-repo-error" className="input-error-msg" role="alert">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{errors.repository}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReviewTargetForm;
