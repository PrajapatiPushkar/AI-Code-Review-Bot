package com.pushkar.codereview.security;

import com.pushkar.codereview.config.JwtProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private JwtProperties jwtProperties;
    private JwtService jwtService;
    private StubCustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtProperties = new JwtProperties(
                "secretKeyForFilterTest123456789012345678901234567890",
                3600000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        jwtService = new JwtService(jwtProperties);
        userDetailsService = new StubCustomUserDetailsService();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        userDetailsService.addUser("test@example.com", "password", "USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testNoAuthorizationHeader_ChainContinues_Unauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testMalformedAuthorizationHeader_ChainContinues_Unauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dGVzdDp0ZXN0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testValidBearerToken_AuthenticatesSecurityContext() throws Exception {
        String token = jwtService.generateToken("test@example.com", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test@example.com");
    }

    @Test
    void testExpiredToken_DoesNotAuthenticate() throws Exception {
        JwtProperties expiredProps = new JwtProperties(
                "secretKeyForFilterTest123456789012345678901234567890",
                -1000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        JwtService expiredJwtService = new JwtService(expiredProps);
        String expiredToken = expiredJwtService.generateToken("test@example.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testInvalidTokenSignature_DoesNotAuthenticate() throws Exception {
        JwtProperties wrongProps = new JwtProperties(
                "WRONGKeyForFilterTest123456789012345678901234567890",
                3600000L,
                "Bearer",
                "ai-code-review-bot-test"
        );
        JwtService wrongJwtService = new JwtService(wrongProps);
        String invalidToken = wrongJwtService.generateToken("test@example.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static class StubCustomUserDetailsService extends CustomUserDetailsService {
        private final Map<String, UserDetails> users = new HashMap<>();

        public StubCustomUserDetailsService() {
            super(null);
        }

        public void addUser(String username, String password, String role) {
            users.put(username, User.withUsername(username)
                    .password(password)
                    .roles(role)
                    .build());
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            UserDetails user = users.get(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            return user;
        }
    }
}
