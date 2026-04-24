package tour_planner_lamthi_mehmeti.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Concrete implementation of {@link LocationService} backed by the OpenRouteService API.
 *
 * This class acts as an adapter between the business-layer {@link LocationService} interface
 * and the infrastructure-layer {@link OpenRouteService}.  Controllers and other services
 * that need location features should depend on {@link LocationService} (the interface),
 * not directly on {@link OpenRouteService}.  This makes it easy to swap the geocoding
 * provider in the future without touching any call sites.
 *
 * <p>Design pattern: <b>Adapter</b> — wraps OpenRouteService to satisfy the LocationService
 * interface contract.</p>
 */
@Service
public class OpenRouteLocationService implements LocationService {

    /** The low-level ORS client that performs the actual HTTP calls. */
    private final OpenRouteService openRouteService;

    /**
     * @param openRouteService Spring-injected ORS HTTP client.
     */
    public OpenRouteLocationService(OpenRouteService openRouteService) {
        this.openRouteService = openRouteService;
    }

    /**
     * Delegates autocomplete suggestion fetching to the ORS geocoding endpoint.
     *
     * @param query Partial location string typed by the user (e.g. "Vien").
     * @return List of matching location name strings from ORS.
     */
    @Override
    public List<String> getSuggestions(String query) {
        return openRouteService.getSuggestions(query);
    }

    /**
     * Checks whether the given location string can be resolved to coordinates
     * by delegating to the ORS geocoding endpoint.
     *
     * @param location Full location string to validate (e.g. "Vienna, Austria").
     * @return {@code true} if ORS returns at least one matching feature; {@code false} otherwise.
     * @throws IOException if the ORS HTTP request fails.
     */
    @Override
    public boolean isValidLocation(String location) throws IOException {
        return openRouteService.isValidLocation(location);
    }
}
