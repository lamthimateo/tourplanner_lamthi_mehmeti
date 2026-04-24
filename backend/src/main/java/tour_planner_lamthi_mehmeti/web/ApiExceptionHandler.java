package tour_planner_lamthi_mehmeti.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tour_planner_lamthi_mehmeti.exception.TourLogNotFoundException;
import tour_planner_lamthi_mehmeti.exception.TourNotFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global REST exception translator.
 *
 * <p>{@code @RestControllerAdvice} makes every {@code @ExceptionHandler} method
 * in this class apply to <em>all</em> controllers in the application. The job
 * of the handler is to convert thrown Java exceptions into well-shaped JSON
 * error bodies with correct HTTP status codes — so controllers never have to
 * catch exceptions manually and clients always get a predictable payload.
 *
 * <p>Error body shape:
 * <pre>
 *   {
 *     "timestamp": "2026-04-22T12:34:56Z",
 *     "status":    404,
 *     "error":     "Not Found",
 *     "message":   "Tour not found with id: 99"
 *   }
 * </pre>
 *
 * <p>Status mapping:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → 400 Validation Error</li>
 *   <li>{@link IllegalArgumentException}        → 400 Bad Request</li>
 *   <li>{@link IllegalStateException}           → 409 Conflict</li>
 *   <li>{@link TourNotFoundException}/{@link TourLogNotFoundException} → 404 Not Found</li>
 *   <li>Anything else (catch-all)               → 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Triggered when a {@code @Valid}-annotated request body fails Bean
     * Validation. We join every field error into a single readable sentence
     * so the frontend can show it directly without doing its own formatting.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Validation Error",
                "message", message
        ));
    }

    /** Tour / log lookup misses surface as 404 with the id in the message. */
    @ExceptionHandler({TourNotFoundException.class, TourLogNotFoundException.class})
    public ResponseEntity<Map<String, Object>> notFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    /** General bad input (e.g. ORS returned "location not found"). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    /** Domain conflicts (e.g. registering an existing username) → 409. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    /**
     * Catch-all so no stack trace ever leaks through the HTTP layer; the
     * real exception is still written to the Log4j2 logs for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> serverError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error"
        ));
    }
}
