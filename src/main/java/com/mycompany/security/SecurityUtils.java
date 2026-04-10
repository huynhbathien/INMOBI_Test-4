package com.mycompany.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to retrieve current user information from SecurityContext
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new AssertionError("Cannot instantiate SecurityUtils");
    }

    /**
     * Get current authenticated user
     */
    public static CustomUserDetailsService.CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserDetails) {
            return (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get current user ID
     */
    public static Long getCurrentUserId() {
        CustomUserDetailsService.CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * Get current username
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if user has specific role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
