package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubJwtService;
import com.pushkar.codereview.github.client.dto.GithubInstallationDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubInstallationVerificationClientTest {

    private MockRestServiceServer mockServer;
    private GithubJwtService githubJwtService;
    private GithubInstallationVerificationClient verificationClient;

    @BeforeEach
    void setUp() {
        GithubProperties githubProperties = new GithubProperties();
        githubProperties.setApiBaseUrl("https://api.github.com");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        githubJwtService = new StubGithubJwtService();
        verificationClient = new GithubInstallationVerificationClient(
                githubProperties,
                githubJwtService,
                builder
        );
    }

    @Test
    void testGetInstallationDetails_Success() {
        mockServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer mock-jwt-token"))
                .andRespond(withSuccess("{\"id\":123456,\"account\":{\"login\":\"octocat\",\"type\":\"User\"}}", MediaType.APPLICATION_JSON));

        GithubInstallationDetailsResponse response = verificationClient.getInstallationDetails(123456L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(123456L);
        assertThat(response.getAccountLogin()).isEqualTo("octocat");
        assertThat(response.getAccountType()).isEqualTo("User");
    }

    @Test
    void testGetInstallationDetails_NotFound_ThrowsResourceNotFoundException() {
        mockServer.expect(requestTo("https://api.github.com/app/installations/999999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> verificationClient.getInstallationDetails(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub installation not found on GitHub with ID: 999999");
    }

    @Test
    void testGetInstallationDetails_Unauthorized_ThrowsGithubApiException() {
        mockServer.expect(requestTo("https://api.github.com/app/installations/123456"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> verificationClient.getInstallationDetails(123456L))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("authentication/authorization failed");
    }

    private static class StubGithubJwtService extends GithubJwtService {
        public StubGithubJwtService() { super(null); }
        @Override
        public String generateAppJwt() {
            return "mock-jwt-token";
        }
    }
}
