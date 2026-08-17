package com.pushkar.codereview.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void testFilterGeneratesCorrelationIdWhenMissing() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/v1/health");

        final String[] capturedMdcId = new String[1];
        doAnswer(invocation -> {
            capturedMdcId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull().isNotBlank();
        assertThat(capturedMdcId[0]).isEqualTo(responseHeader);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void testFilterPreservesExistingCorrelationIdHeader() throws ServletException, IOException {
        String existingId = "custom-correlation-12345";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        request.setMethod("POST");
        request.setRequestURI("/api/v1/code-reviews");

        final String[] capturedMdcId = new String[1];
        doAnswer(invocation -> {
            capturedMdcId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(capturedMdcId[0]).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void testFilterPreservesRequestIdHeaderWhenCorrelationIdMissing() throws ServletException, IOException {
        String requestId = "req-id-abc-999";
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, requestId);
        request.setMethod("GET");
        request.setRequestURI("/api/v1/health");

        final String[] capturedMdcId = new String[1];
        doAnswer(invocation -> {
            capturedMdcId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(requestId);
        assertThat(capturedMdcId[0]).isEqualTo(requestId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void testFilterRejectsUnsafeHeaderValuesAndGeneratesNew() throws ServletException, IOException {
        String unsafeHeader = "bad-header\r\nInject: true";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, unsafeHeader);

        final String[] capturedMdcId = new String[1];
        doAnswer(invocation -> {
            capturedMdcId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        String generatedHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedHeader).isNotEqualTo(unsafeHeader);
        assertThat(generatedHeader).doesNotContain("\r", "\n");
        assertThat(capturedMdcId[0]).isEqualTo(generatedHeader);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }
}
