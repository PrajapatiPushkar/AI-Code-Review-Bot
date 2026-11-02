package com.pushkar.codereview.repository;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.repository.dto.RepositoryCreateRequest;
import com.pushkar.codereview.repository.dto.RepositoryResponse;
import com.pushkar.codereview.repository.mapper.RepositoryMapper;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final RepositoryMapper repositoryMapper;
    private final CurrentUserService currentUserService;

    public RepositoryService(RepositoryRepository repositoryRepository,
                             UserRepository userRepository,
                             RepositoryMapper repositoryMapper) {
        this(repositoryRepository, userRepository, repositoryMapper, null);
    }

    @Autowired
    public RepositoryService(RepositoryRepository repositoryRepository,
                             UserRepository userRepository,
                             RepositoryMapper repositoryMapper,
                             @Autowired(required = false) CurrentUserService currentUserService) {
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.repositoryMapper = repositoryMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RepositoryResponse createRepository(RepositoryCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Repository request cannot be null");
        }

        if (currentUserService != null) {
            if (!currentUserService.isAuthenticated()) {
                throw new AccessDeniedException("Full authentication is required to create repositories");
            }
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(request.getUserId())) {
                    log.warn("Access denied for userId={} attempting to create repository for userId={}",
                            currentUserId, request.getUserId());
                    throw new AccessDeniedException("Cannot create repository for another user");
                }
            }
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        if (repositoryRepository.existsByGithubRepositoryId(request.getGithubRepositoryId())) {
            throw new DuplicateResourceException("Repository with GitHub ID " + request.getGithubRepositoryId() + " already exists");
        }

        com.pushkar.codereview.repository.Repository repository = repositoryMapper.toEntity(request, user);
        com.pushkar.codereview.repository.Repository savedRepository = repositoryRepository.save(repository);

        return repositoryMapper.toResponse(savedRepository);
    }

    @Transactional(readOnly = true)
    public RepositoryResponse getRepositoryById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Repository ID must be positive");
        }

        if (currentUserService != null && !currentUserService.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required to access repository metadata");
        }

        com.pushkar.codereview.repository.Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + id));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                Long ownerUserId = (repository.getUser() != null) ? repository.getUser().getId() : null;
                if (currentUserId == null || !currentUserId.equals(ownerUserId)) {
                    log.warn("Access denied for userId={} attempting to access repository ID={}",
                            currentUserId, id);
                    throw new ResourceNotFoundException("Repository not found with ID: " + id);
                }
            }
        }

        return repositoryMapper.toResponse(repository);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getRepositoriesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }

        if (currentUserService != null && !currentUserService.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required to list repositories");
        }

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (currentUserId == null || !currentUserId.equals(userId)) {
                    log.warn("Access denied for userId={} attempting to list repositories for userId={}",
                            currentUserId, userId);
                    throw new ResourceNotFoundException("User not found with ID: " + userId);
                }
            }
        }

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return repositoryRepository.findByUserId(userId).stream()
                .map(repositoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getCurrentUserRepositories() {
        if (currentUserService == null || !currentUserService.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required to list repositories");
        }

        Long currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        return repositoryRepository.findByUserId(currentUserId).stream()
                .map(repositoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
