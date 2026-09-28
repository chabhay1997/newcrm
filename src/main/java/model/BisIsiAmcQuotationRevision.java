package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "bis_isi_amc_quotation_revisions", uniqueConstraints =
        @jakarta.persistence.UniqueConstraint(name = "uk_amc_quote_revision", columnNames = {"operation_id", "revision_number"}))
public class BisIsiAmcQuotationRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "operation_id", nullable = false) private Long operationId;
    @Column(name = "revision_number", nullable = false) private int revisionNumber;
    @Column(name = "reference_number", nullable = false) private String referenceNumber;
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

    public Long getOperationId() { return operationId; }
    public int getRevisionNumber() { return revisionNumber; }
    public String getReferenceNumber() { return referenceNumber; }
    public LocalDate getProposalDate() { return proposalDate; }
    public String getKindAttention() { return kindAttention; }
    public String getIsStandard() { return isStandard; }
    public String getProduct() { return product; }
    public String getCmlNumber() { return cmlNumber; }
    public LocalDate getLicenceValidityDate() { return licenceValidityDate; }
    public String getActualMarkingFee() { return actualMarkingFee; }
    public String getSampleTestingFee() { return sampleTestingFee; }
    public String getEngineerVisitCharge() { return engineerVisitCharge; }
    public String getConsultancyServiceFee() { return consultancyServiceFee; }
    public String getConsultancyOneYear() { return consultancyOneYear; }

    public void copyFrom(BisIsiAmcQuotation quote, int revision) {
        operationId = quote.getOperationId();
        revisionNumber = revision;
        referenceNumber = quote.getReferenceNumber();
        proposalDate = quote.getProposalDate();
        kindAttention = quote.getKindAttention();
        isStandard = quote.getIsStandard();
        product = quote.getProduct();
        cmlNumber = quote.getCmlNumber();
        licenceValidityDate = quote.getLicenceValidityDate();
        actualMarkingFee = quote.getActualMarkingFee();
        sampleTestingFee = quote.getSampleTestingFee();
        engineerVisitCharge = quote.getEngineerVisitCharge();
        consultancyServiceFee = quote.getConsultancyServiceFee();
        consultancyOneYear = quote.getConsultancyOneYear();
    }
}
