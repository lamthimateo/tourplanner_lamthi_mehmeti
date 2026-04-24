package tour_planner_lamthi_mehmeti.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_mehmeti.service.OpenRouteService;

import java.io.IOException;
import java.util.List;

/**
 * REST controller that exposes the mapping / geocoding helpers backed by
 * OpenRouteService (ORS). Acts as a thin server-side proxy so the API key
 * stays on the backend and so frontend code can hit relative URLs on the
 * same origin (avoiding a second CORS dance per request).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/route/suggest?q=vi}   — city-name autocomplete (Nominatim).</li>
 *   <li>{@code GET /api/route/coordinates?location=Vienna} — geocode a free-text location
 *       to {@code [latitude, longitude]}.</li>
 *   <li>{@code GET /api/route?fromLat=...&fromLng=...&toLat=...&toLng=...}
 *       — fetch the GeoJSON route between two coordinate pairs.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/route")
public class RouteController {

    /** Injected adapter that wraps ORS + Nominatim HTTP calls. */
    private final OpenRouteService openRouteService;

    public RouteController(OpenRouteService openRouteService) {
        this.openRouteService = openRouteService;
    }

    /** Returns up to ~5 matching location names for the given prefix. */
    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String q) {
        return openRouteService.getSuggestions(q);
    }

    /**
     * Geocodes a free-text location to a coordinate pair. When ORS cannot
     * match the string we surface the failure as 400 Bad Request so the UI
     * can show a meaningful "Location not found" message instead of a
     * misleading 500.
     */
    @GetMapping("/coordinates")
    public double[] coordinates(@RequestParam("location") String location) throws IOException {
        double[] coords = openRouteService.getCoordinates(location);
        if (coords == null) {
            throw new IllegalArgumentException("Location not found: " + location);
        }
        return coords;
    }

    /** Returns raw ORS GeoJSON — the frontend draws it on the Leaflet map. */
    @GetMapping
    public JsonNode route(@RequestParam("fromLat") double fromLat,
                          @RequestParam("fromLng") double fromLng,
                          @RequestParam("toLat") double toLat,
                          @RequestParam("toLng") double toLng) throws IOException {
        return openRouteService.getRoute(fromLat, fromLng, toLat, toLng);
    }
}
