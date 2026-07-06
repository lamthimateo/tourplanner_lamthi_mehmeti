package tour_planner_lamthi_mehmeti.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the logged-in user from Spring SecurityContext.
 * JwtAuthFilter stores userId in credentials and username in principal.
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
