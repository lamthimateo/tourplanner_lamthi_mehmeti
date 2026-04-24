package tour_planner_lamthi_kiri_puka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * JPA entity representing a single log entry for a completed tour outing.
 *
 * <p>A TourLog records the real-world outcome of undertaking a particular {@link Tour}
 * on a specific date. Users can attach multiple logs to the same tour so they can
 * track how their performance, experience, or conditions change across different runs.
 *
 * <p>The aggregated data from all logs attached to a tour is used by the service layer
 * to compute two derived tour attributes:
 * <ul>
 *   <li><b>Popularity</b> — simply the number of log entries (more logs = more popular).</li>
 *   <li><b>Child-friendliness</b> — a score in [1, 10] derived from average difficulty,
 *       average distance, and average time. Lower difficulty and shorter outings score
 *       closer to 10.</li>
 * </ul>
 *
 * <p>Database table: {@code tour_logs}
 */
@Entity
@Table(name = "tour_logs")
public class TourLog {

    /**
     * Surrogate primary key, auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent {@link Tour} that this log entry belongs to.
     *
     * <p>{@code @ManyToOne} declares the many-logs-to-one-tour relationship.
     * {@code @JoinColumn} maps it to the {@code tour_id} foreign key column.
     * {@code @JsonIgnore} prevents infinite recursion during JSON serialisation —
     * the client does not need to see the full tour object embedded in each log.
     */
    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonIgnore
    private Tour tour;

    /**
     * The calendar date on which the tour was completed.
     * Stored as a standard SQL DATE via JPA's LocalDate mapping.
     * Required; used as the primary label when listing logs in the UI.
     */
    @NotNull(message = "Log date is required")
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    /**
     * A free-text comment describing the outing (conditions, highlights, notes).
     * Required; maximum 2 000 characters so the column fits in most VARCHAR types.
     */
    @NotBlank(message = "Comment is required")
    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    /**
     * Subjective difficulty of this outing on a scale from 1 (very easy) to 5 (very hard).
     * Used in the child-friendliness formula: higher difficulty → lower score.
     */
    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    @Column(name = "difficulty")
    private Integer difficulty;

    /**
     * Actual total distance covered during this outing in kilometres.
     * May differ from the tour's planned {@link Tour#getDistance()} if the route was altered.
     * Must be non-negative.
     */
    @Min(value = 0, message = "Total distance must be non-negative")
    @Column(name = "total_distance")
    private Double totalDistance;

    /**
     * Actual total time spent on the outing in minutes.
     * Must be non-negative. Used in the child-friendliness formula: longer = less child-friendly.
     */
    @Min(value = 0, message = "Total time must be non-negative")
    @Column(name = "total_time_minutes")
    private Integer totalTimeMinutes;

    /**
     * Overall satisfaction rating for this outing on a scale from 0 (terrible) to 10 (excellent).
     * Aggregated across all logs to form the tour's average rating shown in the statistics dashboard.
     */
    @Min(value = 0, message = "Rating must be between 0 and 10")
    @Max(value = 10, message = "Rating must be between 0 and 10")
    @Column(name = "rating")
    private Integer rating;

    /**
     * Extended free-text notes about the outing (e.g. weather, gear, personal thoughts).
     * Optional; maximum 4 000 characters, stored in a longer column than {@link #comment}.
     */
    @Size(max = 4000, message = "Details must be at most 4000 characters")
    @Column(name = "details", length = 4000)
    private String logDetails;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /** @return the database-assigned primary key */
    public Long getId() {
        return id;
    }

    /** @param id the surrogate primary key */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the parent Tour entity */
    public Tour getTour() {
        return tour;
    }

    /**
     * Associates this log with a parent tour. Must be set before persisting.
     *
     * @param tour the parent {@link Tour}; must not be {@code null}
     */
    public void setTour(Tour tour) {
        this.tour = tour;
    }

    /** @return the date on which the tour was completed */
    public LocalDate getLogDate() {
        return logDate;
    }

    /** @param logDate the completion date */
    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    /** @return the free-text comment about this outing */
    public String getComment() {
        return comment;
    }

    /** @param comment the free-text comment */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** @return the difficulty rating in [1, 5] */
    public Integer getDifficulty() {
        return difficulty;
    }

    /** @param difficulty the difficulty rating (1 = easy, 5 = very hard) */
    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    /** @return the actual distance covered in km */
    public Double getTotalDistance() {
        return totalDistance;
    }

    /** @param totalDistance the actual distance covered in km */
    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance;
    }

    /** @return the actual time spent in minutes */
    public Integer getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    /** @param totalTimeMinutes the actual time spent in minutes */
    public void setTotalTimeMinutes(Integer totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    /** @return the overall satisfaction rating in [0, 10] */
    public Integer getRating() {
        return rating;
    }

    /** @param rating the overall satisfaction rating (0–10) */
    public void setRating(Integer rating) {
        this.rating = rating;
    }

    /** @return the optional extended details/notes, may be null */
    public String getLogDetails() {
        return logDetails;
    }

    /** @param logDetails optional extended notes */
    public void setLogDetails(String logDetails) {
        this.logDetails = logDetails;
    }
}
