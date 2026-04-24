package tour_planner_lamthi_kiri_puka.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_kiri_puka.dto.StatsDto;
import tour_planner_lamthi_kiri_puka.service.StatsService;

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
