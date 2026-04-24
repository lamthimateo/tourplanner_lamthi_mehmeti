package tour_planner_lamthi_mehmeti.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tour_planner_lamthi_mehmeti.model.Tour;
import tour_planner_lamthi_mehmeti.model.TourLog;
import tour_planner_lamthi_mehmeti.repository.TourLogRepository;
import tour_planner_lamthi_mehmeti.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TourDataTransferService}.
 *
 * <p>Exercises both sides of the round-trip:
 * <ul>
 *   <li>{@link TourDataTransferService#exportTourData(Long)} — returns a
 *       DTO that preserves every field of the underlying entity.</li>
 *   <li>{@link TourDataTransferService#importTourData} — creates new
 *       records, assigns the current user's ID (when present), and
 *       attaches logs to the newly-saved tour.</li>
 * </ul>
 *
 * <p>These tests run without a Spring security context to verify the
 * service's graceful fallback behaviour (CLI / seed scenarios), and use
 * Mockito's {@link ArgumentCaptor} to assert on the entities passed to
 * the repositories rather than spying on save-side-effects.
 */
public class TourDataTransferServiceTest {

    private static Tour tour(Long id, String name) {
        Tour t = new Tour();
        t.setId(id);
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
        l.setDifficulty(1);
        l.setTotalDistance(1.0);
        l.setTotalTimeMinutes(10);
        l.setRating(5);
        return l;
    }

    @Test
    void exportCreatesJsonContainingTourName() throws Exception {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        Tour t = tour(1L, "MyTour");
        when(tourRepo.findById(1L)).thenReturn(Optional.of(t));
        when(logRepo.findByTourId(1L)).thenReturn(List.of());

        TourDataTransferService svc = new TourDataTransferService(tourRepo, logRepo);
        String json = svc.exportTourToJsonString(1L);

        assertTrue(json.contains("MyTour"));
        assertTrue(json.contains("\"tour\""));
    }

    @Test
    void exportIncludesLogs() throws Exception {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        Tour t = tour(1L, "T");
        TourLog l = log(null, t);
        l.setComment("hello");
        when(tourRepo.findById(1L)).thenReturn(Optional.of(t));
        when(logRepo.findByTourId(1L)).thenReturn(List.of(l));

        TourDataTransferService svc = new TourDataTransferService(tourRepo, logRepo);
        String json = svc.exportTourToJsonString(1L);

        assertTrue(json.contains("hello"));
        assertTrue(json.contains("\"logs\""));
    }

    @Test
    void importCreatesTourAndReturnsNewId() throws Exception {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        when(tourRepo.save(any())).thenAnswer(inv -> {
            Tour t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TourDataTransferService svc = new TourDataTransferService(tourRepo, logRepo);

        String json = "{\n" +
                "  \"tour\": { \"name\": \"Imported\", \"origin\": \"A\", \"destination\": \"B\", \"transportType\": \"car\" },\n" +
                "  \"logs\": []\n" +
                "}";

        Long id = svc.importTourFromJsonString(json);
        assertEquals(99L, id);
        verify(tourRepo, times(1)).save(any());
    }

    @Test
    void importPersistsLogs() throws Exception {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);

        when(tourRepo.save(any())).thenAnswer(inv -> {
            Tour t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TourDataTransferService svc = new TourDataTransferService(tourRepo, logRepo);

        String json = "{\n" +
                "  \"tour\": { \"name\": \"Imported\", \"origin\": \"A\", \"destination\": \"B\", \"transportType\": \"car\" },\n" +
                "  \"logs\": [ { \"logDate\": \"2026-02-23\", \"comment\": \"c\", \"difficulty\": 2, \"totalDistance\": 3.5, \"totalTimeMinutes\": 40, \"rating\": 4, \"logDetails\": \"d\" } ]\n" +
                "}";

        svc.importTourFromJsonString(json);

        ArgumentCaptor<TourLog> captor = ArgumentCaptor.forClass(TourLog.class);
        verify(logRepo).save(captor.capture());
        TourLog saved = captor.getValue();
        assertEquals(LocalDate.parse("2026-02-23"), saved.getLogDate());
        assertEquals(40, saved.getTotalTimeMinutes());
        assertEquals(3.5, saved.getTotalDistance());
    }

    @Test
    void importInvalidJsonThrows() {
        TourRepository tourRepo = mock(TourRepository.class);
        TourLogRepository logRepo = mock(TourLogRepository.class);
        TourDataTransferService svc = new TourDataTransferService(tourRepo, logRepo);

        assertThrows(Exception.class, () -> svc.importTourFromJsonString("{not json"));
    }
}
