package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "r_d_s_o_s")
public class RdsoQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;

    @Column(name = "reference_no")
    public String referenceNo;

    @Column(name = "date")
    public LocalDate quotationDate;

    @Column(name = "certificate_type")
    public Integer certificateType;

    @Column(name = "client_name")
    public String clientName;

    @Column(name = "product_name")
    public String productName;

    @Column(name = "gst_fee")
    public String gstFee;

    @Column(name = "payment_1")
    public String payment1;

    @Column(name = "payment_2")
    public String payment2;

    @Column(name = "payment_3")
    public String payment3;

    @Column(name = "created_by")
    public Long createdBy;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

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