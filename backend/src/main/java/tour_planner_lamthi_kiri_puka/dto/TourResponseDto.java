package tour_planner_lamthi_kiri_puka.dto;

import tour_planner_lamthi_kiri_puka.model.Tour;

/**
 * Data Transfer Object returned by the Tour API for read operations.
 *
 * <p>This DTO wraps the core fields of a {@link Tour} entity and augments them with two
 * computed, derived attributes that the frontend uses for display purposes:
 * <ul>
 *   <li><b>popularity</b> – the number of {@link tour_planner_lamthi_kiri_puka.model.TourLog}
 *       entries recorded for this tour. More logs indicate a more frequently undertaken
 *       (i.e. popular) tour.</li>
 *   <li><b>childFriendliness</b> – a composite score derived from the tour's distance,
 *       estimated time, and the average difficulty rating from its logs, normalised to
 *       the range [0, 10]. Higher values indicate a tour more suitable for children.</li>
 * </ul>
 *
 * <p>By keeping these computed fields in the DTO rather than the entity, the domain
 * model remains clean and the calculations can be performed on demand in the service
 * layer without polluting the JPA mapping.
 */
public class TourResponseDto {

    /** Surrogate primary key of the tour. */
    private Long id;

    /** Human-readable tour name. */
    private String name;

    /** Optional free-text description of the tour. */
    private String description;

    /** Starting location of the tour. */
    private String origin;

    /** End location of the tour. */
    private String destination;

    /** Mode of transport (e.g. {@code "driving-car"}, {@code "foot-walking"}). */
    private String transportType;

    /** Route distance in kilometres as returned by the routing service. */
    private Double distance;

    /** Estimated travel time in minutes as returned by the routing service. */
    private Integer estimatedTime;

    /** Absolute path to the tour's route map image on the server file system. */
    private String imagePath;

    /**
     * Derived popularity score equal to the total number of log entries for this tour.
     * A higher value means the tour has been completed more frequently.
     */
    private int popularity;

    /**
     * Derived child-friendliness score in the range [0, 10].
     * Computed from distance, estimated time, and average log difficulty.
     */
    private double childFriendliness;

    /**
     * Default no-argument constructor. Required by Jackson for deserialisation.
     */
    public TourResponseDto() {
    }

    /**
     * Constructs a fully populated {@code TourResponseDto} by copying the tour entity's
     * fields and appending the two computed metrics.
     *
     * @param tour              the source {@link Tour} entity; must not be {@code null}
     * @param popularity        the number of log entries recorded for this tour
     * @param childFriendliness the computed child-friendliness score in the range [0, 10]
     */
    public TourResponseDto(Tour tour, int popularity, double childFriendliness) {
        // Copy all plain entity fields into the DTO
        this.id = tour.getId();
        this.name = tour.getName();
        this.description = tour.getDescription();
        this.origin = tour.getOrigin();
        this.destination = tour.getDestination();
        this.transportType = tour.getTransportType();
        this.distance = tour.getDistance();
        this.estimatedTime = tour.getEstimatedTime();
        this.imagePath = tour.getImagePath();

        // Attach the two computed / derived attributes
        this.popularity = popularity;
        this.childFriendliness = childFriendliness;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Returns the tour's surrogate primary key.
     *
     * @return the tour ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the tour ID.
     *
     * @param id the surrogate primary key
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the human-readable tour name.
     *
     * @return the tour name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the tour name.
     *
     * @param name the tour name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the optional free-text description of the tour.
     *
     * @return the description, or {@code null} if none was set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the tour description.
     *
     * @param description optional free-text description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the starting location of the tour.
     *
     * @return the origin location string
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Sets the starting location.
     *
     * @param origin the origin location string
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Returns the end location of the tour.
     *
     * @return the destination location string
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Sets the end location.
     *
     * @param destination the destination location string
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * Returns the mode of transport for this tour.
     *
     * @return the transport type string
     */
    public String getTransportType() {
        return transportType;
    }

    /**
     * Sets the transport type.
     *
     * @param transportType the mode of transport
     */
    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    /**
     * Returns the route distance in kilometres.
     *
     * @return the distance in km, or {@code null} if not yet computed
     */
    public Double getDistance() {
        return distance;
    }

    /**
     * Sets the route distance in kilometres.
     *
     * @param distance the distance in km
     */
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    /**
     * Returns the estimated travel time in minutes.
     *
     * @return the estimated time in minutes, or {@code null} if not yet computed
     */
    public Integer getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * Sets the estimated travel time in minutes.
     *
     * @param estimatedTime the estimated time in minutes
     */
    public void setEstimatedTime(Integer estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    /**
     * Returns the absolute file-system path of the tour's route map image.
     *
     * @return the image path, or {@code null} if no image has been uploaded
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Sets the image path.
     *
     * @param imagePath absolute path on the server's file system
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Returns the popularity of the tour, measured as the total number of log entries.
     *
     * @return the popularity count (0 or more)
     */
    public int getPopularity() {
        return popularity;
    }

    /**
     * Sets the popularity count.
     *
     * @param popularity the number of log entries for this tour
     */
    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    /**
     * Returns the child-friendliness score for this tour in the range [0, 10].
     *
     * @return the child-friendliness score
     */
    public double getChildFriendliness() {
        return childFriendliness;
    }

    /**
     * Sets the child-friendliness score.
     *
     * @param childFriendliness the computed score in the range [0, 10]
     */
    public void setChildFriendliness(double childFriendliness) {
        this.childFriendliness = childFriendliness;
    }
}
