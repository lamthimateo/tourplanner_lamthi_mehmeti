package tour_planner_lamthi_mehmeti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.dto.TourExportDto;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;
import tour_planner_lamthi_mehmeti.security.AuthContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Import/export tours as JSON via TourExportDto.
 */
@Service
public class TourDataTransferService {

    private static final Logger logger = LogManager.getLogger(TourDataTransferService.class);

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;
    /** Jackson mapper with ISO date strings instead of timestamps. */
    private final ObjectMapper mapper;

    public TourDataTransferService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
        this.mapper = new ObjectMapper()
                // JavaTimeModule teaches Jackson about LocalDate/Instant so
                // we don't have to register custom (de)serializers ourselves.
                .registerModule(new JavaTimeModule())
                // ISO-8601 strings are stable across time zones and much
                // friendlier to diff than numeric epoch timestamps.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static TourExportDto toDto(Tour tour, List<TourLog> logs) {
        TourExportDto dto = new TourExportDto();
        dto.tour = new TourExportDto.TourDto();
        dto.tour.id = tour.getId();
        dto.tour.name = tour.getName();
        dto.tour.description = tour.getDescription();
        dto.tour.origin = tour.getOrigin();
        dto.tour.destination = tour.getDestination();
        dto.tour.transportType = tour.getTransportType();
        dto.tour.distance = tour.getDistance();
        dto.tour.estimatedTime = tour.getEstimatedTime();
        dto.tour.imagePath = tour.getImagePath();

        dto.logs = logs.stream().map(l -> {
            TourExportDto.TourLogDto x = new TourExportDto.TourLogDto();
            x.id = l.getId();
            x.logDate = l.getLogDate() == null ? null : l.getLogDate().toString();
            x.comment = l.getComment();
            x.difficulty = l.getDifficulty();
            x.totalDistance = l.getTotalDistance();
            x.totalTimeMinutes = l.getTotalTimeMinutes();
            x.rating = l.getRating();
            x.logDetails = l.getLogDetails();
            return x;
        }).collect(Collectors.toList());
        return dto;
    }

    public TourExportDto exportTourData(Long tourId) {
        logger.info("Exporting tour data for tour ID: {}", tourId);
        Long userId = currentUserIdOrNull();
        // Scope export to the caller's tours when an auth context is present.
        // When called without an auth context (CLI/tests) we fall back to the raw lookup.
        Tour tour = (userId == null
                ? tourRepository.findById(tourId)
                : tourRepository.findByIdAndUserId(tourId, userId))
                .orElseThrow(() -> new TourNotFoundException(tourId));
        List<TourLog> logs = tourLogRepository.findByTourId(tour.getId());
        return toDto(tour, logs);
    }

    // Import always stamps the current user as owner (never trust exported userId).
    public Tour importTourData(TourExportDto dto) {
        logger.info("Importing tour data");
        if (dto == null || dto.tour == null) {
            throw new IllegalArgumentException("Invalid import data");
        }

        Tour tour = new Tour();
        tour.setName(dto.tour.name);
        tour.setDescription(dto.tour.description);
        tour.setOrigin(dto.tour.origin);
        tour.setDestination(dto.tour.destination);
        tour.setTransportType(dto.tour.transportType);
        tour.setDistance(dto.tour.distance);
        tour.setEstimatedTime(dto.tour.estimatedTime);
        // Image paths are machine-specific; re-upload after import.
        tour.setImagePath(null);
        // Stamp ownership from the JWT so an import never re-attaches the tour to
        // a different (or missing) user from the original export file.
        Long userId = currentUserIdOrNull();
        if (userId != null) {
            tour.setUserId(userId);
        }
        Tour savedTour = tourRepository.save(tour);

        if (dto.logs != null) {
            for (TourExportDto.TourLogDto l : dto.logs) {
                TourLog log = new TourLog();
                log.setTour(savedTour);
                log.setLogDate(l.logDate == null ? LocalDate.now() : LocalDate.parse(l.logDate));
                log.setComment(l.comment);
                log.setDifficulty(l.difficulty);
                log.setTotalDistance(l.totalDistance);
                log.setTotalTimeMinutes(l.totalTimeMinutes);
                log.setRating(l.rating);
                log.setLogDetails(l.logDetails);
                tourLogRepository.save(log);
            }
        }
        return savedTour;
    }

    public String exportTourToJsonString(Long tourId) throws IOException {
        TourExportDto dto = exportTourData(tourId);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
    }

    public void exportTourToFile(Long tourId, Path file) throws IOException {
        String json = exportTourToJsonString(tourId);
        Files.writeString(file, json);
    }

    public Long importTourFromJsonString(String json) throws IOException {
        TourExportDto dto = mapper.readValue(json, TourExportDto.class);
        Tour saved = importTourData(dto);
        return saved.getId();
    }

    public Long importTourFromFile(Path file) throws IOException {
        String json = Files.readString(file);
        return importTourFromJsonString(json);
    }

    /** Returns current user ID, or null when called outside a request (tests/CLI). */
    private static Long currentUserIdOrNull() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
