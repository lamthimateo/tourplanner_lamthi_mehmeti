package tour_planner_lamthi_mehmeti.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tour_planner_lamthi_mehmeti.dto.StatsDto;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StatsService} — the "unique feature" of the app.
 *
 * These tests verify that aggregate metrics are computed correctly from a
 * realistic mix of tours, logs, and transport types, without needing a
 * database or a Spring context.
 */
class StatsServiceTest {

    private static final Long USER_ID = 7L;

    private TourRepository tourRepo;
    private TourLogRepository logRepo;
    private StatsService service;

    private static Tour tour(Long id, String type) {
        Tour t = new Tour();
        t.setId(id);
        t.setName("Tour " + id);
        t.setOrigin("A");
        t.setDestination("B");
        t.setTransportType(type);
        t.setUserId(USER_ID);
        return t;
    }

    private static TourLog log(double distance, int minutes, int rating) {
        TourLog l = new TourLog();
        l.setLogDate(LocalDate.now());
        l.setComment("c");
        l.setDifficulty(3);
        l.setTotalDistance(distance);
        l.setTotalTimeMinutes(minutes);
        l.setRating(rating);
        return l;
    }

    @BeforeEach
    void setUp() {
        tourRepo = mock(TourRepository.class);
        logRepo = mock(TourLogRepository.class);
        service = new StatsService(tourRepo, logRepo);

        var auth = new UsernamePasswordAuthenticationToken("u", USER_ID, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void emptyDataProducesZeroTotals() {
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of());

        StatsDto stats = service.getStats();

        assertEquals(0, stats.getTotalTours());
        assertEquals(0, stats.getTotalLogs());
        assertEquals(0.0, stats.getTotalDistanceKm());
        assertEquals(0.0, stats.getTotalTimeHours());
        assertEquals(0.0, stats.getAvgRating());
        assertTrue(stats.getByTransportType().isEmpty());
    }

    @Test
    void sumsAndAveragesAcrossAllLogs() {
        Tour car = tour(1L, "car");
        Tour bike = tour(2L, "bicycle");
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of(car, bike));
        when(logRepo.findByTourId(1L)).thenReturn(List.of(log(10.0, 60, 8), log(20.0, 120, 6)));
        when(logRepo.findByTourId(2L)).thenReturn(List.of(log(5.0, 30, 10)));

        StatsDto stats = service.getStats();

        assertEquals(2, stats.getTotalTours());
        assertEquals(3, stats.getTotalLogs());
        assertEquals(35.0, stats.getTotalDistanceKm()); // 10 + 20 + 5
        assertEquals(3.5, stats.getTotalTimeHours());  // 210 min / 60 = 3.5
        assertEquals(8.0, stats.getAvgRating());       // (8 + 6 + 10) / 3
    }

    @Test
    void transportBreakdownIsSortedByTourCountDescending() {
        Tour car1 = tour(1L, "car");
        Tour car2 = tour(2L, "car");
        Tour bike = tour(3L, "bicycle");
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of(car1, car2, bike));
        when(logRepo.findByTourId(anyLong())).thenReturn(List.of());

        StatsDto stats = service.getStats();

        assertEquals(2, stats.getByTransportType().size());
        assertEquals("car", stats.getByTransportType().get(0).getTransportType());
        assertEquals(2, stats.getByTransportType().get(0).getTourCount());
        assertEquals("bicycle", stats.getByTransportType().get(1).getTransportType());
        assertEquals(1, stats.getByTransportType().get(1).getTourCount());
    }

    @Test
    void nullFieldsDoNotBreakAggregation() {
        Tour t = tour(1L, "walking");
        TourLog incomplete = new TourLog();
        incomplete.setLogDate(LocalDate.now());
        incomplete.setComment("c");
        // No distance, no time, no rating — must not NPE or skew averages
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of(t));
        when(logRepo.findByTourId(1L)).thenReturn(List.of(incomplete));

        StatsDto stats = service.getStats();

        assertEquals(1, stats.getTotalTours());
        assertEquals(1, stats.getTotalLogs());
        assertEquals(0.0, stats.getTotalDistanceKm());
        assertEquals(0.0, stats.getTotalTimeHours());
        assertEquals(0.0, stats.getAvgRating());
    }

    @Test
    void unknownTransportTypeFallsBackToUnknownBucket() {
        Tour t = tour(1L, null);
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of(t));
        when(logRepo.findByTourId(1L)).thenReturn(List.of());

        StatsDto stats = service.getStats();

        assertEquals(1, stats.getByTransportType().size());
        assertEquals("unknown", stats.getByTransportType().get(0).getTransportType());
    }
}
