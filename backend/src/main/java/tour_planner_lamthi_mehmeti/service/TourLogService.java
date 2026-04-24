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
import java.util.Optional;

/**
 * Business-logic service for {@link TourLog} entities.
 *
 * <p>Every mutating operation is scoped to the current authenticated user: the
 * parent {@link Tour} is resolved via {@link TourRepository#findByIdAndUserId}
 * so that one user can never read, create, update or delete logs on another
 * user's tour. Listing logs is also protected by the same ownership check.
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

    // -------------------------------------------------------------------------
    // Public API (user-scoped)
    // -------------------------------------------------------------------------

    /**
     * Returns all logs for a tour the current user owns.
     *
     * @param tourId parent tour ID
     * @return list of logs, possibly empty; never {@code null}
     * @throws TourNotFoundException if the tour does not exist or belongs to another user
     */
    public List<TourLog> getAllTourLogs(Long tourId) {
        logger.info("Fetching all logs for tour ID: {}", tourId);
        requireOwnedTour(tourId);
        return tourLogRepository.findByTourId(tourId);
    }

    /**
     * Creates a new log under a tour owned by the current user.
     *
     * @param tourId the parent tour
     * @param log    the log to persist (its parent tour reference is set automatically)
     * @return the saved log
     * @throws TourNotFoundException if the tour does not exist or belongs to another user
     */
    public TourLog createTourLog(Long tourId, TourLog log) {
        logger.info("Creating log for tour ID: {}", tourId);
        Tour tour = requireOwnedTour(tourId);
        log.setTour(tour);
        return tourLogRepository.save(log);
    }

    /**
     * Updates an existing log on a tour owned by the current user.
     * The log must actually belong to the given tour (so users can't retarget a
     * log into a different tour via a crafted request).
     */
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

    /**
     * Deletes a log on a tour owned by the current user. The log must belong to
     * that tour.
     */
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

    // -------------------------------------------------------------------------
    // Lower-level helpers (no user check) — use only when ownership was
    // already verified by the caller.
    // -------------------------------------------------------------------------

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

    /**
     * Resolves the tour with the given ID scoped to the authenticated user or
     * throws {@link TourNotFoundException}. Centralising the check here means
     * every public method is protected by exactly one line at its entry.
     */
    private Tour requireOwnedTour(Long tourId) {
        Long userId = AuthContext.getCurrentUserId();
        return tourRepository.findByIdAndUserId(tourId, userId)
                .orElseThrow(() -> new TourNotFoundException(tourId));
    }
}
