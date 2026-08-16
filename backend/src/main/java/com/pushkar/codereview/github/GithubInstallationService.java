package com.pushkar.codereview.github;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.dto.GithubInstallationCreateRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.github.mapper.GithubInstallationMapper;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GithubInstallationService {

    private final GithubInstallationRepository githubInstallationRepository;
    private final UserRepository userRepository;
    private final GithubInstallationMapper githubInstallationMapper;
    private final CurrentUserService currentUserService;

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository,
                                     UserRepository userRepository,
                                     GithubInstallationMapper githubInstallationMapper) {
        this(githubInstallationRepository, userRepository, githubInstallationMapper, null);
    }

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository,
                                     UserRepository userRepository,
                                     GithubInstallationMapper githubInstallationMapper,
                                     CurrentUserService currentUserService) {
        this.githubInstallationRepository = githubInstallationRepository;
        this.userRepository = userRepository;
        this.githubInstallationMapper = githubInstallationMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GithubInstallationResponse createInstallation(GithubInstallationCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        } else if (currentUserService != null && currentUserService.isAuthenticated()) {
            user = currentUserService.getCurrentUser();
        } else {
            throw new IllegalArgumentException("User must be specified or authenticated");
        }

        if (githubInstallationRepository.existsByGithubInstallationId(request.getGithubInstallationId())) {
            throw new DuplicateResourceException("GitHub installation with ID " + request.getGithubInstallationId() + " already exists");
        }

        GithubInstallation installation = githubInstallationMapper.toEntity(request, user);
        GithubInstallation savedInstallation = githubInstallationRepository.save(installation);

        return githubInstallationMapper.toResponse(savedInstallation);
    }

    @Transactional(readOnly = true)
    public List<GithubInstallationResponse> getInstallationsForCurrentUser() {
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (currentUserService.hasRole("ADMIN")) {
                return githubInstallationRepository.findAll().stream()
                        .map(githubInstallationMapper::toResponse)
                        .toList();
            }
            Long currentUserId = currentUserService.getCurrentUserId();
            if (currentUserId != null) {
                return githubInstallationRepository.findByUserId(currentUserId).stream()
                        .map(githubInstallationMapper::toResponse)
                        .toList();
            }
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public GithubInstallationResponse getInstallationById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Installation ID must be positive");
        }

        GithubInstallation installation = githubInstallationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub installation not found with ID: " + id));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (installation.getUser() != null && !installation.getUser().getId().equals(currentUserId)) {
                    throw new AccessDeniedException("You do not have permission to access this installation");
                }
            }
        }

        return githubInstallationMapper.toResponse(installation);
    }

    @Transactional(readOnly = true)
    public List<GithubInstallationResponse> getInstallationsByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (currentUserId != null && !userId.equals(currentUserId)) {
                    throw new AccessDeniedException("You do not have permission to access these installations");
                }
            }
        }

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return githubInstallationRepository.findByUserId(userId).stream()
                .map(githubInstallationMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteInstallation(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Installation ID must be positive");
        }

        GithubInstallation installation = githubInstallationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub installation not found with ID: " + id));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (installation.getUser() != null && !installation.getUser().getId().equals(currentUserId)) {
                    throw new AccessDeniedException("You do not have permission to delete this installation");
                }
            }
        }

        githubInstallationRepository.delete(installation);
    }
}
