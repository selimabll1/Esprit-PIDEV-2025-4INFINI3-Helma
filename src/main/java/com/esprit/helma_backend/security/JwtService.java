package com.esprit.helma_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // put this in application.properties ideally (later)
    private static final String SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_32CHARS_MIN!!!!";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateToken(String subject, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)               // email
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 1000 * 60 * 60 * 6)) // 6 hours
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
