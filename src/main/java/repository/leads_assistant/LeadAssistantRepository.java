package repository.leads_assistant;

import model.LeadAssistant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeadAssistantRepository extends JpaRepository<LeadAssistant, Long> {
    List<LeadAssistant> findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(String sessionId);

    @Modifying
    @Transactional
    @Query("update LeadAssistant l set l.deletedAt = :deletedAt where l.sessionId = :sessionId and l.deletedAt is null")
    void softDeleteBySessionId(@Param("sessionId") String sessionId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying
    @Transactional
    void deleteByDeletedAtBefore(LocalDateTime cutoff);
}