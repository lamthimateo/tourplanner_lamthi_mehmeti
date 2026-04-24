package tour_planner_lamthi_mehmeti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that inspects every incoming HTTP request, extracts a JWT
 * from the {@code Authorization: Bearer ...} header (if present), validates
 * it, and — on success — populates Spring's {@link SecurityContextHolder}
 * with the authenticated principal.
 *
 * <p>Architectural role: this is the concrete realization of the
 * <b>Filter / Chain-of-Responsibility pattern</b> required by the project
 * specification. Each request walks through Spring's filter chain exactly
 * once (we extend {@link OncePerRequestFilter} to guarantee that), and this
 * filter is wired in <i>before</i> the built-in
 * {@code UsernamePasswordAuthenticationFilter} so that downstream code —
 * including {@code AuthContext.getCurrentUserId()} — can rely on the
 * principal already being set.
 *
 * <p>Three branches:
 * <ul>
 *   <li><b>No header</b> — the request continues; Spring Security will later
 *       decide if that's acceptable based on the URL rules in
 *       {@code SecurityConfig} (only {@code /api/auth/**} is public).</li>
 *   <li><b>Valid header</b> — we set the authentication and continue.</li>
 *   <li><b>Invalid/expired header</b> — we abort with a flat 401; the
 *       client must fetch a new token before retrying.</li>
 * </ul>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Helper that knows how to parse and validate our tokens. */
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Core filter logic — runs exactly once per request. See the class
     * Javadoc for an overview of the branching behaviour.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // Only consider requests that actually present a bearer token. Requests
        // without one fall through and are handled by SecurityConfig (either
        // allowed because the URL is public, or rejected with 401).
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // strip the "Bearer " prefix
            if (jwtUtil.isValid(token)) {
                // Token ok - hydrate Spring's SecurityContext so the rest of
                // the request sees an authenticated principal.
                String username = jwtUtil.getUsername(token);
                Long userId = jwtUtil.getUserId(token);

                // Principal = username; credentials = userId (we stuff the
                // primary key in there so AuthContext can read it back without
                // another JWT parse). Empty authorities because this app has
                // only one user-role.
                var auth = new UsernamePasswordAuthenticationToken(username, userId, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Token was provided but is invalid/expired -> fail fast with 401.
                // We don't continue the chain because letting the request through
                // would silently treat it as anonymous, which is surprising.
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // Hand the request off to the next filter / controller.
        filterChain.doFilter(request, response);
    }
}
