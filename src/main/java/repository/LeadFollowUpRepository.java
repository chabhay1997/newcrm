package repository;

import model.LeadFollowUp;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LeadFollowUpRepository extends JpaRepository<LeadFollowUp, Long> {

    @Query(value = "SELECT f.id AS id, f.reason AS reason, f.followup_type AS followupType, "
            + "f.next_followup_date AS nextFollowupDate, f.next_followup_time AS nextFollowupTime, "
            + "f.created_at AS createdAt, u.name AS userName "
            + "FROM follow_up_leads f LEFT JOIN users u ON u.id = f.created_by "
            + "WHERE f.lead_fk_id = :leadId AND f.reason IS NOT NULL AND TRIM(f.reason) <> '' "
            + "ORDER BY f.created_at DESC, f.id DESC", nativeQuery = true)
    List<FollowUpSummary> findByLeadId(@Param("leadId") Long leadId);

        @Query(value = "SELECT f.lead_fk_id AS leadId, f.reason AS reason FROM follow_up_leads f "
            + "WHERE f.lead_fk_id IN (:leadIds) AND f.reason IS NOT NULL AND TRIM(f.reason) <> '' "
            + "AND NOT EXISTS (SELECT 1 FROM follow_up_leads newer WHERE newer.lead_fk_id = f.lead_fk_id "
            + "AND newer.reason IS NOT NULL AND TRIM(newer.reason) <> '' "
            + "AND ((newer.created_at IS NOT NULL AND (f.created_at IS NULL OR newer.created_at > f.created_at)) "
            + "OR ((newer.created_at = f.created_at OR (newer.created_at IS NULL AND f.created_at IS NULL)) "
            + "AND newer.id > f.id)))", nativeQuery = true)
        List<LatestFollowUpSummary> findLatestCommentsByLeadIds(@Param("leadIds") List<Long> leadIds);

        @Query(value = "SELECT DISTINCT f.lead_fk_id FROM follow_up_leads f "
            + "WHERE f.reason IS NOT NULL AND TRIM(f.reason) <> ''", nativeQuery = true)
        List<Long> findLeadIdsWithComments();

        interface LatestFollowUpSummary {
            Long getLeadId();

            String getReason();
        }

    interface FollowUpSummary {
        Long getId();

        String getReason();

        String getFollowupType();

        String getNextFollowupDate();

        String getNextFollowupTime();

        LocalDateTime getCreatedAt();

        String getUserName();
    }
}