package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.Tour;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Tour} entity.
 *
 * <p>By extending {@link JpaRepository} we inherit the full CRUD surface
 * ({@code findAll}, {@code findById}, {@code save}, {@code deleteById}, …) for
 * free — Spring generates the implementation at startup, so no boilerplate is
 * required. The three additional methods below follow the "derived query"
 * naming convention: Spring parses the method name and generates the matching
 * JPQL automatically.
 *
 * <p>All three custom methods include {@code userId} in the query because the
 * application is multi-tenant: every service call must scope data to the
 * authenticated user so two users can never read or modify each other's tours.
 * This is the single most important invariant of the data layer.
 *
 * <p>Implements: <b>Repository pattern</b> (one of the required design patterns).
 */
@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    /**
     * Returns every tour owned by the given user.
     *
     * @param userId the owning user's primary key
     * @return an unsorted list, possibly empty, never {@code null}
     */
    List<Tour> findByUserId(Long userId);

    /**
     * Returns the tour with the given ID only if it belongs to the given user.
     * This is the workhorse query used by the service layer to enforce
     * per-user ownership on every read / update / delete.
     *
     * @param id     the tour primary key
     * @param userId the expected owner
     * @return {@code Optional.of(tour)} if found and owned; otherwise empty
     */
    Optional<Tour> findByIdAndUserId(Long id, Long userId);

    /**
     * Lightweight existence check — avoids pulling the full entity when the
     * caller only needs to answer "does this user own this tour?".
     *
     * @param id     the tour primary key
     * @param userId the expected owner
     * @return {@code true} iff a row matches both conditions
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}
