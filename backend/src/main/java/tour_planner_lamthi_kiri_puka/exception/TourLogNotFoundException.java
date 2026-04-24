package tour_planner_lamthi_kiri_puka.exception;

/**
 * Thrown when a TourLog with the given ID does not exist or does not belong to
 * the requested tour.  Mapped to HTTP 404 by ApiExceptionHandler.
 */
public class TourLogNotFoundException extends RuntimeException {
    public TourLogNotFoundException(Long id) {
        super("TourLog not found with ID: " + id);
    }
}
