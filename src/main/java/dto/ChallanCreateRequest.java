package dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ChallanCreateRequest {
    private String challanNo;
    private String clientName;
    private String clientNumber;
    private String itemName;
    private String brandName;
    private Long stateId;
    private String qty;
    private String amount;
    private String gst;
    private String totalAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate date;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate sampleReturnDate;
    private String address;
    private String pincode;
    private String remark;

    public String getChallanNo() { return challanNo; }
    public String getClientName() { return clientName; }
    public String getClientNumber() { return clientNumber; }
    public String getItemName() { return itemName; }
    public String getBrandName() { return brandName; }
    public Long getStateId() { return stateId; }
    public String getQty() { return qty; }
    public String getAmount() { return amount; }
    public String getGst() { return gst; }
    public String getTotalAmount() { return totalAmount; }
    public LocalDate getDate() { return date; }
    public LocalDate getSampleReturnDate() { return sampleReturnDate; }
    public String getAddress() { return address; }
    public String getPincode() { return pincode; }
    public String getRemark() { return remark; }

    public void setChallanNo(String challanNo) { this.challanNo = challanNo; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setClientNumber(String clientNumber) { this.clientNumber = clientNumber; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
    public void setQty(String qty) { this.qty = qty; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setGst(String gst) { this.gst = gst; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setSampleReturnDate(LocalDate sampleReturnDate) { this.sampleReturnDate = sampleReturnDate; }
    public void setAddress(String address) { this.address = address; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setRemark(String remark) { this.remark = remark; }
}
