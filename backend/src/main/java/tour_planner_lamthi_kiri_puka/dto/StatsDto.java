package tour_planner_lamthi_kiri_puka.dto;

import java.util.List;

/**
 * Unique feature: aggregated statistics across all tours for the current user.
 */
public class StatsDto {
    private int totalTours;
    private int totalLogs;
    private double totalDistanceKm;
    private double totalTimeHours;
    private double avgRating;
    private List<TransportStat> byTransportType;

    public StatsDto() {
    }

    public int getTotalTours() {
        return totalTours;
    }

    public void setTotalTours(int totalTours) {
        this.totalTours = totalTours;
    }

    public int getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(int totalLogs) {
        this.totalLogs = totalLogs;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public double getTotalTimeHours() {
        return totalTimeHours;
    }

    public void setTotalTimeHours(double totalTimeHours) {
        this.totalTimeHours = totalTimeHours;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public List<TransportStat> getByTransportType() {
        return byTransportType;
    }

    public void setByTransportType(List<TransportStat> byTransportType) {
        this.byTransportType = byTransportType;
    }

    public static class TransportStat {
        private String transportType;
        private int tourCount;
        private int logCount;
        private double avgDistanceKm;
        private double avgRating;

        public TransportStat() {
        }

        public TransportStat(String transportType, int tourCount, int logCount,
                             double avgDistanceKm, double avgRating) {
            this.transportType = transportType;
            this.tourCount = tourCount;
            this.logCount = logCount;
            this.avgDistanceKm = avgDistanceKm;
            this.avgRating = avgRating;
        }

        public String getTransportType() {
            return transportType;
        }

        public int getTourCount() {
            return tourCount;
        }

        public int getLogCount() {
            return logCount;
        }

        public double getAvgDistanceKm() {
            return avgDistanceKm;
        }

        public double getAvgRating() {
            return avgRating;
        }
    }
}
