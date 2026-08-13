package com.pushkar.codereview.user;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.user.dto.UserCreateRequest;
import com.pushkar.codereview.user.dto.UserResponse;
import com.pushkar.codereview.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByGithubId(request.getGithubId())) {
            throw new DuplicateResourceException("User with GitHub ID " + request.getGithubId() + " already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User with username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByGithubId(Long githubId) {
        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with GitHub ID: " + githubId));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
}
