package tour_planner_lamthi_mehmeti.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tour_planner_lamthi_mehmeti.dto.AuthRequest;
import tour_planner_lamthi_mehmeti.dto.AuthResponse;
import tour_planner_lamthi_mehmeti.model.User;
import tour_planner_lamthi_mehmeti.repository.UserRepository;
import tour_planner_lamthi_mehmeti.security.JwtUtil;

/**
 * Service responsible for user authentication and registration.
 *
 * <p>This service implements the two main authentication flows for the Tour
 * Planner application:
 * <ol>
 *   <li><b>Registration</b> – validates username uniqueness, hashes the
 *       password with BCrypt via Spring Security's {@link PasswordEncoder},
 *       persists the new {@link User}, and immediately issues a JWT so the
 *       client is logged in after registration without a separate login
 *       request.</li>
 *   <li><b>Login</b> – looks up the user by username, verifies the supplied
 *       plain-text password against the stored BCrypt hash, and issues a
 *       fresh JWT on success.</li>
 * </ol>
 *
 * <p>Intentionally vague error messages ("Invalid username or password") are
 * used for login failures to avoid revealing whether a given username exists
 * in the system (username enumeration defence).
 *
 * <p>JWT generation is delegated to {@link JwtUtil}, which embeds both the
 * username and the internal user ID as claims so that downstream services can
 * identify the calling user without an extra database round-trip.
 */
@Service
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Constructs an {@code AuthService} with its required collaborators.
     *
     * @param userRepository  JPA repository for {@link User} persistence and
     *                        lookup.
     * @param passwordEncoder Spring Security password encoder (BCrypt) used to
     *                        hash new passwords and verify login attempts.
     * @param jwtUtil         utility class that generates and validates JWT
     *                        tokens.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user account and returns an authentication token.
     *
     * <p>Registration steps:
     * <ol>
     *   <li>Check that the requested username is not already in use.</li>
     *   <li>BCrypt-encode the plain-text password before persisting.</li>
     *   <li>Save the new {@link User} to the database.</li>
     *   <li>Generate a JWT embedding the username and the newly assigned user
     *       ID.</li>
     *   <li>Return an {@link AuthResponse} containing the token and
     *       username.</li>
     * </ol>
     *
     * @param request the registration payload containing the desired username
     *                and plain-text password; must not be {@code null}.
     * @return an {@link AuthResponse} with a valid JWT and the registered
     *         username.
     * @throws IllegalArgumentException if the requested username is already
     *                                  taken.
     */
    public AuthResponse register(AuthRequest request) {
        logger.info("Registering user: {}", request.getUsername());

        // Reject duplicate usernames early to keep the error message clear
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Hash the password with BCrypt before storing; never persist plain text
        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        // Issue a token immediately so the client can proceed without a separate login
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user.getUsername());
    }

    /**
     * Authenticates an existing user and returns a fresh JWT on success.
     *
     * <p>Login steps:
     * <ol>
     *   <li>Look up the user by username.  A missing record is treated the
     *       same as a wrong password (no username enumeration).</li>
     *   <li>Verify the supplied plain-text password against the stored BCrypt
     *       hash using {@link PasswordEncoder#matches}.</li>
     *   <li>Generate and return a new JWT if credentials are valid.</li>
     * </ol>
     *
     * @param request the login payload containing the username and plain-text
     *                password; must not be {@code null}.
     * @return an {@link AuthResponse} with a valid JWT and the authenticated
     *         username.
     * @throws IllegalArgumentException if the username does not exist or the
     *                                  password is incorrect.
     */
    public AuthResponse login(AuthRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());

        // Use a generic error message to avoid revealing whether the username exists
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // BCrypt comparison: the encoder re-hashes the raw password and compares
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Generate a JWT that carries the user's identity for subsequent requests
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user.getUsername());
    }
}
