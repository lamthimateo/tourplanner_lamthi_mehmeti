package tour_planner_lamthi_kiri_puka.dto;

import java.util.List;

/**
 * Data Transfer Object used for tour import and export operations.
 *
 * <p>This DTO acts as the serialisation envelope for the
 * {@code GET /api/tours/{id}/export} and {@code POST /api/tours/import} endpoints. It
 * bundles together a snapshot of a tour's data and all its associated log entries in a
 * single self-contained JSON document that can be archived, shared, or later re-imported
 * into any Tour Planner instance.
 *
 * <p>The class uses two static inner classes – {@link TourDto} and {@link TourLogDto} –
 * as simple data containers with public fields to keep serialisation straightforward and
 * avoid the overhead of getters/setters for a pure transport type.
 *
 * <p>Dates in {@link TourLogDto} are represented as ISO-8601 strings
 * (e.g. {@code "2024-06-15"}) so that the JSON is human-readable and independent of
 * any particular {@link java.time.LocalDate} serialisation configuration.
 */
public class TourExportDto {

    /**
     * The tour data snapshot included in this export bundle.
     * Populated by {@link tour_planner_lamthi_kiri_puka.service.TourDataTransferService}
     * when exporting, and consumed when importing.
     */
    public TourDto tour;

    /**
     * The ordered list of log entry snapshots associated with the exported tour.
     * May be empty if the tour has no logs; never {@code null} after a valid export.
     */
    public List<TourLogDto> logs;

    // -------------------------------------------------------------------------
    // Inner DTOs
    // -------------------------------------------------------------------------

    /**
     * Flat snapshot of a {@link tour_planner_lamthi_kiri_puka.model.Tour} entity used
     * within an export bundle.
     *
     * <p>All fields are public to minimise boilerplate; this is a pure data-carrying
     * structure with no business logic.
     */
    public static class TourDto {

        /** Surrogate primary key of the original tour (informational only on import). */
        public Long id;

        /** Human-readable tour name. */
        public String name;

        /** Optional free-text description of the tour. */
        public String description;

        /** Starting location of the tour. */
        public String origin;

        /** End location of the tour. */
        public String destination;

        /**
         * Mode of transport (e.g. {@code "driving-car"}, {@code "cycling-regular"},
         * {@code "foot-walking"}).
         */
        public String transportType;

        /** Route distance in kilometres; may be {@code null}. */
        public Double distance;

        /** Estimated travel time in minutes; may be {@code null}. */
        public Integer estimatedTime;

        /**
         * Absolute path to the tour's route map image on the exporting server.
         * Note: this path may not be valid on the importing server; image transfer is
         * handled separately.
         */
        public String imagePath;
    }

    /**
     * Flat snapshot of a {@link tour_planner_lamthi_kiri_puka.model.TourLog} entity
     * used within an export bundle.
     *
     * <p>All fields are public; no business logic is present. The {@code logDate} field
     * is stored as an ISO-8601 date string (e.g. {@code "2024-06-15"}) for readability
     * and portability.
     */
    public static class TourLogDto {

        /** Surrogate primary key of the original log entry (informational only on import). */
        public Long id;

        /**
         * Date of the tour run as an ISO-8601 string (e.g. {@code "2024-06-15"}).
         * Using a {@code String} here avoids deserialisation issues when the importing
         * system has a different {@link java.time.LocalDate} Jackson configuration.
         */
        public String logDate; // ISO date

        /** Short comment describing the tour run. */
        public String comment;

        /** Subjective difficulty rating in the range [1, 5]. */
        public Integer difficulty;

        /** Actual distance covered during the run in kilometres. */
        public Double totalDistance;

        /** Actual duration of the run in minutes. */
        public Integer totalTimeMinutes;

        /** Overall satisfaction rating in the range [0, 10]. */
        public Integer rating;

        /** Extended free-text notes about the tour run. */
        public String logDetails;
    }
}
