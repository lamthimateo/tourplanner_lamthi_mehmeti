package tour_planner_lamthi_mehmeti.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Log entry for a completed tour run. Used to compute popularity and child-friendliness.
 * Table: tour_logs
 */
@Entity
@Table(name = "tour_logs")
public class TourLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // JsonIgnore avoids infinite recursion when serializing logs with their parent tour.
    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonIgnore
    private Tour tour;

    @NotNull(message = "Log date is required")
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @NotBlank(message = "Comment is required")
    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    @NotNull(message = "Difficulty is required")
    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    @Column(name = "difficulty")
    private Integer difficulty;

    @Min(value = 0, message = "Total distance must be non-negative")
    @Column(name = "total_distance")
    private Double totalDistance;

    @Min(value = 0, message = "Total time must be non-negative")
    @Column(name = "total_time_minutes")
    private Integer totalTimeMinutes;

    @NotNull(message = "Rating is required")
    @Min(value = 0, message = "Rating must be between 0 and 10")
    @Max(value = 10, message = "Rating must be between 0 and 10")
    @Column(name = "rating")
    private Integer rating;

    @Size(max = 4000, message = "Details must be at most 4000 characters")
    @Column(name = "details", length = 4000)
    private String logDetails;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tour getTour() {
        return tour;
    }

    public void setTour(Tour tour) {
        this.tour = tour;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Integer getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public void setTotalTimeMinutes(Integer totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getLogDetails() {
        return logDetails;
    }

    public void setLogDetails(String logDetails) {
        this.logDetails = logDetails;
    }
}
