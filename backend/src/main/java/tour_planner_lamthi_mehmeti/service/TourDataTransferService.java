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
 * Import / export service that round-trips a tour and its logs through a
 * portable JSON DTO ({@link TourExportDto}).
 *
 * <p>The two public operations used by the REST layer are:
 * <ul>
 *   <li>{@link #exportTourData(Long)} — returns a DTO for a given tour (with
 *       per-user ownership check when running inside a request).</li>
 *   <li>{@link #importTourData(TourExportDto)} — persists a new tour +
 *       logs; the incoming {@code userId} field is deliberately discarded
 *       and replaced with the current user's ID (so attackers can't hand
 *       themselves ownership of someone else's data by crafting an import
 *       file).</li>
 * </ul>
 *
 * <p>There are additional string/file helpers used by unit tests and a
 * potential CLI tool to materialize the DTO to / from JSON text on disk.
 *
 * <p>Design pattern: this class implements the <b>Data Transfer Object</b>
 * pattern — the wire format ({@code TourExportDto}) is decoupled from the
 * JPA entities so the export format can evolve without forcing database
 * migrations and vice-versa.
 */
@Service
public class TourDataTransferService {

    private static final Logger logger = LogManager.getLogger(TourDataTransferService.class);

    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;
    /** Shared Jackson mapper configured to serialize {@link LocalDate} as ISO text. */
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

    /**
     * Copies a {@link Tour} + its logs into a flat DTO tree suitable for
     * JSON serialization. Deliberately a private static helper — nothing
     * outside this class should know the DTO shape.
     */
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
     * Exports the given tour (including its logs) as a DTO that the
     * controller can directly serialize to JSON.
     *
     * <p>When running inside an HTTP request, the ownership check makes
     * sure one user cannot request another user's data by guessing the id.
     * For tests/CLI without a security context we fall back to raw lookup.
     *
     * @throws TourNotFoundException if the tour doesn't exist or isn't owned by the caller
     */
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

    // -------------------------------------------------------------------------
    // JSON string / file helpers (convenience wrappers for CLI and tests)
    // -------------------------------------------------------------------------

    /**
     * Persists a new tour (and its logs) from an import DTO.
     *
     * <p>Security hardening: the caller-supplied {@code userId} is discarded.
     * The new tour is always stamped with the currently authenticated user's
     * ID, so importing a payload that was exported by a different user
     * simply re-homes it into the importer's account.
     *
     * @throws IllegalArgumentException when {@code dto} or {@code dto.tour} is null
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

    /** Serializes the tour DTO to a pretty-printed JSON string. */
    public String exportTourToJsonString(Long tourId) throws IOException {
        TourExportDto dto = exportTourData(tourId);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
    }

    /** Writes the pretty-printed JSON export to {@code file} on disk. */
    public void exportTourToFile(Long tourId, Path file) throws IOException {
        String json = exportTourToJsonString(tourId);
        Files.writeString(file, json);
    }

    /** Imports a tour from a JSON string and returns the new tour's primary key. */
    public Long importTourFromJsonString(String json) throws IOException {
        TourExportDto dto = mapper.readValue(json, TourExportDto.class);
        Tour saved = importTourData(dto);
        return saved.getId();
    }

    /** Imports a tour from a JSON file on disk and returns the new tour's primary key. */
    public Long importTourFromFile(Path file) throws IOException {
        String json = Files.readString(file);
        return importTourFromJsonString(json);
    }

    /**
     * Returns the current user's ID if a Spring Security context is active,
     * or {@code null} otherwise. Keeps this service usable in unit tests and
     * in CLI invocations that don't set up an authentication context.
     */
    private static Long currentUserIdOrNull() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
