package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_leads")
public class LeadFollowUp {

    @Id
    private Long id;

    @Column(name = "lead_fk_id")
    private Long leadId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "next_followup_date")
    private String nextFollowupDate;

    @Column(name = "next_followup_time")
    private String nextFollowupTime;

    @Column(name = "followup_type")
    private String followupType;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LeadFollowUp() {
    }
}