package tour_planner_lamthi_kiri_puka.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tour_planner_lamthi_kiri_puka.dto.AuthRequest;
import tour_planner_lamthi_kiri_puka.dto.AuthResponse;
import tour_planner_lamthi_kiri_puka.model.User;
import tour_planner_lamthi_kiri_puka.repository.UserRepository;
import tour_planner_lamthi_kiri_puka.security.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private UserRepository userRepo;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtil;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("TestSecretKeyForTourPlannerJwtAuth!!", 86400000L);
        service = new AuthService(userRepo, encoder, jwtUtil);
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        when(userRepo.existsByUsername("alice")).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse res = service.register(new AuthRequest("alice", "password1"));

        assertNotNull(res.getToken());
        assertEquals("alice", res.getUsername());
        verify(userRepo).save(any());
    }

    @Test
    void registerThrowsWhenUsernameTaken() {
        when(userRepo.existsByUsername("bob")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.register(new AuthRequest("bob", "pass")));
        verify(userRepo, never()).save(any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        String hash = encoder.encode("secret");
        User u = new User("carol", hash);
        u.setId(2L);
        when(userRepo.findByUsername("carol")).thenReturn(Optional.of(u));

        AuthResponse res = service.login(new AuthRequest("carol", "secret"));

        assertNotNull(res.getToken());
        assertEquals("carol", res.getUsername());
    }

    @Test
    void loginThrowsForUnknownUsername() {
        when(userRepo.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.login(new AuthRequest("nobody", "pass")));
    }

    @Test
    void loginThrowsForWrongPassword() {
        String hash = encoder.encode("correct");
        User u = new User("dave", hash);
        u.setId(3L);
        when(userRepo.findByUsername("dave")).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class,
                () -> service.login(new AuthRequest("dave", "wrong")));
    }
}
