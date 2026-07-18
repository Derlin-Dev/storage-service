package com.backend.storage_service.auth.security;

import com.backend.storage_service.auth.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtServices {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateTokeJwt(UserEntity user){
        Date expirationDate = new Date(System.currentTimeMillis() + Duration.ofMinutes(expirationMinutes).toMillis());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            Date expiration = extractAllClaims(token).getExpiration();
            boolean isUsernameMatch = username.equals(userDetails.getUsername());
            boolean isNotExpired = expiration.after(new Date());
            System.out.println("JWT Validation - Username match: " + isUsernameMatch + ", Not expired: " + isNotExpired + ", Username: " + username);
            return isUsernameMatch && isNotExpired;
        } catch (Exception e) {
            System.out.println("JWT Validation Error: " + e.getMessage());
            return false;
        }
    }
}
