package com.pushkar.codereview.github.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.pushkar.codereview.config.GithubProperties;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class GithubJwtService {

    private final GithubProperties githubProperties;

    public GithubJwtService(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public String generateAppJwt() {
        String appId = githubProperties.getAppId();
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("GitHub App ID is not configured");
        }

        String rawPrivateKey = githubProperties.getPrivateKey();
        if (rawPrivateKey == null || rawPrivateKey.isBlank()) {
            throw new IllegalStateException("GitHub App private key is not configured");
        }

        RSAPrivateKey privateKey = parsePrivateKey(rawPrivateKey);
        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        Instant now = Instant.now();
        Instant issuedAt = now.minusSeconds(60); // 60s in the past for clock skew
        Instant expiresAt = now.plusSeconds(600); // 10 minutes short-lived expiration

        return JWT.create()
                .withIssuer(appId)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String sanitized = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                                  .replace("-----END RSA PRIVATE KEY-----", "")
                                  .replace("-----BEGIN PRIVATE KEY-----", "")
                                  .replace("-----END PRIVATE KEY-----", "")
                                  .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            try {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
                return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException e) {
                RSAPrivateCrtKeySpec pkcs1KeySpec = parsePkcs1PrivateKey(decoded);
                return (RSAPrivateKey) keyFactory.generatePrivate(pkcs1KeySpec);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RSA private key configuration", e);
        }
    }

    private RSAPrivateCrtKeySpec parsePkcs1PrivateKey(byte[] der) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(der);
        if (buffer.get() != 0x30) {
            throw new IllegalArgumentException("Invalid DER sequence tag");
        }
        readDerLength(buffer);

        readDerInteger(buffer); // version
        BigInteger modulus = readDerInteger(buffer);
        BigInteger publicExponent = readDerInteger(buffer);
        BigInteger privateExponent = readDerInteger(buffer);
        BigInteger primeP = readDerInteger(buffer);
        BigInteger primeQ = readDerInteger(buffer);
        BigInteger primeExponentP = readDerInteger(buffer);
        BigInteger primeExponentQ = readDerInteger(buffer);
        BigInteger crtCoefficient = readDerInteger(buffer);

        return new RSAPrivateCrtKeySpec(
                modulus, publicExponent, privateExponent,
                primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient
        );
    }

    private int readDerLength(ByteBuffer buffer) {
        int length = buffer.get() & 0xFF;
        if ((length & 0x80) != 0) {
            int numberOfBytes = length & 0x7F;
            length = 0;
            for (int i = 0; i < numberOfBytes; i++) {
                length = (length << 8) | (buffer.get() & 0xFF);
            }
        }
        return length;
    }

    private BigInteger readDerInteger(ByteBuffer buffer) {
        if (buffer.get() != 0x02) {
            throw new IllegalArgumentException("Invalid DER integer tag");
        }
        int length = readDerLength(buffer);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new BigInteger(bytes);
    }
}
