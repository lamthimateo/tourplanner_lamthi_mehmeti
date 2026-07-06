package tour_planner_lamthi_mehmeti.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** Unit tests for TourService CRUD. Repositories are mocked; SecurityContext fakes a logged-in user. */
public class TourServiceCrudTest {

    private static final Long USER_ID = 42L;
    private TourRepository tourRepo;
    private TourLogRepository logRepo;
    private TourService service;

    private static Tour tour(Long id) {
        Tour t = new Tour();
        t.setId(id);
        t.setName("Tour");
        t.setOrigin("A");
        t.setDestination("B");
        t.setTransportType("car");
        return t;
    }

    private static void inject(TourService svc, TourRepository tourRepo, TourLogRepository logRepo) {
        try {
            var f1 = TourService.class.getDeclaredField("tourRepository");
            f1.setAccessible(true);
            f1.set(svc, tourRepo);
            var f2 = TourService.class.getDeclaredField("tourLogRepository");
            f2.setAccessible(true);
            f2.set(svc, logRepo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("user", USER_ID, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        tourRepo = mock(TourRepository.class);
        logRepo = mock(TourLogRepository.class);
        service = new TourService();
        inject(service, tourRepo, logRepo);
    }

    @Test
    void getAllToursReturnsToursBelongingToCurrentUser() {
        when(tourRepo.findByUserId(USER_ID)).thenReturn(List.of(tour(1L), tour(2L)));
        List<Tour> result = service.getAllTours();
        assertEquals(2, result.size());
        verify(tourRepo).findByUserId(USER_ID);
    }

    @Test
    void getTourByIdReturnsCorrectTour() {
        Tour t = tour(5L);
        when(tourRepo.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(t));
        Tour result = service.getTourById(5L);
        assertEquals(5L, result.getId());
    }

    @Test
    void getTourByIdThrowsWhenNotFound() {
        when(tourRepo.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());
        assertThrows(TourNotFoundException.class, () -> service.getTourById(99L));
    }

    @Test
    void createTourSetsUserIdAndSaves() {
        Tour t = tour(null);
        when(tourRepo.save(any())).thenAnswer(inv -> {
            Tour saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Tour result = service.createTour(t);
        assertEquals(USER_ID, t.getUserId());
        assertEquals(10L, result.getId());
        verify(tourRepo).save(t);
    }

    @Test
    void updateTourThrowsWhenTourDoesNotBelongToUser() {
        Tour t = tour(7L);
        when(tourRepo.existsByIdAndUserId(7L, USER_ID)).thenReturn(false);
        assertThrows(TourNotFoundException.class, () -> service.updateTour(t));
        verify(tourRepo, never()).save(any());
    }

    @Test
    void updateTourSavesWhenOwned() {
        Tour t = tour(7L);
        when(tourRepo.existsByIdAndUserId(7L, USER_ID)).thenReturn(true);
        when(tourRepo.save(t)).thenReturn(t);
        Tour result = service.updateTour(t);
        assertEquals(7L, result.getId());
        verify(tourRepo).save(t);
    }

    @Test
    void deleteTourThrowsWhenNotOwned() {
        when(tourRepo.existsByIdAndUserId(3L, USER_ID)).thenReturn(false);
        assertThrows(TourNotFoundException.class, () -> service.deleteTour(3L));
        verify(tourRepo, never()).deleteById(any());
    }

    @Test
    void deleteTourDeletesWhenOwned() {
        when(tourRepo.existsByIdAndUserId(3L, USER_ID)).thenReturn(true);
        service.deleteTour(3L);
        // Logs must be removed before the tour itself, otherwise the
        // tour_id foreign key on tour_logs would be violated.
        var order = inOrder(logRepo, tourRepo);
        order.verify(logRepo).deleteByTourId(3L);
        order.verify(tourRepo).deleteById(3L);
    }

    @Test
    void computePopularityCountsLogs() {
        when(logRepo.findByTourId(1L)).thenReturn(List.of());
        assertEquals(0, service.computePopularity(1L));

        when(logRepo.findByTourId(2L)).thenReturn(List.of(
                new tour_planner_lamthi_mehmeti.model.TourLog(),
                new tour_planner_lamthi_mehmeti.model.TourLog()
        ));
        assertEquals(2, service.computePopularity(2L));
    }
}
