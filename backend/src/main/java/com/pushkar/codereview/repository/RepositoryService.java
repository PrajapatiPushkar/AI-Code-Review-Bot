package com.pushkar.codereview.repository;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.repository.dto.RepositoryCreateRequest;
import com.pushkar.codereview.repository.dto.RepositoryResponse;
import com.pushkar.codereview.repository.mapper.RepositoryMapper;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final RepositoryMapper repositoryMapper;

    public RepositoryService(RepositoryRepository repositoryRepository, UserRepository userRepository, RepositoryMapper repositoryMapper) {
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.repositoryMapper = repositoryMapper;
    }

    @Transactional
    public RepositoryResponse createRepository(RepositoryCreateRequest request) {
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
        com.pushkar.codereview.repository.Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + id));

        return repositoryMapper.toResponse(repository);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getRepositoriesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return repositoryRepository.findByUserId(userId).stream()
                .map(repositoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
