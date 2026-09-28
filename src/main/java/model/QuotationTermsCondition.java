package model;

import jakarta.persistence.*;

@Entity
@Table(name = "terms_conditions")
public class QuotationTermsCondition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "unit_price") private String unitPrice;
    private String delivery; private String payment; private String freight; private String warranty; private String inspection;
    @Column(name = "ins_comm") private String installationCommissioning;
    private String training; private String note;
    public String getUnitPrice() { return unitPrice; } public String getDelivery() { return delivery; } public String getPayment() { return payment; } public String getFreight() { return freight; } public String getWarranty() { return warranty; } public String getInspection() { return inspection; } public String getInstallationCommissioning() { return installationCommissioning; } public String getTraining() { return training; } public String getNote() { return note; }
}
