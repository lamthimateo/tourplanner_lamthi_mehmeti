package tour_planner_lamthi_kiri_puka.model;

import java.time.LocalDate;

public class TourLogViewModel {
    private Long id;
    private LocalDate logDate;
    private String comment;
    private Integer difficulty;
    private Double totalDistance;
    private Integer totalTimeMinutes;
    private Integer rating;
    private String logDetails;

    public TourLogViewModel() {}

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
