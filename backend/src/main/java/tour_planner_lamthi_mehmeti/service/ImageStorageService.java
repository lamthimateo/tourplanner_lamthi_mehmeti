package tour_planner_lamthi_mehmeti.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service responsible for copying user-supplied image files into the
 * application's managed image directory ({@code ~/TourPlanner/images/}).
 *
 * <p>Each stored file is renamed to a random UUID so that user-supplied file
 * names can never collide, contain path-traversal payloads, or overwrite an
 * existing image. The original file-name is only consulted to preserve the
 * file extension so the browser can still pick the correct MIME type when
 * the image is served.
 *
 * <p>Note: the HTTP upload path in {@code TourController} currently uses a
 * deterministic name (so that re-uploading replaces the previous image);
 * this class is kept as a more generic building block that other features
 * (import, seed data, CLI tools) can rely on.
 */
@Service
public class ImageStorageService {

    /**
     * Extracts the extension (without the dot) from a file name.
     * Returns an empty string for names with no dot or names ending in a dot.
     */
    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1);
    }

    /**
     * Returns the managed images directory, creating it on first use.
     * The directory lives under the user's home folder so images survive
     * rebuilds of the project and are not polluted into {@code target/}.
     */
    public Path getImagesDir() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), "TourPlanner", "images");
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Copies {@code sourceFile} into the managed images directory under a new
     * UUID-based name and returns the absolute path of the resulting file.
     *
     * @param sourceFile an already-existing file on disk; {@code null} is
     *                   treated as "nothing to store" and returns {@code null}
     * @return absolute path to the stored copy, or {@code null} if the input
     *         was {@code null}
     * @throws IOException if the directory cannot be created or the copy fails
     */
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
