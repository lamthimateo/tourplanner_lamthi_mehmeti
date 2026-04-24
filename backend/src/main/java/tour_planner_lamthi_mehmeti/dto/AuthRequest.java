package tour_planner_lamthi_mehmeti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) carrying the credentials sent by a client when it
 * calls {@code POST /api/auth/register} or {@code POST /api/auth/login}.
 *
 * <p>This class is deliberately separate from the {@code User} JPA entity: the
 * network shape must not leak persistence concerns (id, foreign keys, BCrypt
 * hash, etc.). A DTO also lets us attach Bean Validation constraints that are
 * specific to the API call without polluting the domain model.
 *
 * <p>Validation: both fields are annotated with {@link NotBlank} and {@link Size};
 * when the request body fails these checks Spring throws
 * {@code MethodArgumentNotValidException}, which is translated by
 * {@code ApiExceptionHandler} into a 400 response with a human-readable message.
 */
public class AuthRequest {

    /** Desired / existing username. 3–50 chars, non-blank. */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Plain-text password as typed by the user. The server never stores this
     * value directly — {@code AuthService} hashes it with BCrypt before saving
     * the {@code User} row. 4–100 chars, non-blank.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    private String password;

    /** Default constructor required by Jackson for JSON deserialization. */
    public AuthRequest() {
    }

    /**
     * Convenience constructor used by unit tests to build a valid request in
     * a single line.
     */
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** @return the submitted username */
    public String getUsername() {
        return username;
    }

    /** @param username the submitted username */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return the submitted (plain-text) password */
    public String getPassword() {
        return password;
    }

    /** @param password the submitted (plain-text) password */
    public void setPassword(String password) {
        this.password = password;
    }
}
