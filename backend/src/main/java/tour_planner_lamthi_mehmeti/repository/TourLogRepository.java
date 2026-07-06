package tour_planner_lamthi_mehmeti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tour_planner_lamthi_mehmeti.model.TourLog;

import java.util.Collection;
import java.util.List;

/**
 * JPA repository for TourLog. Ownership checks happen in TourLogService.
 */
@Repository
public interface TourLogRepository extends JpaRepository<TourLog, Long> {

    List<TourLog> findByTourId(Long tourId);

    /** Bulk fetch for search — one query instead of N+1. */
    List<TourLog> findByTourIdIn(Collection<Long> tourIds);

    /** Called before deleting a tour to avoid FK violations. */
    void deleteByTourId(Long tourId);
}
