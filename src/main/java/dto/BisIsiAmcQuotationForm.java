package dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class BisIsiAmcQuotationForm {
    private Integer revisionNumber;
    private boolean newRevision;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate proposalDate;
    private String kindAttention;
    private String isStandard;
    private String product;
    private String cmlNumber;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate licenceValidityDate;
    private String actualMarkingFee;
    private String sampleTestingFee;
    private String engineerVisitCharge;
    private String consultancyServiceFee;
    private String consultancyOneYear;

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer value) { revisionNumber = value; }
    public boolean isNewRevision() { return newRevision; }
    public void setNewRevision(boolean value) { newRevision = value; }

    public LocalDate getProposalDate() { return proposalDate; }
    public void setProposalDate(LocalDate value) { proposalDate = value; }
    public String getKindAttention() { return kindAttention; }
    public void setKindAttention(String value) { kindAttention = value; }
    public String getIsStandard() { return isStandard; }
    public void setIsStandard(String value) { isStandard = value; }
    public String getProduct() { return product; }
    public void setProduct(String value) { product = value; }
    public String getCmlNumber() { return cmlNumber; }
    public void setCmlNumber(String value) { cmlNumber = value; }
    public LocalDate getLicenceValidityDate() { return licenceValidityDate; }
    public void setLicenceValidityDate(LocalDate value) { licenceValidityDate = value; }
    public String getActualMarkingFee() { return actualMarkingFee; }
    public void setActualMarkingFee(String value) { actualMarkingFee = value; }
    public String getSampleTestingFee() { return sampleTestingFee; }
    public void setSampleTestingFee(String value) { sampleTestingFee = value; }
    public String getEngineerVisitCharge() { return engineerVisitCharge; }
    public void setEngineerVisitCharge(String value) { engineerVisitCharge = value; }
    public String getConsultancyServiceFee() { return consultancyServiceFee; }
    public void setConsultancyServiceFee(String value) { consultancyServiceFee = value; }
    public String getConsultancyOneYear() { return consultancyOneYear; }
    public void setConsultancyOneYear(String value) { consultancyOneYear = value; }
}
