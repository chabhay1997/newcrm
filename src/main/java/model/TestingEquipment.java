package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "testing_equipments")
public class TestingEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "invoice_no")
    private String invoiceNo;

    private LocalDate date;
    private String attention;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "is_code")
    private String isCode;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "equipment_name")
    private String equipmentName;

    @Column(name = "actual_price")
    private BigDecimal actualPrice;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "des_qty_price", columnDefinition = "LONGTEXT")
    private String desQtyPrice;

    public Integer getId() { return id; }
    public Long getCreatedBy() { return createdBy; }
    public String getInvoiceNo() { return invoiceNo; }
    public LocalDate getDate() { return date; }
    public String getAttention() { return attention; }
    public String getClientName() { return clientName; }
    public String getIsCode() { return isCode; }
    public String getCompanyName() { return companyName; }
    public String getHsnCode() { return hsnCode; }
    public String getEquipmentName() { return equipmentName; }
    public BigDecimal getActualPrice() { return actualPrice; }
    public String getDescription() { return description; }
    public String getDesQtyPrice() { return desQtyPrice; }

    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setAttention(String attention) { this.attention = attention; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setIsCode(String isCode) { this.isCode = isCode; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    public void setDescription(String description) { this.description = description; }
    public void setDesQtyPrice(String desQtyPrice) { this.desQtyPrice = desQtyPrice; }
}
