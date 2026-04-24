package tour_planner_lamthi_mehmeti.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tour_planner_lamthi_mehmeti.dto.AuthRequest;
import tour_planner_lamthi_mehmeti.dto.AuthResponse;
import tour_planner_lamthi_mehmeti.service.AuthService;

/**
 * Public REST controller that exposes the two unauthenticated endpoints
 * needed to bootstrap a session:
 * <ul>
 *   <li>{@code POST /api/auth/register} — creates a new account and returns a token.</li>
 *   <li>{@code POST /api/auth/login}    — verifies credentials and returns a token.</li>
 * </ul>
 *
 * <p>Both endpoints are permitted by {@code SecurityConfig} without a bearer
 * token — they are the only way a new client can obtain one. All heavy
 * lifting (BCrypt hashing, uniqueness checks, JWT minting) lives in
 * {@link AuthService}; this controller is intentionally thin.
 *
 * <p>The {@code @Valid} annotation drives Bean Validation on the request
 * body; violations bubble up as {@code MethodArgumentNotValidException},
 * which {@link ApiExceptionHandler} turns into 400 responses.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Creates a new user + returns a freshly-minted JWT. */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    /** Verifies credentials + returns a freshly-minted JWT. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }
}
