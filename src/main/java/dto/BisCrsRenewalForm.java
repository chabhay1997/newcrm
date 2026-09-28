package dto;

import java.time.LocalDate;

public class BisCrsRenewalForm {
    private String licenceNumber;
    private String manufacturerName;
    private String productName;
    private String isStandard;
    private String airName;
    private String brandName;
    private LocalDate licenceDate;
    private LocalDate expiryDate;
    private LocalDate notifyDate;
    private String airAddress;
    private String airEmailId;
    private String airContactNumber;
    private String currentStatus;

    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }
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
}
