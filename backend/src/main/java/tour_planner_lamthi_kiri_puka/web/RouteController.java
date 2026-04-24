package tour_planner_lamthi_kiri_puka.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_kiri_puka.service.OpenRouteService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final OpenRouteService openRouteService;

    public RouteController(OpenRouteService openRouteService) {
        this.openRouteService = openRouteService;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String q) {
        return openRouteService.getSuggestions(q);
    }

    @GetMapping("/coordinates")
    public double[] coordinates(@RequestParam("location") String location) throws IOException {
        double[] coords = openRouteService.getCoordinates(location);
        if (coords == null) {
            throw new IllegalArgumentException("Location not found: " + location);
        }
        return coords;
    }

    @GetMapping
    public JsonNode route(@RequestParam("fromLat") double fromLat,
                          @RequestParam("fromLng") double fromLng,
                          @RequestParam("toLat") double toLat,
                          @RequestParam("toLng") double toLng) throws IOException {
        return openRouteService.getRoute(fromLat, fromLng, toLat, toLng);
    }
}
