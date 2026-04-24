package tour_planner_lamthi_kiri_puka.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tour_planner_lamthi_kiri_puka.dto.TourExportDto;
import tour_planner_lamthi_kiri_puka.dto.TourResponseDto;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.service.ReportService;
import tour_planner_lamthi_kiri_puka.service.TourDataTransferService;
import tour_planner_lamthi_kiri_puka.service.TourLogService;
import tour_planner_lamthi_kiri_puka.service.TourService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TourController {

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

    // --- Tours ---

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

    @PostMapping("/tours")
    public TourResponseDto createTour(@Valid @RequestBody Tour tour) {
        tour.setId(null);
        Tour saved = tourService.createTour(tour);
        return toResponseDto(saved);
    }

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

    @GetMapping("/tours/search")
    public List<Long> searchTours(@RequestParam(name = "q", defaultValue = "") String q) {
        return tourService.searchTourIds(q);
    }

    // --- Tour logs ---

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

    // --- Image upload ---

    @PostMapping(value = "/tours/{tourId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TourResponseDto uploadImage(@PathVariable Long tourId, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png";

        Path imagesDir = Path.of(System.getProperty("user.home"), "TourPlanner", "images");
        Files.createDirectories(imagesDir);

        Path dest = imagesDir.resolve("tour_" + tourId + ext);
        Files.write(dest, file.getBytes());

        Tour tour = tourService.getTourById(tourId);
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
        // Detect the real MIME type so JPEG, PNG, GIF etc. all render correctly
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = MediaType.IMAGE_PNG_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(bytes);
    }

    // --- Import/Export ---

    @GetMapping("/tours/{tourId}/export")
    public TourExportDto exportTour(@PathVariable Long tourId) {
        return dataTransferService.exportTourData(tourId);
    }

    @PostMapping("/tours/import")
    public TourResponseDto importTour(@RequestBody TourExportDto dto) {
        Tour imported = dataTransferService.importTourData(dto);
        return toResponseDto(imported);
    }

    // --- Reports ---

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

    // --- Helper ---

    private TourResponseDto toResponseDto(Tour tour) {
        int popularity = tourService.computePopularity(tour.getId());
        double childFriendliness = tourService.computeChildFriendliness(tour.getId());
        return new TourResponseDto(tour, popularity, childFriendliness);
    }
}
