package tour_planner_lamthi_mehmeti.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Tour entity. Each tour belongs to one user (userId).
 * Table: tour
 */
@Entity
@Table(name = "tour")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hidden from JSON — owner is always the logged-in user.
    @JsonIgnore
    @Column(name = "owner_id")
    private Long userId;

    @NotBlank(message = "Tour name is required")
    @Size(max = 200, message = "Tour name must be at most 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Transport type is required")
    private String transportType;

    // Filled after an ORS route call; may be null before that.
    @PositiveOrZero(message = "Distance must not be negative")
    private Double distance;

    @Min(value = 0, message = "Estimated time must not be negative")
    private Integer estimatedTime;

    // Absolute path under ~/TourPlanner/images/
    private String imagePath;

    public Tour() {
    }

    /** Convenience constructor for tests. */
    public Tour(String name, String description, String origin, String destination, String transportType, String imagePath) {
        this.name = name;
        this.description = description;
        this.origin = origin;
        this.destination = destination;
        this.transportType = transportType;
        this.imagePath = imagePath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Integer getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Integer estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
