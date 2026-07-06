package tour_planner_lamthi_mehmeti.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.dto.StatsDto;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;
import tour_planner_lamthi_mehmeti.security.AuthContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds dashboard stats for the current user (totals + breakdown by transport type).
 */
@Service
public class StatsService {

    private static final Logger logger = LogManager.getLogger(StatsService.class);

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;

    public StatsService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
    }

    public StatsDto getStats() {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Computing stats for user {}", userId);

        // Fetch once and reuse — the user's tours form the scope of everything below.
        List<Tour> tours = tourRepository.findByUserId(userId);

        // Flatten all logs of all tours into a single collection so we can
        // stream-aggregate totals without nested loops.
        List<TourLog> allLogs = tours.stream()
                .flatMap(t -> tourLogRepository.findByTourId(t.getId()).stream())
                .collect(Collectors.toList());

        StatsDto dto = new StatsDto();
        dto.setTotalTours(tours.size());
        dto.setTotalLogs(allLogs.size());

        // ── Totals ───────────────────────────────────────────────────────────
        // Round to 2 decimals before handing to the UI; the math is elementary
        // (multiply by 100, round, divide) but repeating it inline makes the
        // intent obvious on every metric.
        double totalDist = allLogs.stream()
                .filter(l -> l.getTotalDistance() != null)
                .mapToDouble(TourLog::getTotalDistance)
                .sum();
        dto.setTotalDistanceKm(Math.round(totalDist * 100.0) / 100.0);

        double totalMinutes = allLogs.stream()
                .filter(l -> l.getTotalTimeMinutes() != null)
                .mapToInt(TourLog::getTotalTimeMinutes)
                .sum();
        dto.setTotalTimeHours(Math.round((totalMinutes / 60.0) * 100.0) / 100.0);

        double avgRating = allLogs.stream()
                .filter(l -> l.getRating() != null)
                .mapToInt(TourLog::getRating)
                .average()
                .orElse(0.0);
        dto.setAvgRating(Math.round(avgRating * 10.0) / 10.0);

        // ── Per-transport-type breakdown ────────────────────────────────────
        // Group tours by transport-type, then for each bucket recompute the
        // log aggregates. Unknown / null transport types are bucketed under
        // "unknown" so the UI can still display them.
        Map<String, List<Tour>> byType = tours.stream()
                .collect(Collectors.groupingBy(t -> t.getTransportType() == null ? "unknown" : t.getTransportType()));

        List<StatsDto.TransportStat> transportStats = byType.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    List<Tour> typeTours = entry.getValue();

                    List<TourLog> typeLogs = typeTours.stream()
                            .flatMap(t -> tourLogRepository.findByTourId(t.getId()).stream())
                            .collect(Collectors.toList());

                    double avgDist = typeLogs.stream()
                            .filter(l -> l.getTotalDistance() != null)
                            .mapToDouble(TourLog::getTotalDistance)
                            .average().orElse(0.0);

                    double avgR = typeLogs.stream()
                            .filter(l -> l.getRating() != null)
                            .mapToInt(TourLog::getRating)
                            .average().orElse(0.0);

                    return new StatsDto.TransportStat(
                            type,
                            typeTours.size(),
                            typeLogs.size(),
                            Math.round(avgDist * 100.0) / 100.0,
                            Math.round(avgR * 10.0) / 10.0
                    );
                })
                // Busiest transport type first — makes the horizontal bar chart
                // visually consistent across users.
                .sorted(Comparator.comparing(StatsDto.TransportStat::getTourCount).reversed())
                .collect(Collectors.toList());

        dto.setByTransportType(transportStats);
        return dto;
    }
}
