package repository;

import model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query("SELECT l FROM Lead l WHERE (:status IS NULL OR l.status = :status) AND (:sourceId IS NULL OR l.sourceId = :sourceId)")
    List<Lead> findByFilters(@Param("status") String status, @Param("sourceId") Long sourceId);

    @Query("SELECT l.status AS statusKey, COUNT(l) AS cnt FROM Lead l GROUP BY l.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT l.sourceId AS sourceKey, COUNT(l) AS cnt FROM Lead l GROUP BY l.sourceId")
    List<Object[]> countGroupedBySource();
}