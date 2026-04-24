package tour_planner_lamthi_mehmeti.config;

import org.springframework.context.annotation.Configuration;

/**
 * Intentionally empty Spring {@code @Configuration} class.
 *
 * <p>Earlier in the project's history CORS was configured here as a separate
 * concern. When we added Spring Security ({@code SecurityConfig}) the CORS
 * configuration had to move into the security filter chain (otherwise
 * Spring Security's own CORS handling would run first and the old rules
 * would be silently ignored).
 *
 * <p>This class is kept as a stub so that:
 * <ul>
 *   <li>the {@code config} package is not empty (clean sub-package for
 *       cross-cutting concerns to live in later), and</li>
 *   <li>historical commits referring to {@code CorsConfig} still resolve.</li>
 * </ul>
 *
 * <p>The real CORS policy is defined in
 * {@link tour_planner_lamthi_mehmeti.security.SecurityConfig#corsConfigurationSource()}.
 */
@Configuration
public class CorsConfig {
    // The actual CORS configuration lives in SecurityConfig.
}
