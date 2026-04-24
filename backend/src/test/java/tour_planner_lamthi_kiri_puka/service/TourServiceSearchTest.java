package tour_planner_lamthi_kiri_puka.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.repository.TourLogRepository;
import tour_planner_lamthi_kiri_puka.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TourServiceSearchTest {

    private static final Long TEST_USER_ID = 1L;

    private static Tour tour(Long id, String name) {
        Tour t = new Tour();
        t.setId(id);
        t.setUserId(TEST_USER_ID);
        t.setName(name);
        t.setOrigin("O");
        t.setDestination("D");
        t.setTransportType("car");
        return t;
    }

    private static TourLog log(Long id, Tour tour) {
        TourLog l = new TourLog();
        l.setId(id);
        l.setTour(tour);
        l.setLogDate(LocalDate.now());
        l.setComment("");
        l.setLogDetails("");
        return l;
    }

    private static void inject(TourService service, TourRepository tourRepo, TourLogRepository logRepo) {
        try {
            var f1 = TourService.class.getDeclaredField("tourRepository");
            f1.setAccessible(true);
            f1.set(service, tourRepo);
            var f2 = TourService.class.getDeclaredField("tourLogRepository");
            f2.setAccessible(true);
            f2.set(service, logRepo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUpAuth() {
        var auth = new UsernamePasswordAuthenticationToken("testuser", TEST_USER_ID, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void searchEmptyReturnsAllIds() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(tour(1L, "A"), tour(2L, "B")));
        when(logRepo.findAll()).thenReturn(List.of());

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        List<Long> ids = service.searchTourIds("");
        assertEquals(List.of(1L, 2L), ids);
    }

    @Test
    void searchMatchesTourName() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(tour(1L, "Vienna Trip"), tour(2L, "Salzburg")));
        when(logRepo.findAll()).thenReturn(List.of());

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        assertEquals(List.of(1L), service.searchTourIds("vienna"));
    }

    @Test
    void searchMatchesTourDescription() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        Tour t1 = tour(1L, "T1");
        t1.setDescription("Amazing mountains");
        Tour t2 = tour(2L, "T2");
        t2.setDescription("City walk");

        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(t1, t2));
        when(logRepo.findAll()).thenReturn(List.of());

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        assertEquals(List.of(1L), service.searchTourIds("mountains"));
    }

    @Test
    void searchMatchesLogComment() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        Tour t1 = tour(1L, "T1");
        Tour t2 = tour(2L, "T2");

        TourLog l = log(10L, t2);
        l.setComment("Great weather");
        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(t1, t2));
        when(logRepo.findAll()).thenReturn(List.of(l));

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        assertEquals(List.of(2L), service.searchTourIds("weather"));
    }

    @Test
    void searchMatchesComputedAvgRating() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        Tour t = tour(1L, "T");
        TourLog l1 = log(1L, t);
        l1.setRating(5);
        TourLog l2 = log(2L, t);
        l2.setRating(3);

        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(t));
        when(logRepo.findAll()).thenReturn(List.of(l1, l2));

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        // avg rating = 4.0 -> string contains "4.0"
        assertEquals(List.of(1L), service.searchTourIds("4.0"));
    }

    @Test
    void searchNoMatchesReturnsEmpty() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        when(tourRepo.findByUserId(TEST_USER_ID)).thenReturn(List.of(tour(1L, "T1")));
        when(logRepo.findAll()).thenReturn(List.of());

        TourService service = new TourService();
        inject(service, tourRepo, logRepo);

        assertTrue(service.searchTourIds("zzzz").isEmpty());
    }
}
