package tour_planner_lamthi_kiri_puka.service;

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

@Service
public class OpenRouteService {
    private static final Logger logger = LogManager.getLogger(OpenRouteService.class);
    private final String BASE_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private final String GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";
    @Value("${openrouteservice.api.key:}")
    private String apiKey;

    @PostConstruct
    void logKeyPresence() {
        // Do not log the key itself (secret). Only log whether it was provided.
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenRouteService API key not configured (ORS_API_KEY). Routing/geocoding endpoints will fail.");
        } else {
            logger.info("OpenRouteService API key loaded (length={}).", apiKey.trim().length());
        }
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.trim().isBlank()) {
            // Keep the app usable without ORS: endpoints that rely on ORS should return
            // a clean client error, not crash the server with a stacktrace.
            throw new IllegalArgumentException("OpenRouteService API key is missing. Set ORS_API_KEY in backend/.env (or env vars) to enable routing/geocoding.");
        }
    }

    public JsonNode getRoute(double startLat, double startLng, double endLat, double endLng) throws IOException {
        ensureApiKey();

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

    public double[] getCoordinates(String location) throws IOException {
        ensureApiKey();
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
            logger.error("Failed to fetch suggestions for query: {}", query, e);
            return List.of();
        }
    }
}
