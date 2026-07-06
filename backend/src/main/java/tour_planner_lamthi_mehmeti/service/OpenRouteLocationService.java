package tour_planner_lamthi_mehmeti.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/** LocationService wrapper around OpenRouteService. */
@Service
public class OpenRouteLocationService implements LocationService {

    private final OpenRouteService openRouteService;

    public OpenRouteLocationService(OpenRouteService openRouteService) {
        this.openRouteService = openRouteService;
    }

    @Override
    public List<String> getSuggestions(String query) {
        return openRouteService.getSuggestions(query);
    }

    @Override
    public boolean isValidLocation(String location) throws IOException {
        return openRouteService.isValidLocation(location);
    }
}
