package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "quotation_testing_equipments")
public class QuotationTestingEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "testing_equipment_id", nullable = false)
    private Integer testingEquipmentId;

    @Column(name = "invoice_no")
    private String invoiceNo;

    private LocalDate date;
    private String attention;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "is_code")
    private String isCode;

    @Column(name = "des_qty_price", columnDefinition = "LONGTEXT")
    private String desQtyPrice;

    public Integer getId() { return id; }
    public void setTestingEquipmentId(Integer testingEquipmentId) { this.testingEquipmentId = testingEquipmentId; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setAttention(String attention) { this.attention = attention; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setIsCode(String isCode) { this.isCode = isCode; }
    public void setDesQtyPrice(String desQtyPrice) { this.desQtyPrice = desQtyPrice; }
}
