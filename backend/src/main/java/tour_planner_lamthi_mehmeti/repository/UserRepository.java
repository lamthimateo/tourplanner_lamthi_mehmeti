package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.User;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities (authentication only —
 * the domain data is stored in {@code Tour} / {@code TourLog}).
 *
 * <p>Two derived query methods cover both flows that {@code AuthService}
 * needs:
 * <ul>
 *   <li>{@link #findByUsername(String)} — login path, returns the full user
 *       (including BCrypt hash) so the password can be checked.</li>
 *   <li>{@link #existsByUsername(String)} — registration path, cheaply
 *       prevents creating two accounts with the same username.</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user by username (case-sensitive, as stored).
     * @return the user wrapped in {@code Optional}, or {@code Optional.empty()} if none
     */
    Optional<User> findByUsername(String username);

    /** @return {@code true} iff a row with that username already exists */
    boolean existsByUsername(String username);
}
