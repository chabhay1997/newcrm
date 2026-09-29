package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bee_quotations")
public class BeeQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id")
    public Long leadId;

    public String referenceNo;

    @Column(nullable = false)
    public Integer certificateType = 9;

    public String companyName;
    public String clientName;
    public String certificateName;
    public LocalDate quotationDate;
    public String productName;
    public String productCategoryClass;
    public String manufacturingLocation;
    public String productModels;
    public String summary;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}