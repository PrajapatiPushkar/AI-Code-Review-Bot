package com.pushkar.codereview.policy;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.rule.CodeQualityRule;
import com.pushkar.codereview.github.review.rule.RuleRegistry;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyRequest;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyResponse;
import com.pushkar.codereview.policy.dto.RuleDefinitionDto;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class RepositoryPolicyService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryPolicyService.class);

    private final RepositoryReviewPolicyRepository policyRepository;
    private final RepositoryRepository repositoryRepository;
    private final RuleRegistry ruleRegistry;
    private final CurrentUserService currentUserService;

    public RepositoryPolicyService(RepositoryReviewPolicyRepository policyRepository,
                                   RepositoryRepository repositoryRepository,
                                   RuleRegistry ruleRegistry,
                                   CurrentUserService currentUserService) {
        this.policyRepository = policyRepository;
        this.repositoryRepository = repositoryRepository;
        this.ruleRegistry = ruleRegistry;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public RepositoryReviewPolicyResponse getEffectivePolicy(Long repositoryId) {
        Repository repository = findAndAuthorizeRepository(repositoryId);
        Optional<RepositoryReviewPolicy> policyOpt = policyRepository.findByRepositoryId(repository.getId());

        if (policyOpt.isPresent()) {
            return mapToResponse(policyOpt.get(), repository, true);
        } else {
            RepositoryReviewPolicy defaultPolicy = RepositoryReviewPolicy.createDefault(repository);
            return mapToResponse(defaultPolicy, repository, false);
        }
    }

    public RepositoryReviewPolicyResponse updatePolicy(Long repositoryId, RepositoryReviewPolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Policy request body cannot be null");
        }
        if (request.getFailOnSeverity() == null) {
            throw new IllegalArgumentException("failOnSeverity must not be null");
        }

        Repository repository = findAndAuthorizeRepository(repositoryId);

        // Validate enabledRuleIds
        Set<String> ruleIdsToSet;
        if (request.getEnabledRuleIds() != null) {
            for (String ruleId : request.getEnabledRuleIds()) {
                if (ruleRegistry != null && !ruleRegistry.isSupportedRuleId(ruleId)) {
                    throw new IllegalArgumentException(String.format(
                            "Unsupported rule ID '%s'. Supported rules are: %s",
                            ruleId, ruleRegistry.getRegisteredRuleIds()));
                }
            }
            ruleIdsToSet = request.getEnabledRuleIds();
        } else {
            // If null, default to all supported rules
            ruleIdsToSet = ruleRegistry != null ? ruleRegistry.getRegisteredRuleIds() : Collections.emptySet();
        }

        RepositoryReviewPolicy policy = policyRepository.findByRepositoryId(repository.getId())
                .orElseGet(() -> {
                    RepositoryReviewPolicy p = new RepositoryReviewPolicy();
                    p.setRepository(repository);
                    return p;
                });

        policy.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        policy.setFailOnSeverity(request.getFailOnSeverity());
        policy.setEnabledRuleIdsSet(ruleIdsToSet);

        RepositoryReviewPolicy saved = policyRepository.save(policy);
        log.info("Updated review policy for repository ID={} (fullName={}): enabled={}, failOnSeverity={}, rules={}",
                repository.getId(), repository.getFullName(), saved.isEnabled(), saved.getFailOnSeverity(), saved.getEnabledRuleIds());

        return mapToResponse(saved, repository, true);
    }

    public RepositoryReviewPolicyResponse resetPolicy(Long repositoryId) {
        Repository repository = findAndAuthorizeRepository(repositoryId);

        RepositoryReviewPolicy policy = policyRepository.findByRepositoryId(repository.getId())
                .orElseGet(() -> {
                    RepositoryReviewPolicy p = new RepositoryReviewPolicy();
                    p.setRepository(repository);
                    return p;
                });

        Set<String> defaultRuleIds = (ruleRegistry != null && !ruleRegistry.getRegisteredRuleIds().isEmpty())
                ? ruleRegistry.getRegisteredRuleIds()
                : RepositoryReviewPolicy.createDefault(repository).getEnabledRuleIdsSet();

        policy.setEnabled(true);
        policy.setFailOnSeverity(RepositoryReviewPolicy.DEFAULT_SEVERITY);
        policy.setEnabledRuleIdsSet(defaultRuleIds);

        RepositoryReviewPolicy saved = policyRepository.save(policy);
        log.info("Reset review policy to defaults for repository ID={} (fullName={})",
                repository.getId(), repository.getFullName());

        return mapToResponse(saved, repository, false);
    }

    @Transactional(readOnly = true)
    public RepositoryReviewPolicy resolveEffectivePolicy(String owner, String repositoryName) {
        if (owner == null || repositoryName == null || owner.isBlank() || repositoryName.isBlank()) {
            return RepositoryReviewPolicy.createDefault(null);
        }

        String fullName = owner + "/" + repositoryName;
        Optional<Repository> repoOpt = repositoryRepository.findByFullNameIgnoreCase(fullName);
        if (repoOpt.isEmpty()) {
            List<Repository> byName = repositoryRepository.findByNameIgnoreCase(repositoryName);
            if (!byName.isEmpty()) {
                repoOpt = Optional.of(byName.get(0));
            }
        }

        if (repoOpt.isPresent()) {
            Repository repo = repoOpt.get();
            return policyRepository.findByRepositoryId(repo.getId())
                    .orElseGet(() -> RepositoryReviewPolicy.createDefault(repo));
        }

        return RepositoryReviewPolicy.createDefault(null);
    }

    public Repository findAndAuthorizeRepository(Long repositoryId) {
        if (repositoryId == null || repositoryId <= 0) {
            throw new IllegalArgumentException("Repository ID must be positive");
        }

        if (currentUserService != null && !currentUserService.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required to access repository policies");
        }

        Repository repository = repositoryRepository.findById(repositoryId)
                .or(() -> repositoryRepository.findByGithubRepositoryId(repositoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + repositoryId));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (repository.getUser() != null && !repository.getUser().getId().equals(currentUserId)) {
                    // Do not leak whether an unauthorized private repository exists
                    log.warn("Access denied for userId={} attempting to access repository ID={} owned by userId={}",
                            currentUserId, repositoryId, repository.getUser().getId());
                    throw new ResourceNotFoundException("Repository not found with ID: " + repositoryId);
                }
            }
        }

        return repository;
    }

    private RepositoryReviewPolicyResponse mapToResponse(RepositoryReviewPolicy policy, Repository repository, boolean isCustom) {
        List<RuleDefinitionDto> availableRules = new ArrayList<>();
        if (ruleRegistry != null) {
            for (CodeQualityRule rule : ruleRegistry.getRules()) {
                availableRules.add(new RuleDefinitionDto(
                        rule.getRuleId(),
                        rule.getName(),
                        rule.getDescription(),
                        rule.getCategory(),
                        rule.getSeverity()
                ));
            }
        }

        return new RepositoryReviewPolicyResponse(
                policy.getId(),
                repository != null ? repository.getId() : null,
                repository != null ? repository.getName() : null,
                repository != null ? repository.getFullName() : null,
                policy.isEnabled(),
                policy.getFailOnSeverity(),
                policy.getEnabledRuleIdsSet(),
                availableRules,
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                isCustom
        );
    }
}
