package tour_planner_lamthi_mehmeti.dto;

import tour_planner_lamthi_mehmeti.model.Tour;

/**
 * Tour API response with two extra computed fields:
 * popularity (log count) and childFriendliness (1–10 score from logs).
 */
public class TourResponseDto {

    private Long id;
    private String name;
    private String description;
    private String origin;
    private String destination;
    private String transportType;
    private Double distance;
    private Integer estimatedTime;
    private String imagePath;
    private int popularity;
    private double childFriendliness;

    public TourResponseDto() {
    }

    public TourResponseDto(Tour tour, int popularity, double childFriendliness) {
        this.id = tour.getId();
        this.name = tour.getName();
        this.description = tour.getDescription();
        this.origin = tour.getOrigin();
        this.destination = tour.getDestination();
        this.transportType = tour.getTransportType();
        this.distance = tour.getDistance();
        this.estimatedTime = tour.getEstimatedTime();
        this.imagePath = tour.getImagePath();
        this.popularity = popularity;
        this.childFriendliness = childFriendliness;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    public double getChildFriendliness() {
        return childFriendliness;
    }

    public void setChildFriendliness(double childFriendliness) {
        this.childFriendliness = childFriendliness;
    }
}
