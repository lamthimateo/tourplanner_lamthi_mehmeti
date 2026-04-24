package tour_planner_lamthi_kiri_puka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for Spring Boot application shutdown and logs the exit code.
 *
 * <p>Registered as a Spring component so it is automatically picked up by the
 * application context. Handles {@link ExitCodeEvent}, which is published by
 * Spring Boot just before the JVM exits — useful for monitoring and debugging
 * unexpected shutdowns in production or during grading.
 *
 * <p>Exit code semantics (UNIX convention):
 * <ul>
 *   <li>0 — clean, intentional shutdown (e.g. SIGTERM or actuator /shutdown).</li>
 *   <li>Non-zero — error or unexpected termination that may require investigation.</li>
 * </ul>
 */
@Component
public class ApplicationExitListener {

    /** Logger for recording the shutdown event. Uses SLF4J so the actual logging
     *  backend (Log4j2, Logback, etc.) can be swapped without code changes. */
    private static final Logger logger = LoggerFactory.getLogger(ApplicationExitListener.class);

    /**
     * Called automatically by Spring's event infrastructure when the application is
     * shutting down.
     *
     * <p>The {@code @EventListener} annotation registers this method as a handler for
     * {@link ExitCodeEvent} without any XML or manual bean wiring.
     *
     * @param event the shutdown event emitted by Spring Boot; contains the numeric
     *              exit code that will be returned to the operating system.
     */
    @EventListener
    public void onApplicationEvent(ExitCodeEvent event) {
        // Log the exit code so operators can quickly see whether the shutdown was clean.
        logger.info("Application is exiting with exit code: {}", event.getExitCode());
    }
}
