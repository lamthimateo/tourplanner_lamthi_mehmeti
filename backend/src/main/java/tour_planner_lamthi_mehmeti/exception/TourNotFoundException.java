package tour_planner_lamthi_mehmeti.exception;

/**
 * Thrown when a Tour with the given ID does not exist or does not belong to
 * the currently authenticated user.  Mapped to HTTP 404 by ApiExceptionHandler.
 */
public class TourNotFoundException extends RuntimeException {
    public TourNotFoundException(Long id) {
        super("Tour not found with ID: " + id);
    }
}
