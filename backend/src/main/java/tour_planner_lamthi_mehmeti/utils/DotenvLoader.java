package tour_planner_lamthi_mehmeti.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal .env loader.
 * <p>
 * Why:
 * - When you run from IntelliJ, run.sh is bypassed.
 * - Spring Boot can read configuration from environment variables *and* JVM system properties.
 * <p>
 * This helper reads KEY=VALUE lines from a .env file in the current working directory
 * and sets them as JVM system properties (only if not already set).
 */
public final class DotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotenvLoader.class);

    private DotenvLoader() {
        // util
    }

    public static void loadFromWorkingDirectoryIfPresent() {
        Path dotenv = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.exists(dotenv)) {
            return;
        }

        try {
            load(dotenv);
            log.info("Loaded .env from {}", dotenv.toAbsolutePath());
        } catch (Exception e) {
            // Don't fail app startup if .env is malformed.
            log.warn("Failed to load .env from {}: {}", dotenv.toAbsolutePath(), e.getMessage());
        }
    }

    private static void load(Path dotenv) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(dotenv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                // Strip optional surrounding quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.isEmpty()) {
                    continue;
                }

                // Don't override explicitly provided values
                if (System.getProperty(key) != null) {
                    continue;
                }

                // NOTE: Java cannot set real OS environment variables at runtime.
                // But Spring Boot treats JVM system properties as a valid config source.
                System.setProperty(key, value);
            }
        }
    }
}
