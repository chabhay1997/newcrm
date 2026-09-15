package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Business fields (shown in form/table) ----
    @Column(name = "is_name")
    private String isName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_mobile")
    private String companyMobile;

    @Column(name = "official_mail_id")
    private String officialMailId;

    private String email;
    private String phone;

    @Column(name = "source_id")
    private Long sourceId;

    private String status; // numeric-as-string, e.g. "1".."8"

    @Column(name = "lead_category")
    private String leadCategory;

    @Column(name = "multiselectcategory")
    private String multiselectCategory;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "mcat_name")
    private String mcatName;

    private String address;
    private String requirements;
    private String remarks;
    private String reminder;
    private String message;
    private String pincode;

    @Column(name = "country_id")
    private String countryId;

    @Column(name = "state_id")
    private String stateId;

    @Column(name = "city_id")
    private String cityId;

    private Integer amount;

    @Column(name = "amount_usd")
    private String amountUsd;

    @Column(name = "lead_date")
    private LocalDate leadDate;

    @Column(name = "converted_date")
    private LocalDate convertedDate;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "scheduled_call_time")
    private LocalDateTime scheduledCallTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Internal / system fields (kept for DB integrity, NOT shown in UI) ----
    @Column(name = "job_id_fk")
    private Long jobIdFk;

    @Column(name = "unique_query_id")
    private String uniqueQueryId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_by")
    private Integer deletedBy;

    @Column(name = "assign_to")
    private Long assignTo;

    @Column(name = "assign_by")
    private Integer assignBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_color")
    private Boolean isColor;

    @Column(name = "is_followup_color")
    private String isFollowupColor;

    @Column(name = "vendor_id")
    private Integer vendorId;

    @Column(name = "services_id")
    private String servicesId;

    @Column(name = "certificateType_id")
    private Long certificateTypeId;

    @Column(name = "scheduled_call_reminder_count")
    private Integer scheduledCallReminderCount;

    @Column(name = "scheduled_call_last_reminded_at")
    private LocalDateTime scheduledCallLastRemindedAt;

    @Column(name = "converted_quo_up")
    private String convertedQuoUp;

    @Column(name = "quotation_type")
    private String quotationType;

    @Column(name = "is_number")
    private String isNumber;

    // ---- Getters & setters (business fields only, shown here) ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getIsName() { return isName; }
    public void setIsName(String isName) { this.isName = isName; }

    public String getCompanyMobile() { return companyMobile; }
    public void setCompanyMobile(String companyMobile) { this.companyMobile = companyMobile; }

    public String getOfficialMailId() { return officialMailId; }
    public void setOfficialMailId(String officialMailId) { this.officialMailId = officialMailId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLeadCategory() { return leadCategory; }
    public void setLeadCategory(String leadCategory) { this.leadCategory = leadCategory; }

    public String getMultiselectCategory() { return multiselectCategory; }
    public void setMultiselectCategory(String multiselectCategory) { this.multiselectCategory = multiselectCategory; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getMcatName() { return mcatName; }
    public void setMcatName(String mcatName) { this.mcatName = mcatName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getCountryId() { return countryId; }
    public void setCountryId(String countryId) { this.countryId = countryId; }

    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }

    public String getCityId() { return cityId; }
    public void setCityId(String cityId) { this.cityId = cityId; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getAmountUsd() { return amountUsd; }
    public void setAmountUsd(String amountUsd) { this.amountUsd = amountUsd; }

    public LocalDate getLeadDate() { return leadDate; }
    public void setLeadDate(LocalDate leadDate) { this.leadDate = leadDate; }

    public LocalDate getConvertedDate() { return convertedDate; }
    public void setConvertedDate(LocalDate convertedDate) { this.convertedDate = convertedDate; }

    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }

    public LocalDateTime getScheduledCallTime() { return scheduledCallTime; }
    public void setScheduledCallTime(LocalDateTime scheduledCallTime) { this.scheduledCallTime = scheduledCallTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getQuotationType() { return quotationType; }
    public void setQuotationType(String quotationType) { this.quotationType = quotationType; }
}