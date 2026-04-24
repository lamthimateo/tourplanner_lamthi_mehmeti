package tour_planner_lamthi_kiri_puka.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tour_planner_lamthi_kiri_puka.exception.TourLogNotFoundException;
import tour_planner_lamthi_kiri_puka.exception.TourNotFoundException;
import tour_planner_lamthi_kiri_puka.model.Tour;
import tour_planner_lamthi_kiri_puka.model.TourLog;
import tour_planner_lamthi_kiri_puka.repository.TourLogRepository;
import tour_planner_lamthi_kiri_puka.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TourLogServiceTest {

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
        return t;
    }

    @BeforeEach
    void setUp() {
        logRepo = mock(TourLogRepository.class);
        tourRepo = mock(TourRepository.class);
        service = new TourLogService(logRepo, tourRepo);
    }

    @Test
    void getAllTourLogsReturnsByTourId() {
        TourLog l = log(1L);
        when(logRepo.findByTourId(10L)).thenReturn(List.of(l));

        List<TourLog> result = service.getAllTourLogs(10L);
        assertEquals(1, result.size());
    }

    @Test
    void createTourLogSetsTourAndSaves() {
        Tour t = tour(10L);
        when(tourRepo.findById(10L)).thenReturn(Optional.of(t));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TourLog l = log(null);
        TourLog saved = service.createTourLog(10L, l);

        assertEquals(t, saved.getTour());
        verify(logRepo).save(l);
    }

    @Test
    void createTourLogThrowsWhenTourNotFound() {
        when(tourRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TourNotFoundException.class, () -> service.createTourLog(99L, log(null)));
    }

    @Test
    void updateTourLogThrowsWhenLogNotFound() {
        Tour t = tour(10L);
        when(tourRepo.findById(10L)).thenReturn(Optional.of(t));
        TourLog l = log(5L);
        when(logRepo.existsById(5L)).thenReturn(false);
        assertThrows(TourLogNotFoundException.class, () -> service.updateTourLog(10L, l));
    }

    @Test
    void updateTourLogSavesWhenValid() {
        Tour t = tour(10L);
        TourLog l = log(5L);
        when(tourRepo.findById(10L)).thenReturn(Optional.of(t));
        when(logRepo.existsById(5L)).thenReturn(true);
        when(logRepo.save(l)).thenReturn(l);

        TourLog result = service.updateTourLog(10L, l);
        assertEquals(t, result.getTour());
        verify(logRepo).save(l);
    }

    @Test
    void deleteTourLogThrowsWhenNotFound() {
        when(logRepo.existsById(99L)).thenReturn(false);
        assertThrows(TourLogNotFoundException.class, () -> service.deleteTourLog(1L, 99L));
        verify(logRepo, never()).deleteById(any());
    }

    @Test
    void deleteTourLogDeletesWhenFound() {
        when(logRepo.existsById(5L)).thenReturn(true);
        service.deleteTourLog(1L, 5L);
        verify(logRepo).deleteById(5L);
    }
}
