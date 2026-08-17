package com.pushkar.codereview.config;

import com.pushkar.codereview.controller.HealthCheckController;
import com.pushkar.codereview.security.CustomUserDetailsService;
import com.pushkar.codereview.security.JwtAuthenticationFilter;
import com.pushkar.codereview.security.JwtService;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthCheckController.class})
@Import({
        SecurityConfig.class,
        CorrelationIdFilter.class,
        CustomUserDetailsService.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        JwtProperties.class,
        ActuatorEndpointsTest.TestConfig.class
})
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthCheckControllerEndpoint_ReturnsUpStatusAndCorrelationHeader() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }
}
