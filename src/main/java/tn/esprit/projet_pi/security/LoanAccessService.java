package tn.esprit.projet_pi.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.Loan;

import java.util.Map;

@Service
public class LoanAccessService {

    public void assertCanAccessLoan(Loan loan) {
        if (hasAnyRole("ROLE_ADMIN", "ROLE_COMPLIANCE")) {
            return;
        }
        Long currentUserId = currentUserId();
        if (currentUserId == null || !currentUserId.equals(loan.getUserId())) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres prets.");
        }
    }

    public void assertCanAccessUser(Long userId) {
        if (hasAnyRole("ROLE_ADMIN", "ROLE_COMPLIANCE")) {
            return;
        }
        Long currentUserId = currentUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres donnees.");
        }
    }

    public void assertCanCreateForUser(Long userId) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Un utilisateur Youth ne peut creer une demande que pour lui-meme.");
        }
    }

    private boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (String role : roles) {
            boolean match = authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (match) {
                return true;
            }
        }
        return false;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof Map<?, ?> details)) {
            return null;
        }
        Object userId = details.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
