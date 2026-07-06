package tour_planner_lamthi_mehmeti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tour_planner_lamthi_mehmeti.utils.DotenvLoader;

/**
 * Spring Boot entry point. Loads .env before starting so IntelliJ runs work too.
 */
@SpringBootApplication
public class TourPlannerApplication {

    public static void main(String[] args) {
        // IntelliJ doesn't run run.sh — load .env into system properties here.
        DotenvLoader.loadFromWorkingDirectoryIfPresent();

        SpringApplication.run(TourPlannerApplication.class, args);
    }
}
