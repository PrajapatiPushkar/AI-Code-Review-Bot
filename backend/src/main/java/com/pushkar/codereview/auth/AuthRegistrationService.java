package com.pushkar.codereview.auth;

import com.pushkar.codereview.auth.dto.RegisterRequest;
import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import com.pushkar.codereview.user.dto.UserResponse;
import com.pushkar.codereview.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthRegistrationService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request must not be null");
        }

        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        email = email.trim();

        String password = request.getPassword();
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User with email '" + email + "' already exists");
        }

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }
        username = username.trim();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User with username '" + username + "' already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(username, email, encodedPassword, "USER");
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}
