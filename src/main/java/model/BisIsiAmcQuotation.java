package model;

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
@Table(name = "bis_isi_amc_quotations")
public class BisIsiAmcQuotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "operation_id", nullable = false, unique = true) private Long operationId;
    @Column(name = "reference_number", unique = true) private String referenceNumber;
    @Column(name = "proposal_date", nullable = false) private LocalDate proposalDate;
    @Column(name = "kind_attention") private String kindAttention;
    @Column(name = "is_standard") private String isStandard;
    @Column(name = "product") private String product;
    @Column(name = "cml_number") private String cmlNumber;
    @Column(name = "licence_validity_date") private LocalDate licenceValidityDate;
    @Column(name = "actual_marking_fee") private String actualMarkingFee;
    @Column(name = "sample_testing_fee") private String sampleTestingFee;
    @Column(name = "engineer_visit_charge") private String engineerVisitCharge;
    @Column(name = "consultancy_service_fee") private String consultancyServiceFee;
    @Column(name = "consultancy_one_year") private String consultancyOneYear;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void beforeCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void beforeUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public LocalDate getProposalDate() { return proposalDate; }
    public void setProposalDate(LocalDate proposalDate) { this.proposalDate = proposalDate; }
    public String getKindAttention() { return kindAttention; }
    public void setKindAttention(String kindAttention) { this.kindAttention = kindAttention; }
    public String getIsStandard() { return isStandard; }
    public void setIsStandard(String isStandard) { this.isStandard = isStandard; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getCmlNumber() { return cmlNumber; }
    public void setCmlNumber(String cmlNumber) { this.cmlNumber = cmlNumber; }
    public LocalDate getLicenceValidityDate() { return licenceValidityDate; }
    public void setLicenceValidityDate(LocalDate licenceValidityDate) { this.licenceValidityDate = licenceValidityDate; }
    public String getActualMarkingFee() { return actualMarkingFee; }
    public void setActualMarkingFee(String actualMarkingFee) { this.actualMarkingFee = actualMarkingFee; }
    public String getSampleTestingFee() { return sampleTestingFee; }
    public void setSampleTestingFee(String sampleTestingFee) { this.sampleTestingFee = sampleTestingFee; }
    public String getEngineerVisitCharge() { return engineerVisitCharge; }
    public void setEngineerVisitCharge(String engineerVisitCharge) { this.engineerVisitCharge = engineerVisitCharge; }
    public String getConsultancyServiceFee() { return consultancyServiceFee; }
    public void setConsultancyServiceFee(String consultancyServiceFee) { this.consultancyServiceFee = consultancyServiceFee; }
    public String getConsultancyOneYear() { return consultancyOneYear; }
    public void setConsultancyOneYear(String consultancyOneYear) { this.consultancyOneYear = consultancyOneYear; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
