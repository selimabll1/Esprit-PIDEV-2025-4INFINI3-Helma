package tn.esprit.helma.services;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Petit service utilitaire pour obtenir l'utilisateur courant depuis le SecurityContext.
 * Retourne un Optional pour faciliter le fallback en environnement de dev.
 */
@Service
public class CurrentUserService {

    public Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return Optional.ofNullable(username);
        }
        if (principal instanceof String) {
            return Optional.of((String) principal);
        }
        return Optional.empty();
    }
}
