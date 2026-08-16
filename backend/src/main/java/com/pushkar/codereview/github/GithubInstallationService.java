package com.pushkar.codereview.github;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.GithubInstallationTokenClient;
import com.pushkar.codereview.github.client.GithubInstallationVerificationClient;
import com.pushkar.codereview.github.client.dto.GithubInstallationDetailsResponse;
import com.pushkar.codereview.github.dto.GithubInstallationCreateRequest;
import com.pushkar.codereview.github.dto.GithubInstallationRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.github.mapper.GithubInstallationMapper;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GithubInstallationService {

    private final GithubInstallationRepository githubInstallationRepository;
    private final UserRepository userRepository;
    private final GithubInstallationMapper githubInstallationMapper;
    private final CurrentUserService currentUserService;
    private final GithubInstallationTokenClient installationTokenClient;
    private final GithubInstallationVerificationClient verificationClient;

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository,
                                     UserRepository userRepository,
                                     GithubInstallationMapper githubInstallationMapper) {
        this(githubInstallationRepository, userRepository, githubInstallationMapper, null, null, null);
    }

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository,
                                     UserRepository userRepository,
                                     GithubInstallationMapper githubInstallationMapper,
                                     CurrentUserService currentUserService) {
        this(githubInstallationRepository, userRepository, githubInstallationMapper, currentUserService, null, null);
    }

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository,
                                     UserRepository userRepository,
                                     GithubInstallationMapper githubInstallationMapper,
                                     CurrentUserService currentUserService,
                                     GithubInstallationTokenClient installationTokenClient,
                                     GithubInstallationVerificationClient verificationClient) {
        this.githubInstallationRepository = githubInstallationRepository;
        this.userRepository = userRepository;
        this.githubInstallationMapper = githubInstallationMapper;
        this.currentUserService = currentUserService;
        this.installationTokenClient = installationTokenClient;
        this.verificationClient = verificationClient;
    }

    @Transactional
    public GithubInstallationResponse registerInstallation(GithubInstallationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        User currentUser = null;
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            currentUser = currentUserService.getCurrentUser();
        } else {
            throw new AccessDeniedException("Authentication is required to register an installation");
        }

        Long installationId = request.getInstallationId();
        Optional<GithubInstallation> existingOpt = githubInstallationRepository.findByGithubInstallationId(installationId);

        if (existingOpt.isPresent()) {
            GithubInstallation existing = existingOpt.get();
            if (existing.getUser() != null && existing.getUser().getId().equals(currentUser.getId())) {
                if (existing.isVerified()) {
                    return githubInstallationMapper.toResponse(existing);
                } else {
                    return verifyAndSaveInstallation(existing, request);
                }
            } else {
                throw new DuplicateResourceException("GitHub installation ID " + installationId + " is already registered to another user");
            }
        }

        GithubInstallation newInstallation = new GithubInstallation(
                currentUser,
                installationId,
                request.getGithubAccountLogin(),
                request.getGithubAccountType()
        );

        return verifyAndSaveInstallation(newInstallation, request);
    }

    private GithubInstallationResponse verifyAndSaveInstallation(GithubInstallation installation, GithubInstallationRequest request) {
        if (installationTokenClient != null) {
            installationTokenClient.requestInstallationToken(installation.getGithubInstallationId());
        }

        String verifiedLogin = request.getGithubAccountLogin();
        String verifiedType = request.getGithubAccountType();

        if (verificationClient != null) {
            GithubInstallationDetailsResponse githubDetails = verificationClient.getInstallationDetails(installation.getGithubInstallationId());
            if (githubDetails != null) {
                String remoteLogin = githubDetails.getAccountLogin();
                if (remoteLogin != null && !remoteLogin.equalsIgnoreCase(request.getGithubAccountLogin())) {
                    throw new GithubInstallationVerificationException(
                            "GitHub account login mismatch: provided '" + request.getGithubAccountLogin() + "', but GitHub API returned '" + remoteLogin + "'"
                    );
                }
                if (remoteLogin != null) {
                    verifiedLogin = remoteLogin;
                }
                if (githubDetails.getAccountType() != null) {
                    verifiedType = githubDetails.getAccountType();
                }
            }
        }

        installation.setGithubAccountLogin(verifiedLogin);
        installation.setGithubAccountType(verifiedType);
        installation.setVerified(true);
        installation.setVerifiedAt(Instant.now());

        GithubInstallation saved = githubInstallationRepository.save(installation);
        return githubInstallationMapper.toResponse(saved);
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

        Optional<GithubInstallation> existingOpt = githubInstallationRepository.findByGithubInstallationId(request.getGithubInstallationId());
        if (existingOpt.isPresent()) {
            GithubInstallation existing = existingOpt.get();
            if (existing.getUser() != null && existing.getUser().getId().equals(user.getId())) {
                return githubInstallationMapper.toResponse(existing);
            } else {
                throw new DuplicateResourceException("GitHub installation ID " + request.getGithubInstallationId() + " is already registered to another user");
            }
        }

        GithubInstallation installation = githubInstallationMapper.toEntity(request, user);
        installation.setVerified(true);
        installation.setVerifiedAt(Instant.now());

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
