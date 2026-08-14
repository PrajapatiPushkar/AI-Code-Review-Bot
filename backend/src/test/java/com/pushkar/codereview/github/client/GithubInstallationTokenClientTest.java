package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubJwtService;
import com.pushkar.codereview.github.client.dto.GithubInstallationTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubInstallationTokenClientTest {

    private MockRestServiceServer mockServer;
    private GithubInstallationTokenClient tokenClient;

    private static final String MOCK_JWT = "mock.jwt.token";
    private static final String BASE_URL = "https://api.github.com";

    @BeforeEach
    void setUp() {
        GithubProperties githubProperties = new GithubProperties();
        githubProperties.setApiBaseUrl(BASE_URL);

        // Stub GithubJwtService without bytecode instrumentation
        GithubJwtService stubJwtService = new GithubJwtService(githubProperties) {
            @Override
            public String generateAppJwt() {
                return MOCK_JWT;
            }
        };

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        tokenClient = new GithubInstallationTokenClient(githubProperties, stubJwtService, builder);
    }

    @Test
    void testRequestInstallationToken_Success() {
        String responseJson = """
                {
                  "token": "ghs_16C7e42F292c6912E7710c838347Ae178B4a",
                  "expires_at": "2026-08-13T22:30:00Z"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/app/installations/98765432/access_tokens"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + MOCK_JWT))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubInstallationTokenResponse response = tokenClient.requestInstallationToken(98765432L);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("ghs_16C7e42F292c6912E7710c838347Ae178B4a");
        assertThat(response.getExpiresAt()).isEqualTo(Instant.parse("2026-08-13T22:30:00Z"));

        mockServer.verify();
    }

    @Test
    void testRequestInstallationToken_Unauthorized_401() {
        mockServer.expect(requestTo(BASE_URL + "/app/installations/98765432/access_tokens"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> tokenClient.requestInstallationToken(98765432L))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(MOCK_JWT);

        mockServer.verify();
    }

    @Test
    void testRequestInstallationToken_NotFound_404() {
        mockServer.expect(requestTo(BASE_URL + "/app/installations/98765432/access_tokens"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> tokenClient.requestInstallationToken(98765432L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub installation not found on GitHub with ID: 98765432")
                .hasMessageNotContaining(MOCK_JWT);

        mockServer.verify();
    }

    @Test
    void testRequestInstallationToken_ServerError_500() {
        mockServer.expect(requestTo(BASE_URL + "/app/installations/98765432/access_tokens"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> tokenClient.requestInstallationToken(98765432L))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 500")
                .hasMessageNotContaining(MOCK_JWT);

        mockServer.verify();
    }

    @Test
    void testRequestInstallationToken_InvalidId() {
        assertThatThrownBy(() -> tokenClient.requestInstallationToken(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");
    }
}
