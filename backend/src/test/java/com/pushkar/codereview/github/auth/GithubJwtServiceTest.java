package com.pushkar.codereview.github.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pushkar.codereview.config.GithubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubJwtServiceTest {

    private GithubProperties githubProperties;
    private GithubJwtService githubJwtService;
    private String validPemPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        githubProperties = new GithubProperties();
        githubProperties.setAppId("123456");

        // Dynamically generate a 2048-bit RSA key pair in-memory for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        validPemPrivateKey = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----";
        githubProperties.setPrivateKey(validPemPrivateKey);

        githubJwtService = new GithubJwtService(githubProperties);
    }

    @Test
    void testGenerateAppJwt_Success() {
        String jwtToken = githubJwtService.generateAppJwt();

        assertThat(jwtToken).isNotNull().isNotBlank();
        
        DecodedJWT decodedJWT = JWT.decode(jwtToken);

        // Verify RS256 algorithm
        assertThat(decodedJWT.getAlgorithm()).isEqualTo("RS256");

        // Verify Issuer
        assertThat(decodedJWT.getIssuer()).isEqualTo("123456");

        // Verify timestamps
        Instant iat = decodedJWT.getIssuedAtAsInstant();
        Instant exp = decodedJWT.getExpiresAtAsInstant();

        assertThat(iat).isBefore(exp);
        
        // Expiration duration should be approx 660s (from now-60s to now+600s)
        long durationSeconds = exp.getEpochSecond() - iat.getEpochSecond();
        assertThat(durationSeconds).isEqualTo(660L);
    }

    @Test
    void testGenerateAppJwt_MissingAppId_ThrowsException() {
        githubProperties.setAppId(null);

        assertThatThrownBy(() -> githubJwtService.generateAppJwt())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GitHub App ID is not configured");
    }

    @Test
    void testGenerateAppJwt_MissingPrivateKey_ThrowsException() {
        githubProperties.setPrivateKey(null);

        assertThatThrownBy(() -> githubJwtService.generateAppJwt())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GitHub App private key is not configured");
    }

    @Test
    void testGenerateAppJwt_InvalidPrivateKey_ThrowsExceptionWithoutExposingKey() {
        String invalidKey = "-----BEGIN PRIVATE KEY-----\nINVALID_KEY_CONTENT\n-----END PRIVATE KEY-----";
        githubProperties.setPrivateKey(invalidKey);

        assertThatThrownBy(() -> githubJwtService.generateAppJwt())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid RSA private key configuration")
                .hasMessageNotContaining("INVALID_KEY_CONTENT");
    }

    @Test
    void testGenerateAppJwt_EscapedNewlines_Success() {
        String escapedKey = validPemPrivateKey.replace("\n", "\\n");
        githubProperties.setPrivateKey(escapedKey);

        String jwtToken = githubJwtService.generateAppJwt();
        assertThat(jwtToken).isNotNull().isNotBlank();
    }

    @Test
    void testGithubAppAuthenticatorDelegation() {
        GithubAppAuthenticator authenticator = new GithubAppAuthenticator(githubJwtService);
        String jwtToken = authenticator.getAppJwt();

        assertThat(jwtToken).isNotNull().isNotBlank();
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        assertThat(decodedJWT.getIssuer()).isEqualTo("123456");
    }
}
