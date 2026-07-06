package tour_planner_lamthi_mehmeti.service;

import java.io.IOException;
import java.util.List;

/** Geocoding interface — implemented by OpenRouteLocationService. */
public interface LocationService {

    List<String> getSuggestions(String query);

    boolean isValidLocation(String location) throws IOException;
}
