package com.pushkar.codereview.auth;

import com.pushkar.codereview.auth.dto.LoginRequest;
import com.pushkar.codereview.auth.dto.LoginResponse;
import com.pushkar.codereview.config.JwtProperties;
import com.pushkar.codereview.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private JwtProperties jwtProperties;
    private JwtService jwtService;
    private StubAuthenticationManager authenticationManager;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(
                "secretKeyForAuthServiceTest123456789012345678901234567890",
                3600000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        jwtService = new JwtService(jwtProperties);
        authenticationManager = new StubAuthenticationManager();

        authService = new AuthService(authenticationManager, jwtService, jwtProperties, null);
    }

    @Test
    void testSuccessfulLogin_GeneratesJwt() {
        authenticationManager.registerValidUser("test@example.com", "password123");

        LoginRequest request = new LoginRequest("test@example.com", "password123");
        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600000L);

        String subject = jwtService.extractUsername(response.getAccessToken());
        assertThat(subject).isEqualTo("test@example.com");
    }

    @Test
    void testInvalidCredentials_ThrowsBadCredentialsException() {
        authenticationManager.registerValidUser("test@example.com", "password123");

        LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void testBlankUsername_ThrowsIllegalArgumentException() {
        LoginRequest request = new LoginRequest("   ", "password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username or email must not be blank");
    }

    @Test
    void testBlankPassword_ThrowsIllegalArgumentException() {
        LoginRequest request = new LoginRequest("test@example.com", "   ");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must not be blank");
    }

    private static class StubAuthenticationManager implements AuthenticationManager {
        private String validUsername;
        private String validPassword;

        public void registerValidUser(String username, String password) {
            this.validUsername = username;
            this.validPassword = password;
        }

        @Override
        public Authentication authenticate(Authentication authentication) {
            String principal = (String) authentication.getPrincipal();
            String credentials = (String) authentication.getCredentials();

            if (validUsername != null && validUsername.equals(principal) && validPassword != null && validPassword.equals(credentials)) {
                UserDetails userDetails = User.withUsername(validUsername)
                        .password(validPassword)
                        .roles("USER")
                        .build();
                return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            }

            throw new BadCredentialsException("Invalid credentials");
        }
    }
}
