package tour_planner_lamthi_mehmeti.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_mehmeti.dto.StatsDto;
import tour_planner_lamthi_mehmeti.service.StatsService;

/**
 * REST controller for the <b>Statistics Dashboard</b> — the "unique feature"
 * of this project required by the specification.
 *
 * <p>Single endpoint {@code GET /api/stats} returns a {@link StatsDto} with:
 * aggregated totals (tours, logs, average distance, etc.), a breakdown by
 * transport type, and a "top tours" list. The controller simply delegates to
 * {@link StatsService} where the aggregation logic lives.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Full statistics snapshot scoped to the current user. */
    @GetMapping
    public StatsDto getStats() {
        return statsService.getStats();
    }
}
