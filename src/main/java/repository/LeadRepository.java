package repository;

import model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByUniqueQueryId(String uniqueQueryId);

    @Query("select l from Lead l where lower(l.status)='converted' and (l.isDeleted is null or l.isDeleted = false)")
    List<Lead> findConvertedActive();

    @Query("SELECT l FROM Lead l WHERE (l.isDeleted IS NULL OR l.isDeleted = false) AND (:status IS NULL OR l.status = :status) AND (:sourceId IS NULL OR l.sourceId = :sourceId) AND (:quotationTypes IS NULL OR (l.certificateTypeId IS NULL AND l.quotationType IN :quotationTypes) OR l.certificateTypeId IN :certificateTypeIds) AND (:userId IS NULL OR (l.assignTo IS NOT NULL AND l.assignTo = :userId) OR (l.assignTo IS NULL AND l.createdBy = :userId)) AND (:startDate IS NULL OR l.createdAt >= :startDate) AND (:endDate IS NULL OR l.createdAt <= :endDate) AND (:countryId IS NULL OR l.countryId = :countryId) AND (:stateId IS NULL OR l.stateId = :stateId) AND (:cityId IS NULL OR l.cityId = :cityId) ORDER BY l.createdAt DESC, l.id DESC")
    List<Lead> findByFilters(@Param("status") String status, @Param("sourceId") Long sourceId, @Param("quotationTypes") List<String> quotationTypes, @Param("certificateTypeIds") List<Long> certificateTypeIds, @Param("userId") Long userId, @Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate, @Param("countryId") String countryId, @Param("stateId") String stateId, @Param("cityId") String cityId);

    @Query("SELECT l.status AS statusKey, COUNT(l) AS cnt FROM Lead l GROUP BY l.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT l.sourceId AS sourceKey, COUNT(l) AS cnt FROM Lead l GROUP BY l.sourceId")
    List<Object[]> countGroupedBySource();
}
