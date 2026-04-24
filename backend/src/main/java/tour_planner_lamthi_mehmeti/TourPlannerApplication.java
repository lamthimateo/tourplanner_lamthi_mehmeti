package tour_planner_lamthi_mehmeti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tour_planner_lamthi_mehmeti.utils.DotenvLoader;

/**
 * Entry point for the Tour Planner Spring Boot application.
 *
 * <p>{@code @SpringBootApplication} is a convenience annotation that combines:
 * <ul>
 *   <li>{@code @Configuration} – marks this class as a source of bean definitions.</li>
 *   <li>{@code @EnableAutoConfiguration} – tells Spring Boot to automatically configure
 *       infrastructure (JPA, Security, web MVC, etc.) based on the classpath.</li>
 *   <li>{@code @ComponentScan} – scans this package and all sub-packages for
 *       Spring-managed components ({@code @Service}, {@code @Repository}, etc.).</li>
 * </ul>
 *
 * <p>On startup the application also ensures that the required file-system directories
 * exist under {@code ~/TourPlanner/} so that later writes to those folders (images,
 * reports, logs) never fail with a "no such directory" error.
 */
@SpringBootApplication
public class TourPlannerApplication {

    /**
     * JVM entry point.  Prepares the environment and launches the Spring context.
     *
     * @param args command-line arguments forwarded to {@link SpringApplication#run}.
     */
    public static void main(String[] args) {
        // Load .env from the working directory into JVM system properties so Spring
        // can resolve ${ORS_API_KEY}, ${JWT_SECRET}, etc. when running from IntelliJ
        // (where run.sh is not executed and real OS environment variables may be absent).
        DotenvLoader.loadFromWorkingDirectoryIfPresent();

        // Proactively create the data directories the application writes to at runtime.
        // Using createDirectories (not createDirectory) so intermediate paths are also
        // created if they don't exist yet. Errors are silently swallowed here because
        // the same creation is attempted again on first use, where failures are logged.
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), "TourPlanner", "logs"));
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), "TourPlanner", "images"));
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), "TourPlanner", "reports"));
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), "TourPlanner", "db"));
        } catch (Exception ignored) {
            // If directory creation fails here we still attempt to start; individual
            // service methods will fail loudly with an IOException if they truly can't
            // write to the required location.
        }

        // Hand control to Spring Boot, which wires up all beans and starts the
        // embedded Tomcat server (default port 8081 as configured in application.properties).
        SpringApplication.run(TourPlannerApplication.class, args);
    }
}
