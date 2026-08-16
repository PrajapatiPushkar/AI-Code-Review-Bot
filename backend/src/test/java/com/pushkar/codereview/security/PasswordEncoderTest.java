package com.pushkar.codereview.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void testPasswordIsHashedAndNotEqualToPlaintext() {
        String rawPassword = "SecretPassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$");
    }

    @Test
    void testCorrectPasswordMatches() {
        String rawPassword = "MySecurePassword!";
        String encoded = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encoded);
        assertThat(matches).isTrue();
    }

    @Test
    void testIncorrectPasswordDoesNotMatch() {
        String rawPassword = "RightPassword";
        String wrongPassword = "WrongPassword";
        String encoded = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(wrongPassword, encoded);
        assertThat(matches).isFalse();
    }
}
