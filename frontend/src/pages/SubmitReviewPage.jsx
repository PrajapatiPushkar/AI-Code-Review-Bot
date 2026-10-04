import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import reviewService from '../services/reviewService';
import repositoryService from '../services/repositoryService';
import useToast from '../hooks/useToast';
import ErrorMessage from '../components/ErrorMessage';
import ReviewSubmissionHeader from '../components/submit-review/ReviewSubmissionHeader';
import ReviewTargetCard from '../components/submit-review/ReviewTargetCard';
import ReviewTargetForm from '../components/submit-review/ReviewTargetForm';
import PullRequestInput from '../components/submit-review/PullRequestInput';
import SubmissionSummary from '../components/submit-review/SubmissionSummary';
import SubmissionActions from '../components/submit-review/SubmissionActions';
import SubmitReviewSkeleton from '../components/submit-review/SubmitReviewSkeleton';

export const SubmitReviewPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();

  // Navigation state passed from Repositories page or direct URL
  const originState = location.state;
  const initialInstallationId = originState?.installationId ? String(originState.installationId) : '';
  const initialOwner = originState?.owner || '';
  const initialRepository = originState?.repository || '';
  const hasOriginContext = Boolean(initialInstallationId && initialRepository);

  // Form Field States
  const [selectedInstallationId, setSelectedInstallationId] = useState(initialInstallationId);
  const [manualInstallationId, setManualInstallationId] = useState(initialInstallationId);
  const [owner, setOwner] = useState(initialOwner);
  const [repositoryName, setRepositoryName] = useState(initialRepository);
  const [selectedRepoFullName, setSelectedRepoFullName] = useState(
    initialOwner && initialRepository ? `${initialOwner}/${initialRepository}` : initialRepository
  );
  const [pullRequestNumber, setPullRequestNumber] = useState('');
  const [commitSha, setCommitSha] = useState('');

  // Mode & UI States
  const [isManualMode, setIsManualMode] = useState(false);
  const [showTargetSelector, setShowTargetSelector] = useState(!hasOriginContext);

  // API Data States
  const [installations, setInstallations] = useState([]);
  const [loadingInstallations, setLoadingInstallations] = useState(true);
  const [repositories, setRepositories] = useState([]);
  const [loadingRepositories, setLoadingRepositories] = useState(false);

  // Submission & Validation States
  const [submitting, setSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [apiError, setApiError] = useState(null);

  // Ref to track latest repository request
  const repoFetchIdRef = useRef(null);

  // Fetch installations on mount
  useEffect(() => {
    let isMounted = true;

    const fetchInstallations = async () => {
      try {
        setLoadingInstallations(true);
        const data = await repositoryService.getInstallations();
        if (!isMounted) return;

        const list = Array.isArray(data) ? data : [];
        setInstallations(list);

        if (list.length > 0) {
          // If navigation state had an installationId, verify or preselect it
          if (initialInstallationId) {
            const matched = list.find(
              (inst) => String(inst.githubInstallationId || inst.id) === initialInstallationId
            );
            if (matched) {
              setSelectedInstallationId(initialInstallationId);
            } else {
              setSelectedInstallationId(initialInstallationId);
            }
          } else {
            // Auto-select first installation if none pre-specified
            const firstId = String(list[0].githubInstallationId || list[0].id);
            setSelectedInstallationId(firstId);
            setManualInstallationId(firstId);
          }
        } else if (!initialInstallationId) {
          // No connected installations found, toggle to manual mode
          setIsManualMode(true);
        }
      } catch (err) {
        if (!isMounted) return;
        // Don't block manual entry if installations fail to load
        if (!initialInstallationId) {
          setIsManualMode(true);
        }
      } finally {
        if (isMounted) {
          setLoadingInstallations(false);
        }
      }
    };

    fetchInstallations();

    return () => {
      isMounted = false;
    };
  }, [initialInstallationId]);

  // Fetch repositories whenever selectedInstallationId changes in connected mode
  const fetchRepositories = useCallback(async (instId) => {
    if (!instId || isNaN(Number(instId))) {
      setRepositories([]);
      return;
    }

    repoFetchIdRef.current = instId;

    try {
      setLoadingRepositories(true);
      const data = await repositoryService.getRepositories(instId, { page: 1, perPage: 100 });

      if (repoFetchIdRef.current === instId) {
        const repoList = Array.isArray(data) ? data : [];
        setRepositories(repoList);

        // If repository was provided by navigation state and we haven't selected yet
        if (initialRepository && String(instId) === initialInstallationId) {
          const matchedRepo = repoList.find(
            (r) =>
              (r.name && r.name.toLowerCase() === initialRepository.toLowerCase()) ||
              (r.full_name && r.full_name.toLowerCase() === `${initialOwner}/${initialRepository}`.toLowerCase())
          );
          if (matchedRepo) {
            const full = matchedRepo.full_name || matchedRepo.fullName || matchedRepo.name;
            setSelectedRepoFullName(full);
            setRepositoryName(matchedRepo.name || initialRepository);
            if (matchedRepo.owner?.login) {
              setOwner(matchedRepo.owner.login);
            }
          }
        }
      }
    } catch (err) {
      if (repoFetchIdRef.current === instId) {
        setRepositories([]);
      }
    } finally {
      if (repoFetchIdRef.current === instId) {
        setLoadingRepositories(false);
      }
    }
  }, [initialInstallationId, initialOwner, initialRepository]);

  useEffect(() => {
    if (!isManualMode && selectedInstallationId) {
      fetchRepositories(selectedInstallationId);
    }
  }, [isManualMode, selectedInstallationId, fetchRepositories]);

  // Find active installation metadata
  const effectiveInstallationId = isManualMode ? manualInstallationId : selectedInstallationId;
  const currentInstallation = installations.find(
    (inst) => String(inst.githubInstallationId || inst.id) === String(effectiveInstallationId)
  );
  const installationAccount = currentInstallation?.githubAccountLogin || '';

  // Handler: Change Installation
  const handleSelectInstallation = (newId) => {
    if (newId === selectedInstallationId) return;

    setSelectedInstallationId(newId);
    setManualInstallationId(newId);

    // Consistency rule: Reset selected repository when switching installations
    setSelectedRepoFullName('');
    setRepositoryName('');
    setOwner('');
    setFormErrors((prev) => ({ ...prev, installationId: null, repository: null, owner: null }));
  };

  // Handler: Select Repository from Dropdown
  const handleSelectRepository = (fullName) => {
    setSelectedRepoFullName(fullName);

    if (!fullName) {
      setRepositoryName('');
      setOwner('');
      return;
    }

    const matched = repositories.find(
      (r) => (r.full_name || r.fullName || r.name) === fullName
    );

    if (matched) {
      setRepositoryName(matched.name);
      const repoOwner =
        matched.owner?.login ||
        (fullName.includes('/') ? fullName.split('/')[0] : installationAccount || '');
      setOwner(repoOwner);
    } else if (fullName.includes('/')) {
      const [parsedOwner, parsedRepo] = fullName.split('/');
      setOwner(parsedOwner);
      setRepositoryName(parsedRepo);
    } else {
      setRepositoryName(fullName);
      setOwner(installationAccount || '');
    }

    setFormErrors((prev) => ({ ...prev, repository: null, owner: null }));
  };

  // Handler: Manual Mode Toggle
  const handleToggleManualMode = (manual) => {
    setIsManualMode(manual);
    setFormErrors({});
  };

  // Form Validation
  const validateForm = () => {
    const errors = {};

    // 1. Installation ID
    const instIdNum = Number(effectiveInstallationId);
    if (!effectiveInstallationId || isNaN(instIdNum) || !Number.isInteger(instIdNum) || instIdNum <= 0) {
      errors.installationId = 'A valid positive GitHub Installation ID is required.';
    }

    // 2. Owner
    if (!owner.trim()) {
      errors.owner = 'Repository owner is required.';
    }

    // 3. Repository
    if (!repositoryName.trim()) {
      errors.repository = 'Repository name is required.';
    }

    // 4. Pull Request Number (strictly positive integer)
    const prNum = Number(pullRequestNumber);
    if (!pullRequestNumber || isNaN(prNum) || !Number.isInteger(prNum) || prNum <= 0) {
      errors.pullRequestNumber = 'Pull Request number must be a positive integer.';
    }

    // 5. Optional Commit SHA
    if (commitSha.trim()) {
      if (commitSha.trim().length > 64) {
        errors.commitSha = 'Commit SHA cannot exceed 64 characters.';
      } else if (/\s/.test(commitSha.trim())) {
        errors.commitSha = 'Commit SHA cannot contain spaces.';
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Submit Handler
  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError(null);

    if (submitting) return;

    if (!validateForm()) {
      toast.warning('Please resolve the validation errors before submitting.');
      return;
    }

    const payload = {
      installationId: Number(effectiveInstallationId),
      owner: owner.trim(),
      repository: repositoryName.trim(),
      pullRequestNumber: Number(pullRequestNumber),
      ...(commitSha.trim() ? { commitSha: commitSha.trim() } : {})
    };

    try {
      setSubmitting(true);
      const result = await reviewService.submitPullRequestReview(payload);

      // Verify returned review ID
      const newReviewId = result?.codeReviewId || result?.id;

      toast.success(
        `Review started for ${payload.owner}/${payload.repository} #${payload.pullRequestNumber}. The AI review is running in the background.`
      );

      if (newReviewId) {
        navigate(`/reviews/${newReviewId}`);
      } else {
        // Fallback navigation to reviews list if id not returned
        navigate('/reviews');
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Failed to submit pull request for AI review. Please check inputs and permissions.';
      setApiError(msg);
      toast.error(msg);
      setSubmitting(false);
    }
  };

  // Initial loading skeleton before installations resolution
  if (loadingInstallations && !hasOriginContext) {
    return <SubmitReviewSkeleton />;
  }

  const cancelPath = hasOriginContext ? '/repositories' : '/reviews';

  return (
    <div className="submit-review-container">
      {/* Page Header */}
      <ReviewSubmissionHeader
        hasOriginState={hasOriginContext}
        repository={repositoryName ? `${owner}/${repositoryName}` : ''}
      />

      {/* Global API Error Notice */}
      {apiError && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorMessage
            message={apiError}
            onRetry={() => {
              setApiError(null);
            }}
          />
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="submit-review-grid">
          {/* Main Form Fields Column */}
          <div className="submit-main-col">
            {/* Context Card (shown when target repository is confirmed) */}
            {owner && repositoryName && !showTargetSelector && (
              <ReviewTargetCard
                owner={owner}
                repository={repositoryName}
                installationId={effectiveInstallationId}
                installationAccount={installationAccount}
                onChangeTarget={() => setShowTargetSelector(true)}
              />
            )}

            {/* Target Selection Form (shown when selecting or changing target) */}
            {(showTargetSelector || !owner || !repositoryName) && (
              <ReviewTargetForm
                installations={installations}
                loadingInstallations={loadingInstallations}
                selectedInstallationId={selectedInstallationId}
                onSelectInstallation={handleSelectInstallation}
                repositories={repositories}
                loadingRepositories={loadingRepositories}
                selectedRepository={selectedRepoFullName}
                onSelectRepository={handleSelectRepository}
                owner={owner}
                onChangeOwner={(val) => {
                  setOwner(val);
                  setFormErrors((prev) => ({ ...prev, owner: null }));
                }}
                repositoryName={repositoryName}
                onChangeRepositoryName={(val) => {
                  setRepositoryName(val);
                  setFormErrors((prev) => ({ ...prev, repository: null }));
                }}
                manualInstallationId={manualInstallationId}
                onChangeManualInstallationId={(val) => {
                  setManualInstallationId(val);
                  setFormErrors((prev) => ({ ...prev, installationId: null }));
                }}
                isManualMode={isManualMode}
                onToggleManualMode={handleToggleManualMode}
                errors={formErrors}
                disabled={submitting}
              />
            )}

            {/* Pull Request & Commit SHA Inputs */}
            <PullRequestInput
              pullRequestNumber={pullRequestNumber}
              onChangePullRequestNumber={(val) => {
                setPullRequestNumber(val);
                setFormErrors((prev) => ({ ...prev, pullRequestNumber: null }));
              }}
              commitSha={commitSha}
              onChangeCommitSha={(val) => {
                setCommitSha(val);
                setFormErrors((prev) => ({ ...prev, commitSha: null }));
              }}
              errors={formErrors}
              disabled={submitting}
            />
          </div>

          {/* Side Summary & Submission Column */}
          <div className="submit-side-col">
            <SubmissionSummary
              owner={owner}
              repository={repositoryName}
              pullRequestNumber={pullRequestNumber}
              installationId={effectiveInstallationId}
              installationAccount={installationAccount}
              commitSha={commitSha}
            />

            <SubmissionActions
              submitting={submitting}
              disabled={submitting}
              onCancelPath={cancelPath}
            />
          </div>
        </div>
      </form>
    </div>
  );
};

export default SubmitReviewPage;
