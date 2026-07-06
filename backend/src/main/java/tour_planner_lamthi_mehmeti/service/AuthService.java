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
 * Register and login. Passwords are BCrypt-hashed; login errors stay vague on purpose.
 */
@Service
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(AuthRequest request) {
        logger.info("Registering user: {}", request.getUsername());

        // Reject duplicate usernames early to keep the error message clear
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already taken");
        }

        // Hash the password with BCrypt before storing; never persist plain text
        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        // Issue a token immediately so the client can proceed without a separate login
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user.getUsername());
    }

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
