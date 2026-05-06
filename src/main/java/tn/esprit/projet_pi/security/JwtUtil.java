package tn.esprit.projet_pi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * User-Backend émet claim "role" (string, sans préfixe ROLE_).
     * Ex : "YOUTH_BENEFICIARY", "ADMIN", "INVESTOR", "COMPLIANCE"
     */
    public List<String> extractRoles(String token) {
        Claims claims = getClaims(token);
        Object role = claims.get("role");
        if (role != null && !role.toString().isBlank()) {
            return List.of(normalizeAuthority(role.toString()));
        }
        return List.of();
    }

    public Long extractUserId(String token) {
        Object userId = getClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value && !value.isBlank()) {
            return Long.parseLong(value);
        }
        return null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // User-Backend signe avec Keys.hmacShaKeyFor(secret.getBytes(UTF_8))
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Normalise un rôle vers le format Spring Security attendu ("ROLE_XXX").
     * User-Backend envoie les rôles sans préfixe ("YOUTH_BENEFICIARY"),
     * cette méthode ajoute "ROLE_" pour que hasAuthority() fonctionne.
     */
    private String normalizeAuthority(String rawRole) {
        String normalized = rawRole == null ? "" : rawRole.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return "ROLE_" + normalized;
    }
}
