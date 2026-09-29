package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cb_rdso_details")
public class CbRdsoDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // points to cb_rdso_quotations.id
    @Column(name = "cb_rdso_fk_id", nullable = false) public Long cbRdsoFkId;

    @Column(name = "s_no") public Integer sNo;
    @Column(name = "description", columnDefinition = "TEXT") public String description;
    @Column(name = "timeline", columnDefinition = "TEXT") public String timeline;
    @Column(name = "charges") public String charges;
    @Column(name = "extra_alias") public String extraAlias;
    @Column(name = "consultancy_extra_alias") public String consultancyExtraAlias;

    @Column(name = "cb_rdso_item_id") public String cbRdsoItemId;
    @Column(name = "cb_rdso_item_name", columnDefinition = "TEXT") public String cbRdsoItemName;
    @Column(name = "cb_rdso_registration_fee") public String cbRdsoRegistrationFee;
    @Column(name = "cb_rdso_officer_visit") public String cbRdsoOfficerVisit;
    @Column(name = "cb_rdso_sample_testing") public String cbRdsoSampleTesting;
    @Column(name = "cb_rdso_service_fee") public String cbRdsoServiceFee;
    @Column(name = "cb_rdso_other_expenses") public String cbRdsoOtherExpenses;
    @Column(name = "cb_rdso_engineer_visit") public String cbRdsoEngineerVisit;
    @Column(name = "cb_rdso_total_fee") public String cbRdsoTotalFee;

    @Column(name = "created_at", updatable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at") public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}