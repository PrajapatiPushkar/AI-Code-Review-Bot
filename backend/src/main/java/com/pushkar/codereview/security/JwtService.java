package com.pushkar.codereview.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pushkar.codereview.config.JwtProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(String username) {
        return generateToken(username, null);
    }

    public String generateToken(String username, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username/subject must not be blank for JWT generation");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtProperties.getExpirationMs());

        var builder = JWT.create()
                .withIssuer(jwtProperties.getIssuer())
                .withSubject(username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt));

        if (role != null && !role.isBlank()) {
            builder.withClaim("role", role);
        }

        return builder.sign(Algorithm.HMAC256(jwtProperties.getSecret()));
    }

    public String extractUsername(String token) {
        DecodedJWT decoded = decodeAndVerify(token);
        return decoded != null ? decoded.getSubject() : null;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || token.isBlank() || userDetails == null) {
            return false;
        }

        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isValidToken(String token) {
        return extractUsername(token) != null && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        DecodedJWT decoded = decodeAndVerify(token);
        if (decoded == null || decoded.getExpiresAt() == null) {
            return true;
        }
        return decoded.getExpiresAt().before(new Date());
    }

    private DecodedJWT decodeAndVerify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .withIssuer(jwtProperties.getIssuer())
                    .build()
                    .verify(token);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            return null;
        }
    }
}
