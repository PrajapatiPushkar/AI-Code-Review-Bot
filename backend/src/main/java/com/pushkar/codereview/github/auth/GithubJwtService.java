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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GithubJwtService.class);

    public GithubJwtService(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public String generateAppJwt() {
        String appId = githubProperties.getAppId();
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("GitHub App ID is not configured");
        }

        String rawPrivateKey = loadPrivateKeyContent();
        if (rawPrivateKey == null || rawPrivateKey.isBlank()) {
            throw new IllegalStateException("GitHub App private key is not configured");
        }

        log.info("Generating App JWT for App ID: '{}' (Key Length: {})", appId, rawPrivateKey.length());

        RSAPrivateKey privateKey = parsePrivateKey(rawPrivateKey);
        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        Instant now = Instant.now();
        Instant issuedAt = now.minusSeconds(60); // 60s in the past to guarantee iat is never in the future on GitHub servers
        Instant expiresAt = now.plusSeconds(600); // 10 minutes from now (660s total from issuedAt)

        String jwt = JWT.create()
                .withIssuer(appId)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);

        System.out.println("Generated JWT: " + jwt);
        String[] parts = jwt.split("\\.");
        if (parts.length >= 2) {
            System.out.println("JWT Header:  " + new String(Base64.getDecoder().decode(parts[0])));
            System.out.println("JWT Payload: " + new String(Base64.getDecoder().decode(parts[1])));
        }
        return jwt;
    }

    public String loadPrivateKeyContent() {
        if (githubProperties == null) {
            return null;
        }
        String rawPrivateKey = githubProperties.getPrivateKey();
        if (rawPrivateKey != null && !rawPrivateKey.isBlank()) {
            return unescapeKey(rawPrivateKey);
        }

        String path = githubProperties.getPrivateKeyPath();
        if (path != null && !path.isBlank()) {
            try {
                if (path.startsWith("classpath:")) {
                    String resourcePath = path.substring("classpath:".length());
                    try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                        if (is != null) {
                            return unescapeKey(new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }
                } else {
                    java.nio.file.Path filePath = java.nio.file.Path.of(path);
                    if (java.nio.file.Files.exists(filePath)) {
                        return unescapeKey(java.nio.file.Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                // Fallback to default search paths below
            }
            return null;
        }

        // Fallback search path for container secret file if path was not specified
        if (new java.io.File("/app/secrets/github-app-private-key.pem").exists()) {
            try {
                return unescapeKey(java.nio.file.Files.readString(java.nio.file.Path.of("/app/secrets/github-app-private-key.pem"), java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String unescapeKey(String key) {
        if (key == null) return null;
        return key.replace("\\n", "\n").replace("\\r", "\r");
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            boolean isPkcs1 = pem.contains("RSA PRIVATE KEY");
            String sanitized = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                                  .replace("-----END RSA PRIVATE KEY-----", "")
                                  .replace("-----BEGIN PRIVATE KEY-----", "")
                                  .replace("-----END PRIVATE KEY-----", "")
                                  .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            if (isPkcs1) {
                byte[] pkcs8Bytes = wrapPkcs1ToPkcs8(decoded);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            } else {
                try {
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
                    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
                } catch (InvalidKeySpecException e) {
                    byte[] pkcs8Bytes = wrapPkcs1ToPkcs8(decoded);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RSA private key configuration", e);
        }
    }

    private byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        int pkcs1Length = pkcs1Bytes.length;
        byte[] algorithmIdentifier = new byte[] {
            0x30, 0x0d,
            0x06, 0x09, 0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00
        };

        byte[] octetStringHeader;
        if (pkcs1Length < 128) {
            octetStringHeader = new byte[] { 0x04, (byte) pkcs1Length };
        } else if (pkcs1Length < 256) {
            octetStringHeader = new byte[] { 0x04, (byte) 0x81, (byte) pkcs1Length };
        } else {
            octetStringHeader = new byte[] { 0x04, (byte) 0x82, (byte) (pkcs1Length >> 8), (byte) (pkcs1Length & 0xFF) };
        }

        int innerLength = 3 + algorithmIdentifier.length + octetStringHeader.length + pkcs1Length;
        byte[] pkcs8Header;
        if (innerLength < 128) {
            pkcs8Header = new byte[] { 0x30, (byte) innerLength, 0x02, 0x01, 0x00 };
        } else if (innerLength < 256) {
            pkcs8Header = new byte[] { 0x30, (byte) 0x81, (byte) innerLength, 0x02, 0x01, 0x00 };
        } else {
            pkcs8Header = new byte[] { 0x30, (byte) 0x82, (byte) (innerLength >> 8), (byte) (innerLength & 0xFF), 0x02, 0x01, 0x00 };
        }

        byte[] pkcs8Bytes = new byte[pkcs8Header.length + algorithmIdentifier.length + octetStringHeader.length + pkcs1Bytes.length];
        int pos = 0;
        System.arraycopy(pkcs8Header, 0, pkcs8Bytes, pos, pkcs8Header.length);
        pos += pkcs8Header.length;
        System.arraycopy(algorithmIdentifier, 0, pkcs8Bytes, pos, algorithmIdentifier.length);
        pos += algorithmIdentifier.length;
        System.arraycopy(octetStringHeader, 0, pkcs8Bytes, pos, octetStringHeader.length);
        pos += octetStringHeader.length;
        System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pos, pkcs1Bytes.length);

        return pkcs8Bytes;
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
        return new BigInteger(1, bytes);
    }
}
