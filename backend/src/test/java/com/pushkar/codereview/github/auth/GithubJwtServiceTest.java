package com.pushkar.codereview.github.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pushkar.codereview.config.GithubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubJwtServiceTest {

    private GithubProperties githubProperties;
    private GithubJwtService githubJwtService;
    private String validPemPrivateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        githubProperties = new GithubProperties();
        githubProperties.setAppId("123456");

        // Dynamically generate a 2048-bit RSA key pair in-memory for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        validPemPrivateKey = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----";
        githubProperties.setPrivateKey(validPemPrivateKey);

        githubJwtService = new GithubJwtService(githubProperties);
    }

    @Test
    void testGenerateAppJwt_Success() {
        Instant before = Instant.now();
        String jwtToken = githubJwtService.generateAppJwt();
        Instant after = Instant.now();

        assertThat(jwtToken).isNotNull().isNotBlank();
        
        DecodedJWT decodedJWT = JWT.decode(jwtToken);

        // Verify RS256 algorithm
        assertThat(decodedJWT.getAlgorithm()).isEqualTo("RS256");

        // Verify Issuer
        assertThat(decodedJWT.getIssuer()).isEqualTo("123456");

        // Verify timestamps
        Instant iat = decodedJWT.getIssuedAtAsInstant();
        Instant exp = decodedJWT.getExpiresAtAsInstant();

        // iat must be backdated by 60s and not in the future
        assertThat(iat).isBeforeOrEqualTo(before);
        assertThat(iat).isBefore(exp);
        assertThat(exp).isAfter(after);

        // Expiration duration should be exactly 540s (from now-60s to now+480s)
        long durationSeconds = exp.getEpochSecond() - iat.getEpochSecond();
        assertThat(durationSeconds).isEqualTo(540L);
        // Must not exceed GitHub's 600-second maximum
        assertThat(durationSeconds).isLessThanOrEqualTo(600L);

        // Verify RS256 signature validity using corresponding public key
        JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer("123456")
                .build()
                .verify(jwtToken);
    }

    @Test
    void testGenerateAppJwt_ExpirationWithinGithubLimit() {
        String jwtToken = githubJwtService.generateAppJwt();
        DecodedJWT decodedJWT = JWT.decode(jwtToken);

        Instant iat = decodedJWT.getIssuedAtAsInstant();
        Instant exp = decodedJWT.getExpiresAtAsInstant();

        long durationSeconds = exp.getEpochSecond() - iat.getEpochSecond();
        assertThat(durationSeconds).isPositive();
        assertThat(durationSeconds).isLessThanOrEqualTo(600L);
    }

    @Test
    void testGenerateAppJwt_IssuedAtBackdated() {
        Instant now = Instant.now();
        String jwtToken = githubJwtService.generateAppJwt();
        DecodedJWT decodedJWT = JWT.decode(jwtToken);

        Instant iat = decodedJWT.getIssuedAtAsInstant();
        // iat should be roughly 60 seconds before now (allow 5 seconds clock slack)
        assertThat(iat.getEpochSecond()).isBetween(now.getEpochSecond() - 65, now.getEpochSecond() - 55);
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
