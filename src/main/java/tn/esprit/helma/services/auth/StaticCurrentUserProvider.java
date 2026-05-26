package tn.esprit.helma.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tn.esprit.helma.repositories.UserRepository;

@Component
public class StaticCurrentUserProvider implements CurrentUserProvider {

    private final Long mockCurrentUserId;
    private final HttpServletRequest request;
    private final UserRepository userRepository;

    public StaticCurrentUserProvider(
            @Value("${helma.security.mock-current-user-id:3}") Long mockCurrentUserId,
            HttpServletRequest request,
            UserRepository userRepository) {
        this.mockCurrentUserId = mockCurrentUserId;
        this.request = request;
        this.userRepository = userRepository;
    }

    @Override
    public Long getCurrentUserId() {
        // Priorité 1 : utilisateur connecté via JWT
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
            return userRepository.findByEmailIgnoreCase(ud.getUsername())
                    .map(u -> u.getId())
                    .orElse(mockCurrentUserId);
        }

        // Priorité 2 : header X-User-Id pour les tests Postman/Swagger
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return Long.parseLong(headerUserId.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Fallback : utilisateur mock configuré
        return mockCurrentUserId;
    }
}
