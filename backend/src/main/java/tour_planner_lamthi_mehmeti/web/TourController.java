package tour_planner_lamthi_mehmeti.web;

import jakarta.validation.Valid;
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
 * Main REST controller — surfaces every tour-related endpoint the Angular
 * frontend consumes.
 *
 * <p>Sections (separated below by inline comments):
 * <ul>
 *   <li><b>Tours</b>: CRUD + search.</li>
 *   <li><b>Tour logs</b>: nested CRUD under a parent tour.</li>
 *   <li><b>Image upload / download</b>: multipart upload + binary download
 *       with correct MIME type detection.</li>
 *   <li><b>Import / Export</b>: JSON round-trip of a tour + its logs.</li>
 *   <li><b>Reports</b>: PDF generation via iText 7.</li>
 * </ul>
 *
 * <p>All business rules (ownership checks, validation, computed attributes,
 * ORS geocoding) live in the service layer. This controller is deliberately
 * dumb — it only marshals HTTP in, calls the service, and marshals HTTP out.
 * That separation is what the spec calls the "layered architecture".
 */
@RestController
@RequestMapping("/api")
public class TourController {

    // Collaborators — one service per bounded responsibility. Constructor-
    // injection makes the dependency graph explicit and trivially testable.
    private final TourService tourService;
    private final TourLogService tourLogService;
    private final TourDataTransferService dataTransferService;
    private final ReportService reportService;

    public TourController(TourService tourService,
                          TourLogService tourLogService,
                          TourDataTransferService dataTransferService,
                          ReportService reportService) {
        this.tourService = tourService;
        this.tourLogService = tourLogService;
        this.dataTransferService = dataTransferService;
        this.reportService = reportService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tours — CRUD + search
    // ─────────────────────────────────────────────────────────────────────────

    /** Lists every tour owned by the current user, wrapped with computed attributes. */
    @GetMapping("/tours")
    public List<TourResponseDto> getAllTours() {
        return tourService.getAllTours().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /** Single-tour read by ID. Ownership is enforced by {@code tourService}. */
    @GetMapping("/tours/{id}")
    public TourResponseDto getTour(@PathVariable Long id) {
        return toResponseDto(tourService.getTourById(id));
    }

    /**
     * Creates a tour from the JSON body. We explicitly null the id so a
     * client can't pick its own primary key (important when auto-generated
     * IDs are the source of truth for ownership in other tables).
     */
    @PostMapping("/tours")
    public TourResponseDto createTour(@Valid @RequestBody Tour tour) {
        tour.setId(null);
        Tour saved = tourService.createTour(tour);
        return toResponseDto(saved);
    }

    /**
     * Full replace of a tour. The path variable wins over the body id, so a
     * malicious / buggy client cannot update tour B via PUT on tour A's URL.
     */
    @PutMapping("/tours/{id}")
    public TourResponseDto updateTour(@PathVariable Long id, @Valid @RequestBody Tour tour) {
        tour.setId(id);
        Tour updated = tourService.updateTour(tour);
        return toResponseDto(updated);
    }

    /** Deletes a tour (and cascades to its logs). Returns 200 with an empty body. */
    @DeleteMapping("/tours/{id}")
    public void deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
    }

    /**
     * Full-text search that accepts a free-form query and returns only the
     * IDs of matching tours. The frontend keeps the full tour list cached
     * and filters it locally using this list — this keeps responses tiny
     * and avoids re-shipping the same data.
     */
    @GetMapping("/tours/search")
    public List<Long> searchTours(@RequestParam(name = "q", defaultValue = "") String q) {
        return tourService.searchTourIds(q);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tour logs — nested CRUD
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns all logs for a tour (after verifying the tour belongs to the user). */
    @GetMapping("/tours/{tourId}/logs")
    public List<TourLog> getLogs(@PathVariable Long tourId) {
        return tourLogService.getAllTourLogs(tourId);
    }

    /** Creates a new log under the given tour. Path wins over body id — see create/update above. */
    @PostMapping("/tours/{tourId}/logs")
    public TourLog createLog(@PathVariable Long tourId, @Valid @RequestBody TourLog log) {
        log.setId(null);
        return tourLogService.createTourLog(tourId, log);
    }

    /** Full replace of a single log. */
    @PutMapping("/tours/{tourId}/logs/{logId}")
    public TourLog updateLog(@PathVariable Long tourId, @PathVariable Long logId, @Valid @RequestBody TourLog log) {
        log.setId(logId);
        return tourLogService.updateTourLog(tourId, log);
    }

    /** Deletes a log; verifies both ownership and parent-tour relationship. */
    @DeleteMapping("/tours/{tourId}/logs/{logId}")
    public void deleteLog(@PathVariable Long tourId, @PathVariable Long logId) {
        tourLogService.deleteTourLog(tourId, logId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image upload / download
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Accepts a multipart file upload and stores it under
     * {@code ~/TourPlanner/images/tour_&lt;id&gt;.&lt;ext&gt;}. The deterministic
     * file-name is intentional — uploading again replaces the previous image
     * for the same tour, which matches user expectations.
     */
    @PostMapping(value = "/tours/{tourId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TourResponseDto uploadImage(@PathVariable Long tourId, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        // Extract the real extension so browsers later pick the right MIME.
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png";

        Path imagesDir = Path.of(System.getProperty("user.home"), "TourPlanner", "images");
        Files.createDirectories(imagesDir);

        Path dest = imagesDir.resolve("tour_" + tourId + ext);
        Files.write(dest, file.getBytes());

        // Persist the new path on the tour so subsequent GETs find it.
        Tour tour = tourService.getTourById(tourId);
        tour.setImagePath(dest.toAbsolutePath().toString());
        Tour updated = tourService.updateTour(tour);
        return toResponseDto(updated);
    }

    /**
     * Streams the image bytes back to the browser with the correct MIME
     * type. Returns 404 when the tour has no image set <i>or</i> the file
     * has been removed out-of-band from disk.
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // Import / Export (unique feature helpers)
    // ─────────────────────────────────────────────────────────────────────────

    /** Exports a tour (and its logs) as structured JSON. Scoped to the current user. */
    @GetMapping("/tours/{tourId}/export")
    public TourExportDto exportTour(@PathVariable Long tourId) {
        return dataTransferService.exportTourData(tourId);
    }

    /**
     * Imports a previously-exported tour. The incoming user ID is discarded
     * and replaced with the current user's ID — see
     * {@code TourDataTransferService} for the hardening rationale.
     */
    @PostMapping("/tours/import")
    public TourResponseDto importTour(@RequestBody TourExportDto dto) {
        Tour imported = dataTransferService.importTourData(dto);
        return toResponseDto(imported);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF reports
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a per-tour PDF report with route data + all logs. */
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

    /** Summary PDF covering every tour the current user owns. */
    @GetMapping("/reports/summary")
    public ResponseEntity<byte[]> summaryReport() throws IOException {
        Path file = reportService.generateSummaryReport(tourService.getAllTours());
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SummaryReport.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps a {@link Tour} entity in {@link TourResponseDto}, attaching the two
     * computed attributes the UI displays as small badges in the tour list.
     *
     * <p>Computing these at DTO-build time (rather than storing them in the
     * database) keeps the persistence layer simple and the values always
     * consistent with the current logs.
     */
    private TourResponseDto toResponseDto(Tour tour) {
        int popularity = tourService.computePopularity(tour.getId());
        double childFriendliness = tourService.computeChildFriendliness(tour.getId());
        return new TourResponseDto(tour, popularity, childFriendliness);
    }
}
