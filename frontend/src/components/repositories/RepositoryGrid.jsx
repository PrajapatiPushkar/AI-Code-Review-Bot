import React from 'react';
import RepositoryCard from './RepositoryCard';

export const RepositoryGrid = ({
  repositories = [],
  installationId,
  installationAccount,
  onOpenPolicy
}) => {
  if (!repositories || repositories.length === 0) {
    return null;
  }

  return (
    <div className="repository-grid" role="list" aria-label="Repositories list">
      {repositories.map((repo) => (
        <RepositoryCard
          key={repo.id || repo.full_name || repo.fullName || repo.name}
          repository={repo}
          installationId={installationId}
          installationAccount={installationAccount}
          onOpenPolicy={onOpenPolicy}
        />
      ))}
    </div>
  );
};

export default RepositoryGrid;
