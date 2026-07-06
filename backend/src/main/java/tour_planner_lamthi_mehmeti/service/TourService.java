package tour_planner_lamthi_mehmeti.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.security.AuthContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tour CRUD, search, and computed metrics (popularity, child-friendliness).
 * Every method is scoped to the logged-in user via AuthContext.
 */
@Service
public class TourService {

    private static final Logger logger = LogManager.getLogger(TourService.class);

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private TourLogRepository tourLogRepository;

    public List<Tour> getAllTours() {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Fetching all tours for user {}", userId);
        return tourRepository.findByUserId(userId);
    }

    public Tour getTourById(Long id) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Fetching tour {} for user {}", id, userId);
        // findByIdAndUserId returns empty if the ID exists but belongs to another user —
        // this prevents unauthorised read access.
        return tourRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TourNotFoundException(id));
    }

    public Tour createTour(Tour tour) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Creating tour '{}' for user {}", tour.getName(), userId);
        // Always stamp the owner ID from the JWT rather than trusting the request body
        tour.setUserId(userId);
        return tourRepository.save(tour);
    }

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

    // Logs must be deleted first — no DB cascade on tour_id FK.
    @Transactional
    public void deleteTour(Long id) {
        Long userId = AuthContext.getCurrentUserId();
        logger.info("Deleting tour {} (and its logs) for user {}", id, userId);
        // Ownership check: 404 is safer than 403 here — avoids leaking that a tour ID exists
        if (!tourRepository.existsByIdAndUserId(id, userId)) {
            throw new TourNotFoundException(id);
        }
        // Children first, then the parent — both inside one transaction so a
        // failure midway rolls everything back.
        tourLogRepository.deleteByTourId(id);
        tourRepository.deleteById(id);
    }

    /** Substring search over tour fields, log fields, and computed values. Empty query = all IDs. */
    public List<Long> searchTourIds(String query) {
        // Normalise: null or blank query means "return everything"
        String q = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Tour> tours = getAllTours();

        if (q.isEmpty()) return tours.stream().map(Tour::getId).collect(Collectors.toList());

        // Load only the logs belonging to this user's tours (single derived
        // query instead of scanning the whole table), then group them by tour
        // ID for O(1) lookup per tour.
        List<Long> tourIds = tours.stream().map(Tour::getId).collect(Collectors.toList());
        Map<Long, List<TourLog>> logsByTour = tourLogRepository.findByTourIdIn(tourIds)
                .stream()
                .filter(l -> l.getTour() != null)
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

    public int computePopularity(Long tourId) {
        return tourLogRepository.findByTourId(tourId).size();
    }

    public double computeChildFriendliness(Long tourId) {
        return computeChildFriendliness(tourLogRepository.findByTourId(tourId));
    }

    // Score 1–10 from avg difficulty/distance/time; shared with search haystack.
    private static double computeChildFriendliness(List<TourLog> logs) {
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

        // Also append the child-friendliness score (same shared formula as the
        // API) so queries like "childfriendliness 6.4" find the tour.
        if (!logs.isEmpty()) {
            sb.append(" childfriendliness ").append(computeChildFriendliness(logs));
        }

        // Append all individual log text and numeric fields so log-level searches
        // work. Null fields are skipped instead of being stringified — otherwise
        // every tour with an incomplete log would match the query "null".
        for (TourLog l : logs) {
            add(sb, l.getComment());
            add(sb, l.getLogDetails());
            if (l.getDifficulty() != null)       add(sb, l.getDifficulty().toString());
            if (l.getTotalDistance() != null)    add(sb, l.getTotalDistance().toString());
            if (l.getTotalTimeMinutes() != null) add(sb, l.getTotalTimeMinutes().toString());
            if (l.getRating() != null)           add(sb, l.getRating().toString());
            if (l.getLogDate() != null)          add(sb, l.getLogDate().toString());
        }

        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void add(StringBuilder sb, String s) {
        if (s == null) return;
        sb.append(' ').append(s);
    }
}
