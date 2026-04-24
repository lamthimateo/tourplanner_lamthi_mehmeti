package tour_planner_lamthi_mehmeti.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HTTP adapter around the <a href="https://openrouteservice.org/">OpenRouteService</a>
 * (ORS) API. Provides routing (start/end → GeoJSON route) and geocoding
 * (free-text location → coordinates / suggestions) for the rest of the
 * application.
 *
 * <p>Design notes:
 * <ul>
 *   <li>The API key is read from the {@code ORS_API_KEY} environment variable
 *       (resolved through Spring's property binder at
 *       {@code ${openrouteservice.api.key}}). The key is never logged — we
 *       only log its length on startup so operators can confirm it was loaded.</li>
 *   <li>Every public method runs {@link #ensureApiKey()} first so that a
 *       missing key surfaces as a clean 400 Bad Request instead of a
 *       stack-trace 500.</li>
 *   <li>We use Apache HttpClient 4 directly rather than Spring's
 *       {@code RestTemplate} to keep the dependency footprint minimal and
 *       because the streaming {@code try-with-resources} pattern maps cleanly
 *       to the ORS request/response lifecycle.</li>
 * </ul>
 */
@Service
public class OpenRouteService {
    private static final Logger logger = LogManager.getLogger(OpenRouteService.class);

    /** Routing endpoint — returns a GeoJSON line for the requested profile. */
    private final String BASE_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    /** Geocoding endpoint — free-text location lookup. */
    private final String GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";

    /** Injected from {@code ORS_API_KEY} / {@code openrouteservice.api.key}. */
    @Value("${openrouteservice.api.key:}")
    private String apiKey;

    /**
     * One-shot startup check: do not print the key itself (it's a secret)
     * but do print whether one was supplied so operators can spot a
     * misconfigured {@code .env} in the log within seconds.
     */
    @PostConstruct
    void logKeyPresence() {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenRouteService API key not configured (ORS_API_KEY). Routing/geocoding endpoints will fail.");
        } else {
            logger.info("OpenRouteService API key loaded (length={}).", apiKey.trim().length());
        }
    }

    /**
     * Guard clause used at the top of every API call. Throws an
     * {@link IllegalArgumentException} (→ HTTP 400) when the app was started
     * without a key — this is a configuration error, not a server bug.
     */
    private void ensureApiKey() {
        if (apiKey == null || apiKey.trim().isBlank()) {
            throw new IllegalArgumentException(
                    "OpenRouteService API key is missing. Set ORS_API_KEY in backend/.env (or env vars) to enable routing/geocoding.");
        }
    }

    /**
     * Returns the raw GeoJSON route document for a driving-car route between
     * two lat/lng pairs. Coordinates are emitted in ORS's {@code lon,lat}
     * order, not the conventional {@code lat,lng}.
     *
     * @return Jackson tree node — the frontend knows how to traverse it
     */
    public JsonNode getRoute(double startLat, double startLng, double endLat, double endLng) throws IOException {
        ensureApiKey();

        // ORS expects "lon,lat" (GeoJSON convention). Use Locale.US so that
        // systems with comma decimal separators don't break the URL.
        String start = String.format(Locale.US, "%f,%f", startLng, startLat);
        String end = String.format(Locale.US, "%f,%f", endLng, endLat);

        String url = String.format(Locale.US,
                "%s?api_key=%s&start=%s&end=%s",
                BASE_URL, apiKey, start, end
        );

        logger.info("Requesting route with URL: {}", url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(jsonResponse);
            }
        }
    }

    /**
     * Geocodes a free-text location to a {@code [lat, lng]} pair.
     *
     * @return the coordinates, or {@code null} when ORS cannot match the text
     */
    public double[] getCoordinates(String location) throws IOException {
        ensureApiKey();
        // Simple space encoding is enough for the city/address names users type;
        // ORS tolerates the rest (diacritics etc.) without URL-encoding.
        String url = String.format("%s?api_key=%s&text=%s", GEOCODE_URL, apiKey, location.replace(" ", "%20"));
        logger.info("Requesting coordinates for location: {}", location);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonResponse);
                JsonNode features = node.path("features");

                if (features.isArray() && features.size() > 0) {
                    // ORS returns [lon, lat] — we flip back to the more
                    // conventional [lat, lon] order for our callers.
                    JsonNode coordinates = features.get(0).path("geometry").path("coordinates");
                    double longitude = coordinates.get(0).asDouble();
                    double latitude = coordinates.get(1).asDouble();
                    return new double[]{latitude, longitude};
                } else {
                    logger.warn("No coordinates found for location: {}", location);
                    return null;
                }
            }
        }
    }

    /**
     * Lightweight "does this location exist?" check — used by tests and the
     * import validator to bail out before hitting the routing API.
     */
    public boolean isValidLocation(String location) throws IOException {
        ensureApiKey();
        String url = String.format("%s?api_key=%s&text=%s", GEOCODE_URL, apiKey, location.replace(" ", "%20"));
        logger.info("Validating location: {}", location);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonResponse);
                JsonNode features = node.path("features");

                return features.isArray() && features.size() > 0;
            }
        }
    }

    /**
     * Autocomplete helper used by the city-picker inputs in the UI. Unlike
     * {@link #getCoordinates(String)} this swallows {@link IOException} and
     * returns an empty list, because a failed suggestion lookup must not
     * block the user from typing — they can still submit the form.
     */
    public List<String> getSuggestions(String query) {
        ensureApiKey();
        String url = GEOCODE_URL + "?api_key=" + apiKey + "&text=" + query.replace(" ", "%20");
        logger.info("Fetching suggestions for query: {}", query);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonResponse);
                JsonNode features = node.path("features");

                List<String> suggestions = new ArrayList<>();
                if (features.isArray()) {
                    for (JsonNode feature : features) {
                        String label = feature.path("properties").path("label").asText(null);
                        if (label != null) {
                            suggestions.add(label);
                        }
                    }
                }
                return suggestions;
            }
        } catch (IOException e) {
            // Don't propagate: suggestions are a UX nicety, not a correctness
            // requirement. An empty list simply means "no dropdown this time".
            logger.error("Failed to fetch suggestions for query: {}", query, e);
            return List.of();
        }
    }
}
