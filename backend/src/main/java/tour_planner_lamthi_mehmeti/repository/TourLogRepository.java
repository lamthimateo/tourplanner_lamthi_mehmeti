package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.TourLog;

import java.util.List;

/**
 * Spring Data JPA repository for {@link TourLog} entities.
 *
 * <p>Inherits full CRUD from {@link JpaRepository}. The only extra method is a
 * derived query that fetches every log belonging to a given tour, which is
 * what the UI needs when the user opens a tour's "logs" panel.
 *
 * <p>Ownership checks are NOT performed here — the repository is deliberately
 * thin. Every caller in {@code TourLogService} first validates that the parent
 * tour belongs to the authenticated user, so any log accessed through this
 * method is already proven to be reachable by the current user.
 *
 * <p>Implements: <b>Repository pattern</b> (required design pattern).
 */
@Repository
public interface TourLogRepository extends JpaRepository<TourLog, Long> {

    /**
     * Returns every log recorded under the given parent tour, in no
     * particular order.
     *
     * @param tourId the parent tour's primary key
     * @return logs for that tour, possibly empty, never {@code null}
     */
    List<TourLog> findByTourId(Long tourId);
}
