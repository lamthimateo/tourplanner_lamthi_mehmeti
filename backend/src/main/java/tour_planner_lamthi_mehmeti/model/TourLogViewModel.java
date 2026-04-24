package tour_planner_lamthi_mehmeti.model;

import java.time.LocalDate;

/**
 * "View model" representation of a {@link TourLog}.
 *
 * <p>This class demonstrates the <b>MVVM pattern on the backend side</b>: it
 * exposes exactly the fields the UI needs, stripped of JPA annotations,
 * lifecycle callbacks, and the parent {@code Tour} relationship (which would
 * otherwise cause Jackson to serialize a huge graph). Keeping the view model
 * separate from the JPA entity lets us evolve the database schema without
 * breaking the JSON wire format, and vice-versa.
 *
 * <p>Currently used as a lightweight projection for scenarios where the full
 * entity would be overkill or would introduce circular references in JSON
 * serialization.
 */
public class TourLogViewModel {

    /** Primary key of the underlying log. */
    private Long id;
    /** Date the tour log was recorded. */
    private LocalDate logDate;
    /** Free-text remarks. */
    private String comment;
    /** Subjective 1–10 difficulty rating. */
    private Integer difficulty;
    /** Total distance actually walked / driven on this attempt (km). */
    private Double totalDistance;
    /** Elapsed time in whole minutes. */
    private Integer totalTimeMinutes;
    /** Subjective 1–10 rating of the experience. */
    private Integer rating;
    /** Long-form trip report. */
    private String logDetails;

    /** Default constructor required by Jackson for JSON deserialization. */
    public TourLogViewModel() {}

    /**
     * Copy-constructor that projects a {@link TourLog} entity into this
     * simpler view-model shape.
     *
     * @param tourLog source entity
     */
    public TourLogViewModel(TourLog tourLog) {
        this.id = tourLog.getId();
        this.logDate = tourLog.getLogDate();
        this.comment = tourLog.getComment();
        this.difficulty = tourLog.getDifficulty();
        this.totalDistance = tourLog.getTotalDistance();
        this.totalTimeMinutes = tourLog.getTotalTimeMinutes();
        this.rating = tourLog.getRating();
        this.logDetails = tourLog.getLogDetails();
    }

    /** Static factory — reads better at call sites than {@code new TourLogViewModel(log)}. */
    public static TourLogViewModel fromTourLog(TourLog tourLog) {
        return new TourLogViewModel(tourLog);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public Double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }

    public Integer getTotalTimeMinutes() { return totalTimeMinutes; }
    public void setTotalTimeMinutes(Integer totalTimeMinutes) { this.totalTimeMinutes = totalTimeMinutes; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getLogDetails() { return logDetails; }
    public void setLogDetails(String logDetails) { this.logDetails = logDetails; }
}
