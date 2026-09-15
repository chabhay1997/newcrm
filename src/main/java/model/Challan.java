package model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "challans")
public class Challan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challan_no")
    private String challanNo;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "brand_name")
    private String brandName;

    private String qty;

    @Column(name = "amt")
    private String amount;

    private LocalDate date;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "state_id")
    private Long stateId;

    private String gst;

    @Column(name = "totalAmt")
    private String totalAmount;

    @Column(name = "sample_return_date")
    private LocalDate sampleReturnDate;

    private String upload;

    @Column(columnDefinition = "LONGTEXT")
    private String address;

    private String pincode;

    @Column(name = "client_number")
    private String clientNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getChallanNo() { return challanNo; }
    public String getClientName() { return clientName; }
    public String getItemName() { return itemName; }
    public String getBrandName() { return brandName; }
    public String getQty() { return qty; }
    public String getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public Long getCreatedBy() { return createdBy; }
    public Long getStateId() { return stateId; }
    public String getGst() { return gst; }
    public String getTotalAmount() { return totalAmount; }
    public LocalDate getSampleReturnDate() { return sampleReturnDate; }
    public String getUpload() { return upload; }
    public String getAddress() { return address; }
    public String getPincode() { return pincode; }
    public String getClientNumber() { return clientNumber; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setChallanNo(String challanNo) { this.challanNo = challanNo; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public void setQty(String qty) { this.qty = qty; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
    public void setGst(String gst) { this.gst = gst; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public void setSampleReturnDate(LocalDate sampleReturnDate) { this.sampleReturnDate = sampleReturnDate; }
    public void setUpload(String upload) { this.upload = upload; }
    public void setAddress(String address) { this.address = address; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setClientNumber(String clientNumber) { this.clientNumber = clientNumber; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
