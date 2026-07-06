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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calls OpenRouteService for routing and geocoding. API key comes from ORS_API_KEY env var.
 */
@Service
public class OpenRouteService {
    private static final Logger logger = LogManager.getLogger(OpenRouteService.class);

    private static final String DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/";
    private final String GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";

    @Value("${openrouteservice.api.key:}")
    private String apiKey;

    @PostConstruct
    void logKeyPresence() {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenRouteService API key not configured (ORS_API_KEY). Routing/geocoding endpoints will fail.");
        } else {
            logger.info("OpenRouteService API key loaded (length={}).", apiKey.trim().length());
        }
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.trim().isBlank()) {
            throw new IllegalArgumentException(
                    "OpenRouteService API key is missing. Set ORS_API_KEY in backend/.env (or env vars) to enable routing/geocoding.");
        }
    }

    static String toOrsProfile(String transportType) {
        if (transportType == null) return "driving-car";
        return switch (transportType.trim().toLowerCase(Locale.ROOT)) {
            case "bicycle", "bike", "cycling", "cycling-regular" -> "cycling-regular";
            case "walking", "running", "foot-walking" -> "foot-walking";
            case "hiking", "foot-hiking" -> "foot-hiking";
            default -> "driving-car";
        };
    }

    private static String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void ensureHttpSuccess(CloseableHttpResponse response, String body) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            return;
        }
        String snippet = body == null ? "" : (body.length() > 240 ? body.substring(0, 240) + "…" : body);
        throw new IOException("OpenRouteService HTTP " + code + (snippet.isBlank() ? "" : ": " + snippet));
    }

    public JsonNode getRoute(double startLat, double startLng, double endLat, double endLng) throws IOException {
        return getRoute(startLat, startLng, endLat, endLng, null);
    }

    public JsonNode getRoute(double startLat, double startLng, double endLat, double endLng, String transportType) throws IOException {
        ensureApiKey();
        String profile = toOrsProfile(transportType);

        String start = String.format(Locale.US, "%f,%f", startLng, startLat);
        String end = String.format(Locale.US, "%f,%f", endLng, endLat);

        String url = String.format(Locale.US,
                "%s%s?api_key=%s&start=%s&end=%s",
                DIRECTIONS_URL, profile, encodeQueryParam(apiKey), start, end
        );

        logger.info("Requesting {} route from [{}] to [{}]", profile, start, end);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ensureHttpSuccess(response, jsonResponse);
                return new ObjectMapper().readTree(jsonResponse);
            }
        }
    }

    public double[] getCoordinates(String location) throws IOException {
        ensureApiKey();
        String url = GEOCODE_URL + "?api_key=" + encodeQueryParam(apiKey)
                + "&text=" + encodeQueryParam(location);
        logger.info("Requesting coordinates for location: {}", location);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ensureHttpSuccess(response, jsonResponse);

                JsonNode features = new ObjectMapper().readTree(jsonResponse).path("features");
                if (features.isArray() && !features.isEmpty()) {
                    JsonNode coordinates = features.get(0).path("geometry").path("coordinates");
                    double longitude = coordinates.get(0).asDouble();
                    double latitude = coordinates.get(1).asDouble();
                    return new double[]{latitude, longitude};
                }
                logger.warn("No coordinates found for location: {}", location);
                return null;
            }
        }
    }

    public boolean isValidLocation(String location) throws IOException {
        ensureApiKey();
        String url = GEOCODE_URL + "?api_key=" + encodeQueryParam(apiKey)
                + "&text=" + encodeQueryParam(location);
        logger.info("Validating location: {}", location);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ensureHttpSuccess(response, jsonResponse);
                JsonNode features = new ObjectMapper().readTree(jsonResponse).path("features");
                return features.isArray() && !features.isEmpty();
            }
        }
    }

    public List<String> getSuggestions(String query) {
        ensureApiKey();
        String url = GEOCODE_URL + "?api_key=" + encodeQueryParam(apiKey)
                + "&text=" + encodeQueryParam(query);
        logger.info("Fetching suggestions for query: {}", query);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ensureHttpSuccess(response, jsonResponse);
                JsonNode features = new ObjectMapper().readTree(jsonResponse).path("features");

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
            logger.error("Failed to fetch suggestions for query: {}", query, e);
            return List.of();
        }
    }
}
