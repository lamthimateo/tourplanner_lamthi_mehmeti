package tour_planner_lamthi_kiri_puka.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_kiri_puka.exception.TourLogNotFoundException;
import tour_planner_lamthi_kiri_puka.exception.TourNotFoundException;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.repository.TourLogRepository;
import tour_planner_lamthi_kiri_puka.repository.TourRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TourLogService {

    private static final Logger logger = LogManager.getLogger(TourLogService.class);

    private final TourLogRepository tourLogRepository;
    private final TourRepository tourRepository;

    @Autowired
    public TourLogService(TourLogRepository tourLogRepository, TourRepository tourRepository) {
        this.tourLogRepository = tourLogRepository;
        this.tourRepository = tourRepository;
    }

    // --- Methods used by TourController ---

    public List<TourLog> getAllTourLogs(Long tourId) {
        logger.info("Fetching all logs for tour ID: {}", tourId);
        return tourLogRepository.findByTourId(tourId);
    }

    public TourLog createTourLog(Long tourId, TourLog log) {
        logger.info("Creating log for tour ID: {}", tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException(tourId));
        log.setTour(tour);
        return tourLogRepository.save(log);
    }

    public TourLog updateTourLog(Long tourId, TourLog log) {
        logger.info("Updating log ID: {} for tour ID: {}", log.getId(), tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException(tourId));
        if (!tourLogRepository.existsById(log.getId())) {
            throw new TourLogNotFoundException(log.getId());
        }
        log.setTour(tour);
        return tourLogRepository.save(log);
    }

    public void deleteTourLog(Long tourId, Long logId) {
        logger.info("Deleting log ID: {} for tour ID: {}", logId, tourId);
        if (!tourLogRepository.existsById(logId)) {
            throw new TourLogNotFoundException(logId);
        }
        tourLogRepository.deleteById(logId);
    }

    // --- Lower-level helpers ---

    public List<TourLog> findByTourId(Long tourId) {
        return tourLogRepository.findByTourId(tourId);
    }

    public TourLog findById(Long id) {
        Optional<TourLog> log = tourLogRepository.findById(id);
        return log.orElse(null);
    }

    public void deleteById(Long id) {
        tourLogRepository.deleteById(id);
    }

    public TourLog saveLog(TourLog tourLog) {
        return tourLogRepository.save(tourLog);
    }

    public TourLog updateLog(TourLog tourLog) {
        return tourLogRepository.save(tourLog);
    }
}
