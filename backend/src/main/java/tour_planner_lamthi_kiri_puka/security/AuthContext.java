package tour_planner_lamthi_kiri_puka.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract the current authenticated user's ID from the security context.
 */
public final class AuthContext {

    private AuthContext() {
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return (Long) auth.getCredentials();
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return (String) auth.getPrincipal();
    }
}
