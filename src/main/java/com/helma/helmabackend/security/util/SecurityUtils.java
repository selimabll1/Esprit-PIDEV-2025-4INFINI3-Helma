package com.helma.helmabackend.security.util;

import com.helma.helmabackend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        return auth.getName(); // Spring Security stores username/email here
    }

    public static String currentRoleOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse(null);
    }
}
