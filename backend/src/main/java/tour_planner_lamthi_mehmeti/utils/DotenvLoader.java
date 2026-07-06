package tour_planner_lamthi_mehmeti.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads KEY=VALUE lines from .env into JVM system properties.
 * Needed when running from IntelliJ (run.sh is not used).
 */
public final class DotenvLoader {

    private static final Logger log = LogManager.getLogger(DotenvLoader.class);

    private DotenvLoader() {
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

                // Java can't set OS env vars at runtime; system properties work with Spring Boot.
                System.setProperty(key, value);
            }
        }
    }
}
