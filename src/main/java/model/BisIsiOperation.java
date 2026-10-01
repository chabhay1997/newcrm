package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="operations")
public class BisIsiOperation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="date") private LocalDate operationDate;
    @Column(name="company_name",nullable=false) private String companyName;
    @Column(name="client_name",nullable=false) private String clientName;
    @Column(name="indian_standard",nullable=false,length=80) private String indianStandard;
    @Column(name="contact_number",nullable=false,length=30) private String contactNumber;
    @Column(name="alternate_contact_number",length=30) private String alternateContactNumber;
    @Column(name="client_mail_id") private String email;
    @Column(columnDefinition="TEXT") private String address;
    @Column(name="state_id",nullable=false) private Long stateId;
    @Column(name="user_id") private String portalUsername;
    @Column(name="passcode") private String portalPassword;
    @Column(name="mail_id") private String evtlEmail;
    @Column(name="extra_MP") private String evtlEmailPasscode;
    @Column(name="project_status") private String projectStatus;
    @Column(name="project_fk_id",insertable=false,updatable=false) private String legacyProjectId;
    @Convert(converter=ProcedureConverter.class) @Column(name="`procedure`") private String procedure;
    @Column(name="payment_status") private String paymentStatus;
    @Column(name="payment_remark") private String paymentRemark;
    @Column(name="payment_data",columnDefinition="LONGTEXT") private String paymentData;
    @Column(name="advance_payment_status") private String advancePaymentStatus;
    @Transient private Integer paymentInstallments;
    @Transient private BigDecimal totalAmount;
    @Column(name="testingDeadlineDate") private LocalDate testingDeadline;
    @Convert(converter=TestingStatusConverter.class) @Column(name="testing_status") private String testingStatus;
    @Column(name="testing_incharge") private String testingPerson;
    @Column(name="operation_incharge") private String operatingPerson;
    @Column(name="appDeadlineDate") private LocalDate applicationDeadline;
    @Column(name="licDeadlineDate") private LocalDate licenceDeadline;
    @Column(name="targetDate") private LocalDate targetDate;
    @Column(name="targetDate",insertable=false,updatable=false) private LocalDate targetFinalExceptionDate;
    @Transient private LocalDate finalDate;
    @Column(name="cmlNo") private String cmlNumber;
    @Transient private String licenceNumber;
    @Convert(converter=LegacyDateConverter.class) @Column(name="licGrantDate") private LocalDate licenceDate;
    @Column(name="created_by") private Long createdBy;
    @Column(name="assign_by") private Long assignedEngineerId;
    @Transient private String assignedEngineerName;
    @Column(columnDefinition="TEXT") private String remarks;
    @Transient private boolean inclusionEnabled;
    @Transient private boolean renewalEnabled;
    @Transient private boolean brandInclusionEnabled;
    @Transient private boolean thirdPartyVisitEnabled;
    @Transient private boolean sitEnabled;
    @Transient private String sitStatus;
    @Transient private String sitQuarter;
    @Transient private String extraServiceNotes;
    @Column(name="extra_service_id") private String extraService;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){ createdAt=updatedAt=LocalDateTime.now(); }
    @PreUpdate void update(){ updatedAt=LocalDateTime.now(); }
    public String getLegacyProjectId(){return legacyProjectId;}
    @Converter public static class ProcedureConverter implements AttributeConverter<String,Integer> {
        public Integer convertToDatabaseColumn(String value){if(value==null||value.isBlank())return null;return "Simplified".equalsIgnoreCase(value)?1:2;}
        public String convertToEntityAttribute(Integer value){return value==null?null:(value==1?"Simplified":"Normal");}
    }
    @Converter public static class TestingStatusConverter implements AttributeConverter<String,Integer> {
        public Integer convertToDatabaseColumn(String value){if(value==null||value.isBlank())return null;return "Paid".equalsIgnoreCase(value)?1:0;}
        public String convertToEntityAttribute(Integer value){return value==null?null:(value==1?"Paid":"Pending");}
    }
    public String getPaymentRemark(){return paymentRemark;}
    public void setPaymentRemark(String value){paymentRemark=value;}
    public String getPaymentData(){return paymentData;}
    public void setPaymentData(String value){paymentData=value;}
    @Converter public static class LegacyDateConverter implements AttributeConverter<LocalDate,String> {
        public String convertToDatabaseColumn(LocalDate value){return value==null?null:value.toString();}
        public LocalDate convertToEntityAttribute(String value){if(value==null||value.isBlank())return null;try{return LocalDate.parse(value);}catch(Exception ignored){}try{return LocalDate.parse(value,java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));}catch(Exception ignored){return null;}}
    }
    public Long getId(){return id;} public LocalDate getOperationDate(){return operationDate;} public void setOperationDate(LocalDate v){operationDate=v;} public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;} public String getClientName(){return clientName;} public void setClientName(String v){clientName=v;} public String getIndianStandard(){return indianStandard;} public void setIndianStandard(String v){indianStandard=v;} public String getContactNumber(){return contactNumber;} public void setContactNumber(String v){contactNumber=v;} public String getAlternateContactNumber(){return alternateContactNumber;} public void setAlternateContactNumber(String v){alternateContactNumber=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public Long getStateId(){return stateId;} public void setStateId(Long v){stateId=v;} public String getPortalUsername(){return portalUsername;} public void setPortalUsername(String v){portalUsername=v;} public String getPortalPassword(){return portalPassword;} public void setPortalPassword(String v){portalPassword=v;} public String getEvtlEmail(){return evtlEmail;} public void setEvtlEmail(String v){evtlEmail=v;} public String getEvtlEmailPasscode(){return evtlEmailPasscode;} public void setEvtlEmailPasscode(String v){evtlEmailPasscode=v;} public String getProjectStatus(){return projectStatus;} public void setProjectStatus(String v){projectStatus=v;} public String getProcedure(){return procedure;} public void setProcedure(String v){procedure=v;} public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;} public String getAdvancePaymentStatus(){return advancePaymentStatus;} public void setAdvancePaymentStatus(String v){advancePaymentStatus=v;} public Integer getPaymentInstallments(){return paymentInstallments;} public void setPaymentInstallments(Integer v){paymentInstallments=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public LocalDate getTestingDeadline(){return testingDeadline;} public void setTestingDeadline(LocalDate v){testingDeadline=v;} public String getTestingStatus(){return testingStatus;} public void setTestingStatus(String v){testingStatus=v;} public String getTestingPerson(){return testingPerson;} public void setTestingPerson(String v){testingPerson=v;} public String getOperatingPerson(){return operatingPerson;} public void setOperatingPerson(String v){operatingPerson=v;} public LocalDate getApplicationDeadline(){return applicationDeadline;} public void setApplicationDeadline(LocalDate v){applicationDeadline=v;} public LocalDate getLicenceDeadline(){return licenceDeadline;} public void setLicenceDeadline(LocalDate v){licenceDeadline=v;} public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;} public LocalDate getTargetFinalExceptionDate(){return targetFinalExceptionDate;} public void setTargetFinalExceptionDate(LocalDate v){targetFinalExceptionDate=v;} public LocalDate getFinalDate(){return finalDate;} public void setFinalDate(LocalDate v){finalDate=v;} public String getCmlNumber(){return cmlNumber;} public void setCmlNumber(String v){cmlNumber=v;} public String getLicenceNumber(){return licenceNumber;} public void setLicenceNumber(String v){licenceNumber=v;} public LocalDate getLicenceDate(){return licenceDate;} public void setLicenceDate(LocalDate v){licenceDate=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public Long getAssignedEngineerId(){return assignedEngineerId;} public void setAssignedEngineerId(Long v){assignedEngineerId=v;} public String getAssignedEngineerName(){return assignedEngineerName;} public void setAssignedEngineerName(String v){assignedEngineerName=v;} public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;} public boolean isInclusionEnabled(){return inclusionEnabled;} public void setInclusionEnabled(boolean v){inclusionEnabled=v;} public boolean isRenewalEnabled(){return renewalEnabled;} public void setRenewalEnabled(boolean v){renewalEnabled=v;} public boolean isBrandInclusionEnabled(){return brandInclusionEnabled;} public void setBrandInclusionEnabled(boolean v){brandInclusionEnabled=v;} public boolean isThirdPartyVisitEnabled(){return thirdPartyVisitEnabled;} public void setThirdPartyVisitEnabled(boolean v){thirdPartyVisitEnabled=v;} public boolean isSitEnabled(){return sitEnabled;} public void setSitEnabled(boolean v){sitEnabled=v;} public String getSitStatus(){return sitStatus;} public void setSitStatus(String v){sitStatus=v;} public String getSitQuarter(){return sitQuarter;} public void setSitQuarter(String v){sitQuarter=v;} public String getExtraServiceNotes(){return extraServiceNotes;} public void setExtraServiceNotes(String v){extraServiceNotes=v;} public String getExtraService(){return extraService;} public void setExtraService(String v){extraService=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
