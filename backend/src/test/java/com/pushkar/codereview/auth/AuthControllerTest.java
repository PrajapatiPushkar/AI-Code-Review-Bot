package com.pushkar.codereview.auth;

import com.pushkar.codereview.auth.dto.LoginRequest;
import com.pushkar.codereview.auth.dto.LoginResponse;
import com.pushkar.codereview.auth.dto.RegisterRequest;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private StubAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new StubAuthService();
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegister_Success_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    void testLogin_Success_Returns200WithJwtToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600000));
    }

    @Test
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"test@example.com\",\"password\":\"wrongPassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testLogin_InvalidRequestBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    private static class StubAuthService extends AuthService {
        public StubAuthService() {
            super(null, null, null, null);
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            return new UserResponse(10L, null, "newuser", request.getEmail(), null, "USER", Instant.now(), Instant.now());
        }

        @Override
        public LoginResponse login(LoginRequest request) {
            if ("wrongPassword".equals(request.getPassword())) {
                throw new BadCredentialsException("Invalid username or password");
            }
            return new LoginResponse("mocked.jwt.token", "Bearer", 3600000L);
        }
    }
}
