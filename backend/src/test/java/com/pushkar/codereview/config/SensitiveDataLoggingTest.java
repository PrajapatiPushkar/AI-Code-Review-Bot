package com.pushkar.codereview.config;

import com.pushkar.codereview.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SensitiveDataLoggingTest {

    @Test
    void testJwtFilterDoesNotLeakTokenIntoMdcOrResponseHeaders() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sensitiveJwtSecretTokenValue12345");
        request.setRequestURI("/api/v1/code-reviews");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get("token")).isNull();
        assertThat(MDC.get("Authorization")).isNull();
        assertThat(MDC.get("jwt")).isNull();

        assertThat(response.getHeader("Authorization")).isNull();
    }
}
