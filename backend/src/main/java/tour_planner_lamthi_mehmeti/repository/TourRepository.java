package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.Tour;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Tour. Custom methods always filter by userId for multi-user isolation.
 */
@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByUserId(Long userId);

    Optional<Tour> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
