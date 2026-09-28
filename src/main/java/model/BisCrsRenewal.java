package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bis_crs_renewals")
public class BisCrsRenewal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by") private Long createdBy;
    @Column(name = "manufacturer_name") private String manufacturerName;
    @Column(name = "product_name") private String productName;
    @Column(name = "is_standard") private String isStandard;
    @Column(name = "air_name") private String airName;
    @Column(name = "brand_name") private String brandName;
    @Column(name = "lic_no") private String licenceNumber;
    @Column(name = "date_of_lic") private LocalDate licenceDate;
    @Column(name = "exp_date_of_lic") private LocalDate expiryDate;
    @Column(name = "notify_date") private LocalDate notifyDate;
    @Lob @Column(name = "air_address") private String airAddress;
    @Column(name = "air_email_ID") private String airEmailId;
    @Column(name = "air_cont_no") private String airContactNumber;
    @Column(name = "current_status") private String currentStatus;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getManufacturerName() { return manufacturerName; }
    public void setManufacturerName(String manufacturerName) { this.manufacturerName = manufacturerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getIsStandard() { return isStandard; }
    public void setIsStandard(String isStandard) { this.isStandard = isStandard; }
    public String getAirName() { return airName; }
    public void setAirName(String airName) { this.airName = airName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }
    public LocalDate getLicenceDate() { return licenceDate; }
    public void setLicenceDate(LocalDate licenceDate) { this.licenceDate = licenceDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public LocalDate getNotifyDate() { return notifyDate; }
    public void setNotifyDate(LocalDate notifyDate) { this.notifyDate = notifyDate; }
    public String getAirAddress() { return airAddress; }
    public void setAirAddress(String airAddress) { this.airAddress = airAddress; }
    public String getAirEmailId() { return airEmailId; }
    public void setAirEmailId(String airEmailId) { this.airEmailId = airEmailId; }
    public String getAirContactNumber() { return airContactNumber; }
    public void setAirContactNumber(String airContactNumber) { this.airContactNumber = airContactNumber; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
