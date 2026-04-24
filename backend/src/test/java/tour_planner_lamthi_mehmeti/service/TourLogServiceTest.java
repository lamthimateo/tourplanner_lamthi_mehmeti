package tour_planner_lamthi_mehmeti.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tour_planner_lamthi_mehmeti.exception.TourLogNotFoundException;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TourLogService}.
 *
 * <p>The bulk of the suite focuses on the <b>ownership / tenancy
 * checks</b> that this service enforces: every CRUD path must fail when
 * the parent tour belongs to a different user, and must fail when a log
 * ID is passed that belongs to another tour. Those two invariants were
 * the critical security hardening added late in the project.
 *
 * <p>{@link #setUp()} populates a Spring {@link SecurityContextHolder}
 * with a fake {@code UsernamePasswordAuthenticationToken} so that
 * {@code AuthContext.getCurrentUserId()} resolves to {@link #USER_ID};
 * {@link #tearDown()} clears it again so tests don't leak context.
 */
public class TourLogServiceTest {

    private static final Long USER_ID = 1L;

    private TourLogRepository logRepo;
    private TourRepository tourRepo;
    private TourLogService service;

    private static TourLog log(Long id) {
        TourLog l = new TourLog();
        l.setId(id);
        l.setLogDate(LocalDate.now());
        l.setComment("ok");
        l.setDifficulty(2);
        l.setTotalDistance(5.0);
        l.setTotalTimeMinutes(30);
        l.setRating(7);
        return l;
    }

    private static Tour tour(Long id) {
        Tour t = new Tour();
        t.setId(id);
        t.setName("Tour");
        t.setOrigin("A");
        t.setDestination("B");
        t.setTransportType("car");
        t.setUserId(USER_ID);
        return t;
    }

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("user", USER_ID, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        logRepo = mock(TourLogRepository.class);
        tourRepo = mock(TourRepository.class);
        service = new TourLogService(logRepo, tourRepo);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllTourLogsReturnsByTourId() {
        Tour t = tour(10L);
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findByTourId(10L)).thenReturn(List.of(log(1L)));

        List<TourLog> result = service.getAllTourLogs(10L);
        assertEquals(1, result.size());
    }

    @Test
    void getAllTourLogsThrowsWhenTourNotOwned() {
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.empty());
        assertThrows(TourNotFoundException.class, () -> service.getAllTourLogs(10L));
        verify(logRepo, never()).findByTourId(anyLong());
    }

    @Test
    void createTourLogSetsTourAndSaves() {
        Tour t = tour(10L);
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TourLog l = log(null);
        TourLog saved = service.createTourLog(10L, l);

        assertEquals(t, saved.getTour());
        verify(logRepo).save(l);
    }

    @Test
    void createTourLogThrowsWhenTourNotOwned() {
        when(tourRepo.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());
        assertThrows(TourNotFoundException.class, () -> service.createTourLog(99L, log(null)));
    }

    @Test
    void updateTourLogThrowsWhenLogNotFound() {
        Tour t = tour(10L);
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findById(5L)).thenReturn(Optional.empty());
        assertThrows(TourLogNotFoundException.class, () -> service.updateTourLog(10L, log(5L)));
    }

    @Test
    void updateTourLogThrowsWhenLogBelongsToDifferentTour() {
        Tour t = tour(10L);
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(t));
        TourLog existing = log(5L);
        Tour other = tour(999L);
        existing.setTour(other);
        when(logRepo.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(TourLogNotFoundException.class, () -> service.updateTourLog(10L, log(5L)));
        verify(logRepo, never()).save(any());
    }

    @Test
    void updateTourLogSavesWhenValid() {
        Tour t = tour(10L);
        TourLog existing = log(5L);
        existing.setTour(t);
        when(tourRepo.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findById(5L)).thenReturn(Optional.of(existing));
        TourLog updated = log(5L);
        when(logRepo.save(updated)).thenReturn(updated);

        TourLog result = service.updateTourLog(10L, updated);
        assertEquals(t, result.getTour());
        verify(logRepo).save(updated);
    }

    @Test
    void deleteTourLogThrowsWhenNotFound() {
        Tour t = tour(1L);
        when(tourRepo.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TourLogNotFoundException.class, () -> service.deleteTourLog(1L, 99L));
        verify(logRepo, never()).deleteById(any());
    }

    @Test
    void deleteTourLogDeletesWhenFound() {
        Tour t = tour(1L);
        TourLog existing = log(5L);
        existing.setTour(t);
        when(tourRepo.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findById(5L)).thenReturn(Optional.of(existing));

        service.deleteTourLog(1L, 5L);
        verify(logRepo).deleteById(5L);
    }

    @Test
    void deleteTourLogRejectsLogFromDifferentTour() {
        Tour t = tour(1L);
        TourLog existing = log(5L);
        existing.setTour(tour(999L));
        when(tourRepo.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(t));
        when(logRepo.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(TourLogNotFoundException.class, () -> service.deleteTourLog(1L, 5L));
        verify(logRepo, never()).deleteById(any());
    }
}
