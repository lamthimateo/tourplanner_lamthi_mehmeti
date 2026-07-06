package tour_planner_lamthi_mehmeti.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tour_planner_lamthi_mehmeti.dto.TourExportDto;
import tour_planner_lamthi_mehmeti.dto.TourResponseDto;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.service.ReportService;
import tour_planner_lamthi_mehmeti.service.TourDataTransferService;
import tour_planner_lamthi_mehmeti.service.TourLogService;
import tour_planner_lamthi_mehmeti.service.TourService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for tours, logs, images, import/export, and PDF reports.
 */
@RestController
@RequestMapping("/api")
public class TourController {

    private final TourService tourService;
    private final TourLogService tourLogService;
    private final TourDataTransferService dataTransferService;
    private final ReportService reportService;

    @Value("${app.base-dir:${user.home}/TourPlanner}")
    private String baseDir;

    public TourController(TourService tourService,
                          TourLogService tourLogService,
                          TourDataTransferService dataTransferService,
                          ReportService reportService) {
        this.tourService = tourService;
        this.tourLogService = tourLogService;
        this.dataTransferService = dataTransferService;
        this.reportService = reportService;
    }

    // ── Tours ──

    @GetMapping("/tours")
    public List<TourResponseDto> getAllTours() {
        return tourService.getAllTours().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/tours/{id}")
    public TourResponseDto getTour(@PathVariable Long id) {
        return toResponseDto(tourService.getTourById(id));
    }

    // Client must not pick its own ID on create.
    @PostMapping("/tours")
    public TourResponseDto createTour(@Valid @RequestBody Tour tour) {
        tour.setId(null);
        Tour saved = tourService.createTour(tour);
        return toResponseDto(saved);
    }

    // Path ID wins over body ID.
    @PutMapping("/tours/{id}")
    public TourResponseDto updateTour(@PathVariable Long id, @Valid @RequestBody Tour tour) {
        tour.setId(id);
        Tour updated = tourService.updateTour(tour);
        return toResponseDto(updated);
    }

    @DeleteMapping("/tours/{id}")
    public void deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
    }

    // Returns IDs only — frontend filters its cached tour list locally.
    @GetMapping("/tours/search")
    public List<Long> searchTours(@RequestParam(name = "q", defaultValue = "") String q) {
        return tourService.searchTourIds(q);
    }

    // ── Tour logs ──

    @GetMapping("/tours/{tourId}/logs")
    public List<TourLog> getLogs(@PathVariable Long tourId) {
        return tourLogService.getAllTourLogs(tourId);
    }

    @PostMapping("/tours/{tourId}/logs")
    public TourLog createLog(@PathVariable Long tourId, @Valid @RequestBody TourLog log) {
        log.setId(null);
        return tourLogService.createTourLog(tourId, log);
    }

    @PutMapping("/tours/{tourId}/logs/{logId}")
    public TourLog updateLog(@PathVariable Long tourId, @PathVariable Long logId, @Valid @RequestBody TourLog log) {
        log.setId(logId);
        return tourLogService.updateTourLog(tourId, log);
    }

    @DeleteMapping("/tours/{tourId}/logs/{logId}")
    public void deleteLog(@PathVariable Long tourId, @PathVariable Long logId) {
        tourLogService.deleteTourLog(tourId, logId);
    }

    // ── Images ──

    // Deterministic filename so re-upload replaces the old image.
    @PostMapping(value = "/tours/{tourId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TourResponseDto uploadImage(@PathVariable Long tourId, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        // Only accept actual images — everything else is rejected with a 400
        // before a single byte reaches the disk.
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed (got " + contentType + ")");
        }

        // Ownership check FIRST (throws 404 for foreign/unknown tours) so an
        // attacker can't write files onto the server for tours they don't own.
        Tour tour = tourService.getTourById(tourId);

        // Extract the real extension so browsers later pick the right MIME.
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png";

        Path imagesDir = Path.of(baseDir, "images");
        Files.createDirectories(imagesDir);

        Path dest = imagesDir.resolve("tour_" + tourId + ext);
        Files.write(dest, file.getBytes());

        // Persist the new path on the tour so subsequent GETs find it.
        tour.setImagePath(dest.toAbsolutePath().toString());
        Tour updated = tourService.updateTour(tour);
        return toResponseDto(updated);
    }

    @GetMapping("/tours/{tourId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long tourId) throws IOException {
        Tour tour = tourService.getTourById(tourId);
        if (tour.getImagePath() == null || tour.getImagePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path path = Path.of(tour.getImagePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(path);
        // Detect the real MIME type so JPEG, PNG, GIF etc. all render correctly.
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = MediaType.IMAGE_PNG_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(bytes);
    }

    // ── Import / export ──

    @GetMapping("/tours/{tourId}/export")
    public TourExportDto exportTour(@PathVariable Long tourId) {
        return dataTransferService.exportTourData(tourId);
    }

    @PostMapping("/tours/import")
    public TourResponseDto importTour(@RequestBody TourExportDto dto) {
        Tour imported = dataTransferService.importTourData(dto);
        return toResponseDto(imported);
    }

    // ── PDF reports ──

    @GetMapping("/tours/{tourId}/report")
    public ResponseEntity<byte[]> tourReport(@PathVariable Long tourId) throws IOException {
        Tour tour = tourService.getTourById(tourId);
        Path file = reportService.generateTourReport(tour);
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TourReport_" + tourId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<byte[]> summaryReport() throws IOException {
        Path file = reportService.generateSummaryReport(tourService.getAllTours());
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SummaryReport.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /** Adds popularity and child-friendliness computed from logs. */
    private TourResponseDto toResponseDto(Tour tour) {
        int popularity = tourService.computePopularity(tour.getId());
        double childFriendliness = tourService.computeChildFriendliness(tour.getId());
        return new TourResponseDto(tour, popularity, childFriendliness);
    }
}
