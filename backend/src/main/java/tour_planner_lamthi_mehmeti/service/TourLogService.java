package tour_planner_lamthi_mehmeti.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.exception.TourLogNotFoundException;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;
import tour_planner_lamthi_mehmeti.security.AuthContext;

import java.util.List;

/**
 * CRUD for tour logs. All operations check that the parent tour belongs to the current user.
 */
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

    public List<TourLog> getAllTourLogs(Long tourId) {
        logger.info("Fetching all logs for tour ID: {}", tourId);
        requireOwnedTour(tourId);
        return tourLogRepository.findByTourId(tourId);
    }

    public TourLog createTourLog(Long tourId, TourLog log) {
        logger.info("Creating log for tour ID: {}", tourId);
        Tour tour = requireOwnedTour(tourId);
        log.setTour(tour);
        return tourLogRepository.save(log);
    }

    // Also verify the log actually belongs to this tour (not just any log by ID).
    public TourLog updateTourLog(Long tourId, TourLog log) {
        logger.info("Updating log ID: {} for tour ID: {}", log.getId(), tourId);
        Tour tour = requireOwnedTour(tourId);
        TourLog existing = tourLogRepository.findById(log.getId())
                .orElseThrow(() -> new TourLogNotFoundException(log.getId()));
        if (existing.getTour() == null || !tourId.equals(existing.getTour().getId())) {
            throw new TourLogNotFoundException(log.getId());
        }
        log.setTour(tour);
        return tourLogRepository.save(log);
    }

    public void deleteTourLog(Long tourId, Long logId) {
        logger.info("Deleting log ID: {} for tour ID: {}", logId, tourId);
        requireOwnedTour(tourId);
        TourLog existing = tourLogRepository.findById(logId)
                .orElseThrow(() -> new TourLogNotFoundException(logId));
        if (existing.getTour() == null || !tourId.equals(existing.getTour().getId())) {
            throw new TourLogNotFoundException(logId);
        }
        tourLogRepository.deleteById(logId);
    }

    /** Shared ownership check used by every public method. */
    private Tour requireOwnedTour(Long tourId) {
        Long userId = AuthContext.getCurrentUserId();
        return tourRepository.findByIdAndUserId(tourId, userId)
                .orElseThrow(() -> new TourNotFoundException(tourId));
    }
}
