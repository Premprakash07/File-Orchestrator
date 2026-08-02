package com.filestorage.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LogManager.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        // Decode the base64-encoded secret or use the string directly if it's long
        // enough
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            // If not base64, use UTF-8 bytes directly
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        // Ensure the key is at least 512 bits (64 bytes) for HS512
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "The JWT signing key must be at least 512 bits (64 bytes) for HS512. " +
                            "Current key size: " + (keyBytes.length * 8) + " bits. " +
                            "Use Keys.secretKeyFor(SignatureAlgorithm.HS512) to generate a secure key.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        logger.debug("Generating JWT token for user: {}", userPrincipal.getUsername());

        String token = Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        logger.debug("JWT token generated successfully for user: {}", userPrincipal.getUsername());
        return token;
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            logger.debug("JWT token validated successfully");
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: Malformed token");
        } catch (ExpiredJwtException e) {
            logger.error("Invalid JWT token: Token expired");
        } catch (UnsupportedJwtException e) {
            logger.error("Invalid JWT token: Unsupported token");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid JWT token: Empty claims string");
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}
