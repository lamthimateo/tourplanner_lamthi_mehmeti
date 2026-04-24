package tour_planner_lamthi_kiri_puka.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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

    public String storeImage(Path sourceFile) throws IOException {
        if (sourceFile == null) return null;
        Path dir = getImagesDir();
        String ext = getExtension(sourceFile.getFileName().toString());
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        Path target = dir.resolve(filename);
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
}
