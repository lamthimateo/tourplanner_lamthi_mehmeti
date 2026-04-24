package tour_planner_lamthi_kiri_puka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a single tour in the Tour Planner application.
 *
 * <p>A Tour describes a planned journey from an origin to a destination using a
 * specific transport type. It stores both user-provided fields (name, description,
 * locations, transport type) and values computed/fetched from OpenRouteService
 * (distance, estimatedTime, imagePath).
 *
 * <p>Multi-user isolation: each tour is owned by exactly one user, identified by
 * {@link #userId}. The service layer always filters by the authenticated user's ID
 * so users can never see or modify each other's tours.
 *
 * <p>Database table: {@code tour}
 */
@Entity
@Table(name = "tour")
public class Tour {

    /**
     * Surrogate primary key, auto-incremented by the database.
     * JPA generates the INSERT without an explicit ID; the database assigns the next value.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key referencing the {@link User} who owns this tour.
     *
     * <p>{@code @JsonIgnore} prevents this field from appearing in API responses,
     * hiding the internal user ID from clients. The owning user is always the
     * currently authenticated user; the frontend never needs to read this value.
     */
    @JsonIgnore
    @Column(name = "owner_id")
    private Long userId;

    /**
     * Human-readable name of the tour (e.g. "Vienna City Walk").
     * Required and capped at 200 characters.
     */
    @NotBlank(message = "Tour name is required")
    @Size(max = 200, message = "Tour name must be at most 200 characters")
    private String name;

    /**
     * Optional free-text description providing additional context about the tour.
     * Maximum 2 000 characters.
     */
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    /**
     * Starting location of the tour as a human-readable string
     * (e.g. a city name, address, or landmark). Required.
     * Used as input to the geocoder to determine route start coordinates.
     */
    @NotBlank(message = "Origin is required")
    private String origin;

    /**
     * End location of the tour. Required.
     * Used as input to the geocoder to determine route end coordinates.
     */
    @NotBlank(message = "Destination is required")
    private String destination;

    /**
     * Mode of transport for this tour (e.g. {@code "driving-car"}, {@code "foot-walking"},
     * {@code "cycling-regular"}). Required; determines which ORS routing profile is used.
     */
    @NotBlank(message = "Transport type is required")
    private String transportType;

    /**
     * Total route distance in kilometres, populated after a successful ORS route call.
     * May be null if the route has not been calculated yet.
     */
    private Double distance;

    /**
     * Estimated travel time in minutes, populated after a successful ORS route call.
     * May be null if the route has not been calculated yet.
     */
    private Integer estimatedTime;

    /**
     * Absolute file-system path to the tour's map image stored under
     * {@code ~/TourPlanner/images/}. Null when no image has been uploaded.
     */
    private String imagePath;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default no-arg constructor required by JPA for entity instantiation. */
    public Tour() {
    }

    /**
     * Convenience constructor for tests and seed data.
     *
     * @param name          human-readable tour name
     * @param description   optional description
     * @param origin        start location string
     * @param destination   end location string
     * @param transportType mode of transport
     * @param imagePath     absolute path to the tour image, or null
     */
    public Tour(String name, String description, String origin, String destination, String transportType, String imagePath) {
        this.name = name;
        this.description = description;
        this.origin = origin;
        this.destination = destination;
        this.transportType = transportType;
        this.imagePath = imagePath;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Returns the database-assigned primary key.
     *
     * @return the tour ID, or {@code null} before the entity is persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key (used by JPA on load; should not be called manually).
     *
     * @param id the surrogate primary key
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the ID of the user who owns this tour.
     *
     * @return the owner's user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the owner user ID. Called by the service layer after reading the
     * authenticated user from the security context.
     *
     * @param userId the owner's user ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** @return the tour name */
    public String getName() {
        return name;
    }

    /** @param name the tour name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the optional description, may be null */
    public String getDescription() {
        return description;
    }

    /** @param description the optional description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the starting location string */
    public String getOrigin() {
        return origin;
    }

    /** @param origin the starting location string */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /** @return the ending location string */
    public String getDestination() {
        return destination;
    }

    /** @param destination the ending location string */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /** @return the transport type (e.g. "driving-car") */
    public String getTransportType() {
        return transportType;
    }

    /** @param transportType the transport type */
    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    /** @return the route distance in km, or null if not yet calculated */
    public Double getDistance() {
        return distance;
    }

    /** @param distance the route distance in km */
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    /** @return the estimated travel time in minutes, or null if not yet calculated */
    public Integer getEstimatedTime() {
        return estimatedTime;
    }

    /** @param estimatedTime the estimated travel time in minutes */
    public void setEstimatedTime(Integer estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    /** @return the absolute path to the tour image file, or null */
    public String getImagePath() {
        return imagePath;
    }

    /** @param imagePath absolute path to the tour image file */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
