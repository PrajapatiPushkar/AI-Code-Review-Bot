package com.pushkar.codereview.security;

import com.pushkar.codereview.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(
                "superSecretKey123456789012345678901234567890",
                3600000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void testTokenGenerationAndSubjectExtraction() {
        String token = jwtService.generateToken("user@example.com", "USER");

        assertThat(token).isNotBlank();

        String extractedSubject = jwtService.extractUsername(token);
        assertThat(extractedSubject).isEqualTo("user@example.com");
    }

    @Test
    void testValidTokenValidation() {
        String token = jwtService.generateToken("user@example.com", "USER");
        UserDetails userDetails = new User("user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        boolean isValid = jwtService.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    void testMismatchUsernameValidation_ReturnsFalse() {
        String token = jwtService.generateToken("user@example.com", "USER");
        UserDetails otherUser = new User("other@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        boolean isValid = jwtService.validateToken(token, otherUser);
        assertThat(isValid).isFalse();
    }

    @Test
    void testExpiredTokenRejection() {
        JwtProperties expiredProps = new JwtProperties(
                "superSecretKey123456789012345678901234567890",
                -1000L, // Already expired
                "Bearer",
                "ai-code-review-bot-test"
        );
        JwtService expiredJwtService = new JwtService(expiredProps);

        String token = expiredJwtService.generateToken("user@example.com");
        UserDetails userDetails = new User("user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(expiredJwtService.isTokenExpired(token)).isTrue();
        assertThat(expiredJwtService.validateToken(token, userDetails)).isFalse();
    }

    @Test
    void testMalformedTokenRejection() {
        String malformedToken = "invalid.token.structure";
        UserDetails userDetails = new User("user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(jwtService.extractUsername(malformedToken)).isNull();
        assertThat(jwtService.validateToken(malformedToken, userDetails)).isFalse();
        assertThat(jwtService.isValidToken(malformedToken)).isFalse();
    }

    @Test
    void testInvalidSignatureRejection() {
        JwtProperties otherProps = new JwtProperties(
                "DIFFERENTSecretKey123456789012345678901234567890",
                3600000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        JwtService otherJwtService = new JwtService(otherProps);
        String tokenSignedByOther = otherJwtService.generateToken("user@example.com");

        UserDetails userDetails = new User("user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(jwtService.extractUsername(tokenSignedByOther)).isNull();
        assertThat(jwtService.validateToken(tokenSignedByOther, userDetails)).isFalse();
    }

    @Test
    void testBlankUsername_ThrowsException() {
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username/subject must not be blank");

        assertThatThrownBy(() -> jwtService.generateToken("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username/subject must not be blank");
    }
}
