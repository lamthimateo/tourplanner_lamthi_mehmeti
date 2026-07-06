package tour_planner_lamthi_mehmeti.dto;

import java.util.List;

/** JSON shape for tour import/export (tour + its logs). */
public class TourExportDto {

    public TourDto tour;
    public List<TourLogDto> logs;

    /** Flat tour snapshot with public fields for easy JSON mapping. */
    public static class TourDto {

        public Long id;
        public String name;
        public String description;
        public String origin;
        public String destination;
        public String transportType;
        public Double distance;
        public Integer estimatedTime;
        // Path from the exporting machine — may not exist on import.
        public String imagePath;
    }

    public static class TourLogDto {

        public Long id;
        public String logDate; // ISO date string, e.g. "2024-06-15"
        public String comment;
        public Integer difficulty;
        public Double totalDistance;
        public Integer totalTimeMinutes;
        public Integer rating;
        public String logDetails;
    }
}
