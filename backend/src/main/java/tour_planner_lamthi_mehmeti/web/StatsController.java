package tour_planner_lamthi_mehmeti.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_mehmeti.dto.StatsDto;
import tour_planner_lamthi_mehmeti.service.StatsService;

/**
 * GET /api/stats — dashboard numbers for the current user.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsDto getStats() {
        return statsService.getStats();
    }
}
