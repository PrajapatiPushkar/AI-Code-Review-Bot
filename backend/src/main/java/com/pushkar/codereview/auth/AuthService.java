package com.pushkar.codereview.auth;

import com.pushkar.codereview.auth.dto.LoginRequest;
import com.pushkar.codereview.auth.dto.LoginResponse;
import com.pushkar.codereview.auth.dto.RegisterRequest;
import com.pushkar.codereview.config.JwtProperties;
import com.pushkar.codereview.security.JwtService;
import com.pushkar.codereview.user.dto.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthRegistrationService registrationService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       AuthRegistrationService registrationService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.registrationService = registrationService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsernameOrEmail() == null || request.getUsernameOrEmail().isBlank()) {
            throw new IllegalArgumentException("Username or email must not be blank");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail().trim(),
                        request.getPassword()
                )
        );

        String username = request.getUsernameOrEmail().trim();
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        }

        String token = jwtService.generateToken(username);

        return new LoginResponse(token, jwtProperties.getTokenType(), jwtProperties.getExpirationMs());
    }

    public UserResponse register(RegisterRequest request) {
        return registrationService.register(request);
    }
}
