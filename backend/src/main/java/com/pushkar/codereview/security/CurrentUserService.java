package com.pushkar.codereview.security;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    public String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String username && !"anonymousUser".equals(username)) {
            return username;
        }

        return null;
    }

    public Optional<User> findCurrentUser() {
        String usernameOrEmail = getCurrentUsername();
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail));
    }

    public User getCurrentUser() {
        String usernameOrEmail = getCurrentUsername();
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new ResourceNotFoundException("No authenticated user in security context");
        }
        return findCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user record not found: " + usernameOrEmail));
    }

    public Long getCurrentUserId() {
        return findCurrentUser().map(User::getId).orElse(null);
    }

    public boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated() || role == null || role.isBlank()) {
            return false;
        }

        String targetRole = role.toUpperCase();
        if (!targetRole.startsWith("ROLE_")) {
            targetRole = "ROLE_" + targetRole;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (targetRole.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
