package tour_planner_lamthi_mehmeti.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Central Spring-Security configuration for the backend.
 *
 * <p>Declares three beans:
 * <ol>
 *   <li>{@link #filterChain(HttpSecurity)} — the HTTP security filter chain:
 *       stateless (no server-side sessions), CSRF disabled (we are a pure
 *       JSON API, not an HTML form site), CORS enabled for the Angular dev
 *       server, and the {@link JwtAuthFilter} wired in so every request
 *       carries an identity.</li>
 *   <li>{@link #corsConfigurationSource()} — allows the Angular app served
 *       from {@code localhost:4200} to call this API from a browser.</li>
 *   <li>{@link #passwordEncoder()} — BCrypt encoder used by {@code AuthService}
 *       to hash passwords before persisting them and to verify on login.</li>
 * </ol>
 *
 * <p>Authorization rules are intentionally simple: {@code /api/auth/**} is
 * open (so clients can log in or register), every other {@code /api/**} path
 * requires a valid JWT, and everything else (actuator, error page, static
 * files) is unrestricted. Unauthorized requests receive a flat
 * {@code 401 Unauthorized} instead of a redirect to a non-existent login page
 * — the correct behaviour for a JSON API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Filter that extracts and verifies the JWT on each request. */
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Defines the full HTTP security filter chain.
     *
     * <p>The order of configuration mirrors the order in which requests are
     * processed: CORS → CSRF (off) → session policy → error mapping →
     * URL-pattern authorization → JWT filter installed <em>before</em> the
     * default {@code UsernamePasswordAuthenticationFilter} so that requests
     * bearing a Bearer token are already authenticated when the built-in
     * filters run.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF protection is irrelevant for a token-authenticated, stateless API.
                .csrf(csrf -> csrf.disable())
                // No HTTP session at all — every request authenticates itself via its JWT.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Return a flat 401 for missing/invalid credentials (no login redirect).
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()   // login / register
                        .requestMatchers("/api/**").authenticated()    // everything else under /api
                        .anyRequest().permitAll()                      // static files, errors, actuator
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Explicit CORS configuration so the Angular dev server (port 4200) can
     * call this API from the browser. Without this the browser's
     * same-origin policy would block all XHRs to port 8081.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Required so the browser sends the Authorization header on cross-origin requests.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * Password encoder bean. BCrypt is intentionally slow (adaptive work
     * factor) which makes brute-force attacks against the stored hashes
     * expensive even if an attacker dumps the database.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
