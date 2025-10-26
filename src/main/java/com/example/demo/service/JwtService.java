package com.example.demo.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    private static final String SECRET_KEY = "your-256-bit-secret-your-256-bit-secret"; // 32+ chars

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    // New method: Validate token
    public boolean isTokenValid(String token, String username) {
        try {
            // Extract username from token
            String tokenUsername = extractUsername(token);

            // Check if username matches and token is not expired
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            // If any exception occurs (malformed, signature invalid, etc.), token is invalid
            return false;
        }
    }

    // Helper method: Check if token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Helper method: Extract expiration date from token
    private Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
}
