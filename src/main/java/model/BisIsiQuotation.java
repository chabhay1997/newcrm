package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "quotations")
public class BisIsiQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lead_fk_id", nullable = false)
    private Long leadId;
    @Column(name = "reference_no")
    private String referenceNo;
    @Column(name = "quotation_date")
    private LocalDate quotationDate;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "certificate_name")
    private String certificateName;
    @Column(name = "client_name")
    private String clientName;
    @Column(name = "commercial_alias_1")
    private String commercialAlias1;
    @Column(name = "commercial_value_1")
    private String commercialValue1;
    @Column(name = "commercial_alias_2")
    private String commercialAlias2;
    @Column(name = "commercial_value_2")
    private String commercialValue2;
    @Column(name = "commercial_alias_3")
    private String commercialAlias3;
    @Column(name = "commercial_value_3")
    private String commercialValue3;
    @Column(name = "commercial_alias_4")
    private String commercialAlias4;
    @Column(name = "commercial_value_4")
    private String commercialValue4;
    @Column(name = "commercial_alias_5")
    private String commercialAlias5;
    @Column(name = "commercial_value_5")
    private String commercialValue5;
    @Column(name = "consultancy_charge")
    private String consultancyCharge;
    @Column(name = "consultancy_alias_1")
    private String consultancyAlias1;
    @Column(name = "consultancy_value_1")
    private String consultancyValue1;
    @Column(name = "calibration_fees")
    private String consultancyFees;
    @Column(name = "consultancy_alias_2")
    private String consultancyAlias2;
    @Column(name = "consultancy_value_2")
    private String consultancyValue2;
    @Column(name = "other_expense")
    private String otherExpense;
    @Column(name = "consultancy_alias_3")
    private String consultancyAlias3;
    @Column(name = "consultancy_value_3")
    private String consultancyValue3;
    @Column(name = "lab_setup_cost")
    private String labSetupCost;
    @Column(name = "lab_time")
    private String leadTime;
    @Column(name = "as_per_msme")
    private Boolean asPerMsme;
    @Column(name = "advance_fee")
    private String advanceFee;
    @Column(name = "registration_application_fee")
    private String registrationFee;
    @Column(name = "inspection_fee")
    private String inspectionFee;
    @Column(name = "granting_fee")
    private String grantingFee;
    @Column(name = "notes")
    private String notes;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public LocalDate getQuotationDate() { return quotationDate; }
    public void setQuotationDate(LocalDate quotationDate) { this.quotationDate = quotationDate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCertificateName() { return certificateName; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getCommercialAlias1() { return commercialAlias1; }
    public void setCommercialAlias1(String commercialAlias1) { this.commercialAlias1 = commercialAlias1; }
    public String getCommercialValue1() { return commercialValue1; }
    public void setCommercialValue1(String commercialValue1) { this.commercialValue1 = commercialValue1; }
    public String getCommercialAlias2() { return commercialAlias2; }
    public void setCommercialAlias2(String commercialAlias2) { this.commercialAlias2 = commercialAlias2; }
    public String getCommercialValue2() { return commercialValue2; }
    public void setCommercialValue2(String commercialValue2) { this.commercialValue2 = commercialValue2; }
    public String getCommercialAlias3() { return commercialAlias3; }
    public void setCommercialAlias3(String commercialAlias3) { this.commercialAlias3 = commercialAlias3; }
    public String getCommercialValue3() { return commercialValue3; }
    public void setCommercialValue3(String commercialValue3) { this.commercialValue3 = commercialValue3; }
    public String getCommercialAlias4() { return commercialAlias4; }
    public void setCommercialAlias4(String commercialAlias4) { this.commercialAlias4 = commercialAlias4; }
    public String getCommercialValue4() { return commercialValue4; }
    public void setCommercialValue4(String commercialValue4) { this.commercialValue4 = commercialValue4; }
    public String getCommercialAlias5() { return commercialAlias5; }
    public void setCommercialAlias5(String commercialAlias5) { this.commercialAlias5 = commercialAlias5; }
    public String getCommercialValue5() { return commercialValue5; }
    public void setCommercialValue5(String commercialValue5) { this.commercialValue5 = commercialValue5; }
    public String getConsultancyCharge() { return consultancyCharge; }
    public void setConsultancyCharge(String consultancyCharge) { this.consultancyCharge = consultancyCharge; }
    public String getConsultancyAlias1() { return consultancyAlias1; }
    public void setConsultancyAlias1(String consultancyAlias1) { this.consultancyAlias1 = consultancyAlias1; }
    public String getConsultancyValue1() { return consultancyValue1; }
    public void setConsultancyValue1(String consultancyValue1) { this.consultancyValue1 = consultancyValue1; }
    public String getConsultancyFees() { return consultancyFees; }
    public void setConsultancyFees(String consultancyFees) { this.consultancyFees = consultancyFees; }
    public String getConsultancyAlias2() { return consultancyAlias2; }
    public void setConsultancyAlias2(String consultancyAlias2) { this.consultancyAlias2 = consultancyAlias2; }
    public String getConsultancyValue2() { return consultancyValue2; }
    public void setConsultancyValue2(String consultancyValue2) { this.consultancyValue2 = consultancyValue2; }
    public String getOtherExpense() { return otherExpense; }
    public void setOtherExpense(String otherExpense) { this.otherExpense = otherExpense; }
    public String getConsultancyAlias3() { return consultancyAlias3; }
    public void setConsultancyAlias3(String consultancyAlias3) { this.consultancyAlias3 = consultancyAlias3; }
    public String getConsultancyValue3() { return consultancyValue3; }
    public void setConsultancyValue3(String consultancyValue3) { this.consultancyValue3 = consultancyValue3; }
    public String getLabSetupCost() { return labSetupCost; }
    public void setLabSetupCost(String labSetupCost) { this.labSetupCost = labSetupCost; }
    public String getLeadTime() { return leadTime; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; }
    public Boolean getAsPerMsme() { return asPerMsme; }
    public void setAsPerMsme(Boolean asPerMsme) { this.asPerMsme = asPerMsme; }
    public String getAdvanceFee() { return advanceFee; }
    public void setAdvanceFee(String advanceFee) { this.advanceFee = advanceFee; }
    public String getRegistrationFee() { return registrationFee; }
    public void setRegistrationFee(String registrationFee) { this.registrationFee = registrationFee; }
    public String getInspectionFee() { return inspectionFee; }
    public void setInspectionFee(String inspectionFee) { this.inspectionFee = inspectionFee; }
    public String getGrantingFee() { return grantingFee; }
    public void setGrantingFee(String grantingFee) { this.grantingFee = grantingFee; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
