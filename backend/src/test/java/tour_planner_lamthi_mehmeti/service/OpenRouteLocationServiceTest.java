package tour_planner_lamthi_mehmeti.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for OpenRouteLocationService (mocked ORS client). */
class OpenRouteLocationServiceTest {

    @Test
    void getSuggestionsDelegatesToUnderlyingClient() {
        OpenRouteService ors = mock(OpenRouteService.class);
        when(ors.getSuggestions("Vien")).thenReturn(List.of("Vienna, Austria"));

        OpenRouteLocationService adapter = new OpenRouteLocationService(ors);
        List<String> suggestions = adapter.getSuggestions("Vien");

        assertEquals(1, suggestions.size());
        assertEquals("Vienna, Austria", suggestions.get(0));
        verify(ors).getSuggestions("Vien");
    }

    @Test
    void isValidLocationDelegatesReturnValue() throws IOException {
        OpenRouteService ors = mock(OpenRouteService.class);
        when(ors.isValidLocation("Vienna")).thenReturn(true);
        when(ors.isValidLocation("Atlantis, Mu")).thenReturn(false);

        OpenRouteLocationService adapter = new OpenRouteLocationService(ors);

        assertTrue(adapter.isValidLocation("Vienna"));
        assertFalse(adapter.isValidLocation("Atlantis, Mu"));
    }

    @Test
    void isValidLocationPropagatesIoException() throws IOException {
        OpenRouteService ors = mock(OpenRouteService.class);
        when(ors.isValidLocation("boom")).thenThrow(new IOException("network down"));

        OpenRouteLocationService adapter = new OpenRouteLocationService(ors);

        IOException ex = assertThrows(IOException.class, () -> adapter.isValidLocation("boom"));
        assertEquals("network down", ex.getMessage());
    }
}
