package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.User;

import java.util.Optional;

/**
 * JPA repository for User (login/register only).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
