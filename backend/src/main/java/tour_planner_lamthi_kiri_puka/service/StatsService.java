package tour_planner_lamthi_kiri_puka.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_kiri_puka.dto.StatsDto;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.repository.TourLogRepository;
import tour_planner_lamthi_kiri_puka.repository.TourRepository;
import tour_planner_lamthi_kiri_puka.security.AuthContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unique Feature: per-user tour statistics aggregated by transport type.
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

        List<Tour> tours = tourRepository.findByUserId(userId);

        List<TourLog> allLogs = tours.stream()
                .flatMap(t -> tourLogRepository.findByTourId(t.getId()).stream())
                .collect(Collectors.toList());

        StatsDto dto = new StatsDto();
        dto.setTotalTours(tours.size());
        dto.setTotalLogs(allLogs.size());

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

        // Group by transport type
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
                .sorted(Comparator.comparing(StatsDto.TransportStat::getTourCount).reversed())
                .collect(Collectors.toList());

        dto.setByTransportType(transportStats);
        return dto;
    }
}
