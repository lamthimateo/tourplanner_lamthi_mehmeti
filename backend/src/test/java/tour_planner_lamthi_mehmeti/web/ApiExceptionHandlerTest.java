package tour_planner_lamthi_mehmeti.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tour_planner_lamthi_mehmeti.exception.TourLogNotFoundException;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApiExceptionHandler}.
 *
 * These tests exercise every handler method on the advice and verify both the
 * HTTP status code and the response body shape, so that the REST contract for
 * errors does not drift silently.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void validationErrorReturns400WithFieldMessage() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        binding.addError(new FieldError("target", "name", "must not be blank"));
        Method method = Dummy.class.getDeclaredMethod("noop");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, -1), binding);

        ResponseEntity<Map<String, Object>> response = handler.validationError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation Error", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("name"));
        assertTrue(response.getBody().get("message").toString().contains("must not be blank"));
    }

    @Test
    void notFoundForTourReturns404() {
        ResponseEntity<Map<String, Object>> response = handler.notFound(new TourNotFoundException(123L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
    }

    @Test
    void notFoundForTourLogReturns404() {
        ResponseEntity<Map<String, Object>> response = handler.notFound(new TourLogNotFoundException(7L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void illegalArgumentReturns400() {
        ResponseEntity<Map<String, Object>> response = handler.badRequest(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad", response.getBody().get("message"));
    }

    @Test
    void illegalStateReturns409() {
        ResponseEntity<Map<String, Object>> response = handler.conflict(new IllegalStateException("dup"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    void genericExceptionReturns500() {
        ResponseEntity<Map<String, Object>> response = handler.serverError(new RuntimeException("kaboom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
    }

    @Test
    void genericExceptionHandlesNullMessage() {
        ResponseEntity<Map<String, Object>> response = handler.serverError(new RuntimeException());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error", response.getBody().get("message"));
    }

    /** Placeholder used only to build a valid {@link MethodParameter} for the test. */
    @SuppressWarnings("unused")
    static class Dummy {
        void noop() {}
    }
}
