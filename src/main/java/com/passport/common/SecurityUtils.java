package com.passport.common;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getPrincipal() instanceof UUID uuid) {
                return uuid;
            }
            if (auth.getPrincipal() instanceof String str) {
                try {
                    return UUID.fromString(str);
                } catch (IllegalArgumentException ignored) {}
            }
            if (auth.getPrincipal() instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    return UUID.nameUUIDFromBytes(subject.getBytes());
                }
            }
        }
        throw new AccessDeniedException("Access denied: Operation requires an authenticated user session.");
    }

    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("email");
            }
            if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        }
        return "anonymous@passport.internal";
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String formatted = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equalsIgnoreCase(formatted)) {
                return true;
            }
        }
        return false;
    }
}
