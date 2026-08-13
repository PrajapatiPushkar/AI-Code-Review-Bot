package com.pushkar.codereview.github;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.dto.GithubInstallationCreateRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.github.mapper.GithubInstallationMapper;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GithubInstallationService {

    private final GithubInstallationRepository githubInstallationRepository;
    private final UserRepository userRepository;
    private final GithubInstallationMapper githubInstallationMapper;

    public GithubInstallationService(GithubInstallationRepository githubInstallationRepository, UserRepository userRepository, GithubInstallationMapper githubInstallationMapper) {
        this.githubInstallationRepository = githubInstallationRepository;
        this.userRepository = userRepository;
        this.githubInstallationMapper = githubInstallationMapper;
    }

    @Transactional
    public GithubInstallationResponse createInstallation(GithubInstallationCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        if (githubInstallationRepository.existsByGithubInstallationId(request.getGithubInstallationId())) {
            throw new DuplicateResourceException("GitHub installation with ID " + request.getGithubInstallationId() + " already exists");
        }

        GithubInstallation installation = githubInstallationMapper.toEntity(request, user);
        GithubInstallation savedInstallation = githubInstallationRepository.save(installation);

        return githubInstallationMapper.toResponse(savedInstallation);
    }

    @Transactional(readOnly = true)
    public GithubInstallationResponse getInstallationById(Long id) {
        GithubInstallation installation = githubInstallationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub installation not found with ID: " + id));

        return githubInstallationMapper.toResponse(installation);
    }

    @Transactional(readOnly = true)
    public List<GithubInstallationResponse> getInstallationsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        return githubInstallationRepository.findByUserId(userId).stream()
                .map(githubInstallationMapper::toResponse)
                .collect(Collectors.toList());
    }
}
