package tour_planner_lamthi_mehmeti.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static helper that reads the authenticated user out of Spring's
 * {@link SecurityContextHolder}.
 *
 * <p>This is the single point in the codebase where service-layer code asks
 * "who is calling me?". Concentrating the lookup here keeps
 * {@code TourService}, {@code TourLogService} and {@code TourDataTransferService}
 * blissfully unaware of servlet filters and JWTs.
 *
 * <p>Why userId is read from {@code credentials} and username from
 * {@code principal}: the {@link JwtAuthFilter} sets up
 * {@code new UsernamePasswordAuthenticationToken(username, userId, ...)}
 * so Spring stores the two values in exactly those slots.
 *
 * <p>The class is {@code final} with a private constructor because it's a
 * utility class — instantiating it would be meaningless and wasteful.
 */
public final class AuthContext {

    /** Utility class — no instances. */
    private AuthContext() {
    }

    /**
     * @return the primary key of the currently authenticated user
     * @throws IllegalStateException when no user is authenticated (defensive: this should
     *         never happen on protected endpoints because {@code SecurityConfig} rejects
     *         unauthenticated requests earlier, but we guard against misuse from
     *         unprotected tests or scheduled jobs)
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return (Long) auth.getCredentials();
    }

    /**
     * @return the username (login name) of the current user
     * @throws IllegalStateException when no user is authenticated
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return (String) auth.getPrincipal();
    }
}
