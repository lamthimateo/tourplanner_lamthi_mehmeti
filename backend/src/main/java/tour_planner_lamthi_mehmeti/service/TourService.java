package tour_planner_lamthi_mehmeti.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.security.AuthContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business-logic service for {@link Tour} entities.
 *
 * <p>All public methods enforce <b>user-scoping</b>: they read the currently
 * authenticated user's ID from {@link AuthContext} and filter/guard every query
 * against that ID so that users can never access or modify each other's tours.
 *
 * <p>In addition to standard CRUD this service provides:
 * <ul>
 *   <li>{@link #searchTourIds(String)} — full-text search across tour fields and
 *       all associated log fields, including computed attributes.</li>
 *   <li>{@link #computePopularity(Long)} — derives how popular a tour is from
 *       its log count.</li>
 *   <li>{@link #computeChildFriendliness(Long)} — derives a 1–10 score from
 *       average difficulty, distance, and time across all logs.</li>
 * </ul>
 */
@Service
public class TourService {

    private static final Logger logger = LogManager.getLogger(TourService.class);

    /** Repository for Tour persistence operations. */
    @Autowired
    private TourRepository tourRepository;

    /** Repository used to load logs when computing search haystacks and derived metrics. */
    @Autowired
    private TourLogRepository tourLogRepository;

    // -------------------------------------------------------------------------
    // CRUD methods (all user-scoped via AuthContext)
    // -------------------------------------------------------------------------

    /**
     * Returns all tours owned by the currently authenticated user.
     *
     * @return list of tours belonging to the current user; may be empty, never null
     */
    public List<Tour> getAllTours() {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Fetching all tours for user {}", userId);
        return tourRepository.findByUserId(userId);
    }

    /**
     * Returns a single tour by ID, verifying it belongs to the current user.
     *
     * @param id the tour's primary key
     * @return the matching {@link Tour}
     * @throws TourNotFoundException if no tour with that ID exists for the current user
     */
    public Tour getTourById(Long id) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Fetching tour {} for user {}", id, userId);
        // findByIdAndUserId returns empty if the ID exists but belongs to another user —
        // this prevents unauthorised read access.
        return tourRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TourNotFoundException(id));
    }

    /**
     * Persists a new tour for the currently authenticated user.
     *
     * @param tour the tour to save; the {@code userId} field will be overwritten
     *             with the current user's ID to prevent spoofing
     * @return the saved tour with its server-assigned {@code id}
     */
    public Tour createTour(Tour tour) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Creating tour '{}' for user {}", tour.getName(), userId);
        // Always stamp the owner ID from the JWT rather than trusting the request body
        tour.setUserId(userId);
        return tourRepository.save(tour);
    }

    /**
     * Updates an existing tour, verifying it belongs to the current user.
     *
     * @param tour the tour with updated fields; {@code tour.getId()} identifies
     *             which record to replace
     * @return the updated and persisted tour
     * @throws TourNotFoundException if the tour does not exist or belongs to another user
     */
    public Tour updateTour(Tour tour) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Updating tour {} for user {}", tour.getId(), userId);
        // Verify ownership before overwriting — prevents one user from editing another's tour
        if (!tourRepository.existsByIdAndUserId(tour.getId(), userId)) {
            throw new TourNotFoundException(tour.getId());
        }
        // Re-stamp the owner ID to guard against a client sending a forged userId
        tour.setUserId(userId);
        return tourRepository.save(tour);
    }

    /**
     * Deletes a tour and (via cascading) all associated logs,
     * verifying the tour belongs to the current user.
     *
     * @param id the primary key of the tour to delete
     * @throws TourNotFoundException if the tour does not exist or belongs to another user
     */
    public void deleteTour(Long id) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Deleting tour {} for user {}", id, userId);
        // Ownership check: 404 is safer than 403 here — avoids leaking that a tour ID exists
        if (!tourRepository.existsByIdAndUserId(id, userId)) {
            throw new TourNotFoundException(id);
        }
        tourRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Lower-level helpers (used by services that already verified ownership)
    // -------------------------------------------------------------------------

    /**
     * Convenience alias for {@link #getAllTours()}.
     *
     * @return all tours for the current user
     */
    public List<Tour> findAll() {
        return getAllTours();
    }

    /**
     * Looks up a tour by ID for the current user without throwing on miss.
     *
     * @param id the tour's primary key
     * @return an {@link Optional} containing the tour, or empty if not found / wrong user
     */
    public Optional<Tour> findById(Long id) {
        Long userId = AuthContext.getCurrentUserId();
        return tourRepository.findByIdAndUserId(id, userId);
    }

    /**
     * Raw save — skips the ownership and user-stamping logic.
     * Use only when the caller has already verified ownership (e.g. import flow).
     *
     * @param tour the tour to save or update
     * @return the persisted tour
     */
    public Tour save(Tour tour) {
        return tourRepository.save(tour);
    }

    /**
     * Raw delete by ID — skips ownership verification.
     * Use only in contexts where ownership has already been confirmed.
     *
     * @param id the primary key of the tour to delete
     */
    public void deleteById(Long id) {
        tourRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Full-text search
    // -------------------------------------------------------------------------

    /**
     * Performs a full-text search across all tour fields and associated log data
     * for the current user, returning the IDs of matching tours.
     *
     * <p>The search haystack for each tour includes:
     * <ul>
     *   <li>Tour fields: name, description, origin, destination, transportType.</li>
     *   <li>Computed numeric attributes: popularity, average distance, average rating,
     *       average time, and child-friendliness (so queries like "popular" or
     *       "child friendly" work).</li>
     *   <li>Log fields: comment, details, difficulty, distance, time, rating, date.</li>
     * </ul>
     *
     * @param query the search string; case-insensitive; empty query returns all tour IDs
     * @return list of tour IDs whose haystack contains the query as a substring
     */
    public List<Long> searchTourIds(String query) {
        // Normalise: null or blank query means "return everything"
        String q = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Tour> tours = getAllTours();

        if (q.isEmpty()) return tours.stream().map(Tour::getId).collect(Collectors.toList());

        // Load all logs that belong to the current user's tours in one query,
        // then group them by tour ID for O(1) lookup per tour.
        Map<Long, List<TourLog>> logsByTour = tourLogRepository.findAll()
                .stream()
                .filter(l -> l.getTour() != null && tours.stream().anyMatch(t -> t.getId().equals(l.getTour().getId())))
                .collect(Collectors.groupingBy(l -> l.getTour().getId()));

        List<Long> matches = new ArrayList<>();
        for (Tour t : tours) {
            // Build a flat, space-separated string containing all searchable text for this tour
            String haystack = buildSearchHaystack(t, logsByTour.getOrDefault(t.getId(), List.of()));
            // Simple substring match — fast enough for typical tour counts per user
            if (haystack.contains(q)) {
                matches.add(t.getId());
            }
        }
        return matches;
    }

    // -------------------------------------------------------------------------
    // Derived metric computation
    // -------------------------------------------------------------------------

    /**
     * Computes the popularity of a tour as the total number of log entries.
     * More log entries indicate the tour has been completed more often.
     *
     * @param tourId the tour's primary key
     * @return the number of logs attached to this tour (0 if none)
     */
    public int computePopularity(Long tourId) {
        return tourLogRepository.findByTourId(tourId).size();
    }

    /**
     * Computes a child-friendliness score in the range [1.0, 10.0] for a tour,
     * based on the aggregated log data.
     *
     * <p>Formula (all inputs normalised to [0, 1] before weighting):
     * <pre>
     *   score = 10 − (normalisedDifficulty × 4
     *              + normalisedDistance   × 3
     *              + normalisedTime       × 3)
     * </pre>
     * Normalisation thresholds: difficulty/5, distance/50 km, time/300 min.
     * A score close to 10 means easy, short, and quick — ideal for children.
     *
     * @param tourId the tour's primary key
     * @return the computed score in [1.0, 10.0], or 0.0 if the tour has no logs
     */
    public double computeChildFriendliness(Long tourId) {
        List<TourLog> logs = tourLogRepository.findByTourId(tourId);
        // Without any logs there is no data to compute from
        if (logs.isEmpty()) return 0.0;

        // Compute averages, falling back to neutral mid-range values when a field is null
        double avgDifficulty = logs.stream().filter(l -> l.getDifficulty() != null).mapToInt(TourLog::getDifficulty).average().orElse(3);
        double avgDistance   = logs.stream().filter(l -> l.getTotalDistance() != null).mapToDouble(TourLog::getTotalDistance).average().orElse(10);
        double avgTime       = logs.stream().filter(l -> l.getTotalTimeMinutes() != null).mapToInt(TourLog::getTotalTimeMinutes).average().orElse(60);

        // Normalise each dimension to [0, 1] and clamp at 1 so extreme outliers don't
        // push the score below 1.
        double normDiff = Math.min(avgDifficulty / 5.0, 1.0);
        double normDist = Math.min(avgDistance / 50.0, 1.0);
        double normTime = Math.min(avgTime / 300.0, 1.0);

        // Weighted penalty: difficulty has the largest weight (4) because it correlates
        // most directly with suitability for children.
        double score = 10.0 - (normDiff * 4 + normDist * 3 + normTime * 3);
        // Clamp result to [1.0, 10.0] and round to one decimal place
        return Math.max(1.0, Math.min(10.0, Math.round(score * 10.0) / 10.0));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a single lowercase string containing all searchable text for a tour
     * and its logs. The string is used for substring matching in
     * {@link #searchTourIds(String)}.
     *
     * <p>Computed attributes (popularity, average distance/rating/time,
     * child-friendliness) are appended as labelled tokens so queries like
     * {@code "popularity 5"} or {@code "childfriendliness 8"} also work.
     *
     * @param t    the tour whose fields to include
     * @param logs all log entries associated with the tour
     * @return a lowercase, space-separated haystack string
     */
    private static String buildSearchHaystack(Tour t, List<TourLog> logs) {
        StringBuilder sb = new StringBuilder();
        // Append the core tour text fields
        add(sb, t.getName());
        add(sb, t.getDescription());
        add(sb, t.getOrigin());
        add(sb, t.getDestination());
        add(sb, t.getTransportType());

        // Append computed/derived numeric attributes with labels so they are searchable
        int count = logs.size();
        sb.append(" popularity ").append(count);

        Double avgDistance = logs.stream().filter(l -> l.getTotalDistance() != null).mapToDouble(TourLog::getTotalDistance).average().orElse(0);
        Double avgRating   = logs.stream().filter(l -> l.getRating() != null).mapToInt(TourLog::getRating).average().orElse(0);
        Double avgTime     = logs.stream().filter(l -> l.getTotalTimeMinutes() != null).mapToInt(TourLog::getTotalTimeMinutes).average().orElse(0);

        sb.append(" avgdistance ").append(avgDistance);
        sb.append(" avgrating ").append(avgRating);
        sb.append(" avgtime ").append(avgTime);

        // Also compute and append the child-friendliness score so it is searchable
        if (!logs.isEmpty()) {
            double avgDifficulty = logs.stream().filter(l -> l.getDifficulty() != null).mapToInt(TourLog::getDifficulty).average().orElse(3);
            double normDiff = Math.min(avgDifficulty / 5.0, 1.0);
            double normDist = Math.min(avgDistance / 50.0, 1.0);
            double normTime = Math.min(avgTime / 300.0, 1.0);
            double cf = 10.0 - (normDiff * 4 + normDist * 3 + normTime * 3);
            cf = Math.max(1.0, Math.min(10.0, Math.round(cf * 10.0) / 10.0));
            sb.append(" childfriendliness ").append(cf);
        }

        // Append all individual log text and numeric fields so log-level searches work
        for (TourLog l : logs) {
            add(sb, l.getComment());
            add(sb, l.getLogDetails());
            add(sb, String.valueOf(l.getDifficulty()));
            add(sb, String.valueOf(l.getTotalDistance()));
            add(sb, String.valueOf(l.getTotalTimeMinutes()));
            add(sb, String.valueOf(l.getRating()));
            add(sb, String.valueOf(l.getLogDate()));
        }

        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Appends a non-null string to the StringBuilder with a leading space.
     * Null values are silently skipped so the haystack builder never throws NPE.
     *
     * @param sb the builder to append to
     * @param s  the string to append, or null to skip
     */
    private static void add(StringBuilder sb, String s) {
        if (s == null) return;
        sb.append(' ').append(s);
    }
}
