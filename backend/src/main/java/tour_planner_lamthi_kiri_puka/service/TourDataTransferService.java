package tour_planner_lamthi_kiri_puka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_kiri_puka.dto.TourExportDto;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.repository.TourLogRepository;
import tour_planner_lamthi_kiri_puka.repository.TourRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TourDataTransferService {

    private static final Logger logger = LogManager.getLogger(TourDataTransferService.class);

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;
    private final ObjectMapper mapper;

    public TourDataTransferService(TourRepository tourRepository, TourLogRepository tourLogRepository) {
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
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

    /**
     * Export a tour (with logs) as a DTO — used by the controller directly.
     */
    public TourExportDto exportTourData(Long tourId) {
        logger.info("Exporting tour data for tour ID: {}", tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with ID: " + tourId));
        List<TourLog> logs = tourLogRepository.findByTourId(tourId);
        return toDto(tour, logs);
    }

    // --- JSON string / file helpers ---

    /**
     * Import a tour (with logs) from a DTO — used by the controller directly.
     */
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
        tour.setImagePath(dto.tour.imagePath);
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
}
