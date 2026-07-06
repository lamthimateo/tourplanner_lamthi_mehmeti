package tour_planner_lamthi_mehmeti.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Copies uploaded images to ~/TourPlanner/images/ with a random UUID filename.
 */
@Service
public class ImageStorageService {

    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1);
    }

    public Path getImagesDir() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), "TourPlanner", "images");
        Files.createDirectories(dir);
        return dir;
    }

    /** Copy source file to images dir under a UUID name; returns absolute path. */
    public String storeImage(Path sourceFile) throws IOException {
        if (sourceFile == null) return null;
        Path dir = getImagesDir();
        String ext = getExtension(sourceFile.getFileName().toString());
        // UUID collisions are astronomically unlikely; REPLACE_EXISTING is a
        // safety belt in the (impossible) case one is generated twice.
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        Path target = dir.resolve(filename);
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
}
