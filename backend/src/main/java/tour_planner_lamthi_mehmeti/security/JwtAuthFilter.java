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
 * Reads the Bearer JWT from each request and sets the Spring Security context.
 * No header → anonymous; bad token → 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

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
