package tour_planner_lamthi_mehmeti;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Logs the exit code when the app shuts down. */
@Component
public class ApplicationExitListener {

    private static final Logger logger = LogManager.getLogger(ApplicationExitListener.class);

    @EventListener
    public void onApplicationEvent(ExitCodeEvent event) {
        logger.info("Application is exiting with exit code: {}", event.getExitCode());
    }
}
