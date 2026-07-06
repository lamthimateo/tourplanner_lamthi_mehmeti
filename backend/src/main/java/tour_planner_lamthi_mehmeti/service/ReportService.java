package tour_planner_lamthi_mehmeti.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates PDF reports with iText (single tour or summary of all tours).
 */
@Service
public class ReportService {

    private static final Logger logger = LogManager.getLogger(ReportService.class);

    private final TourLogRepository tourLogRepository;

    @Value("${app.base-dir:${user.home}/TourPlanner}")
    private String baseDir;

    public ReportService(TourLogRepository tourLogRepository) {
        this.tourLogRepository = tourLogRepository;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public Path generateTourReport(Tour tour) throws IOException {
        Path reportsDir = Path.of(baseDir, "reports");
        Files.createDirectories(reportsDir);
        Path file = reportsDir.resolve("TourReport_" + tour.getId() + ".pdf");

        logger.info("Generating tour report for tour ID: {} at {}", tour.getId(), file);

        try (PdfWriter writer = new PdfWriter(file.toString())) {
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Tour Report"));
            document.add(new Paragraph("Tour Name: " + nullSafe(tour.getName())));
            document.add(new Paragraph("Description: " + nullSafe(tour.getDescription())));
            document.add(new Paragraph("Origin: " + nullSafe(tour.getOrigin())));
            document.add(new Paragraph("Destination: " + nullSafe(tour.getDestination())));
            document.add(new Paragraph("Transport Type: " + nullSafe(tour.getTransportType())));
            document.add(new Paragraph("Distance: " + (tour.getDistance() != null ? tour.getDistance() + " km" : "N/A")));
            document.add(new Paragraph("Estimated Time: " + (tour.getEstimatedTime() != null ? tour.getEstimatedTime() + " min" : "N/A")));

            addTourImageIfPresent(document, tour.getImagePath());

            List<TourLog> tourLogs = tourLogRepository.findByTourId(tour.getId());

            Table table = new Table(new float[]{1, 3, 1, 1, 1, 1});
            table.addCell(new Cell().add(new Paragraph("Date")));
            table.addCell(new Cell().add(new Paragraph("Comment")));
            table.addCell(new Cell().add(new Paragraph("Difficulty")));
            table.addCell(new Cell().add(new Paragraph("Distance")));
            table.addCell(new Cell().add(new Paragraph("Time (min)")));
            table.addCell(new Cell().add(new Paragraph("Rating")));

            for (TourLog log : tourLogs) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(log.getLogDate()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(log.getComment()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(log.getDifficulty()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(log.getTotalDistance()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(log.getTotalTimeMinutes()))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(log.getRating()))));
            }

            document.add(table);
            document.close();
        }
        return file;
    }

    public Path generateSummaryReport(List<Tour> tours) throws IOException {
        Path reportsDir = Path.of(baseDir, "reports");
        Files.createDirectories(reportsDir);
        Path file = reportsDir.resolve("SummaryReport.pdf");

        logger.info("Generating summary report at {}", file);

        try (PdfWriter writer = new PdfWriter(file.toString())) {
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Summary Report"));

            Table table = new Table(new float[]{1, 2, 3, 2, 2, 2, 1});
            table.addCell(new Cell().add(new Paragraph("Tour ID")));
            table.addCell(new Cell().add(new Paragraph("Name")));
            table.addCell(new Cell().add(new Paragraph("Description")));
            table.addCell(new Cell().add(new Paragraph("Origin")));
            table.addCell(new Cell().add(new Paragraph("Destination")));
            table.addCell(new Cell().add(new Paragraph("Transport Type")));
            table.addCell(new Cell().add(new Paragraph("Logs")));

            for (Tour tour : tours) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tour.getId()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(tour.getName()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(tour.getDescription()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(tour.getOrigin()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(tour.getDestination()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(tour.getTransportType()))));
                List<TourLog> tourLogs = tourLogRepository.findByTourId(tour.getId());
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tourLogs.size()))));
            }

            document.add(table);
            document.close();
        }
        return file;
    }

    /** Skip image on any error — the PDF is still useful without it. */
    private void addTourImageIfPresent(Document document, String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) return;

            // Accept either a plain file path or a "file:" URI (some code
            // paths set the path as a URI when copying uploaded images).
            String normalized = imagePath.trim();
            Path path;
            if (normalized.startsWith("file:")) {
                path = Path.of(URI.create(normalized));
            } else {
                path = Path.of(normalized);
            }
            if (!Files.exists(path)) return;

            ImageData imageData = ImageDataFactory.create(path.toAbsolutePath().toString());
            Image img = new Image(imageData);
            img.setAutoScale(true); // Scale to fit the available page width.
            document.add(new Paragraph("Tour Image:"));
            document.add(img);
        } catch (Exception ignored) {
            // Report should still generate even if image fails.
        }
    }
}
