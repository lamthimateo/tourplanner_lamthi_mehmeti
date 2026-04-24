package tour_planner_lamthi_kiri_puka.service;

import java.io.IOException;
import java.util.List;

/**
 * Business-layer abstraction for location-related operations.
 *
 * Defines the contract for geocoding support needed by the application without
 * exposing the underlying HTTP provider (OpenRouteService).  Any code that needs
 * location features should depend on this interface, not on the concrete
 * {@link OpenRouteService} class directly.
 *
 * <p>Current implementation: {@link OpenRouteLocationService} (backed by ORS REST API).</p>
 *
 * <p>Checklist requirement: "Layers only call methods of the immediate layer below" —
 * this interface ensures the service layer defines its own abstraction rather than
 * leaking infrastructure types upward.</p>
 */
public interface LocationService {

    /**
     * Returns a list of location name suggestions matching the given partial query.
     * Intended for autocomplete UI features.
     *
     * @param query Partial search string (e.g. "Vien").
     * @return List of matching location strings (may be empty, never null).
     */
    List<String> getSuggestions(String query);

    /**
     * Returns {@code true} if the given location string can be successfully resolved
     * to geographic coordinates by the underlying geocoding provider.
     *
     * @param location A full or partial location string (e.g. "Vienna, Austria").
     * @return {@code true} if at least one result is found; {@code false} otherwise.
     * @throws IOException if the geocoding request fails due to a network error.
     */
    boolean isValidLocation(String location) throws IOException;
}
