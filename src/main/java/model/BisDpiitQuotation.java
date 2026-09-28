package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "dpiit_quotations")
public class BisDpiitQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String companyName;
    public String clientName;
    public String certificateName;
    public String productName;

    public String commercialExpense1;
    public String commercialAlias1;
    public String commercialValue1;
    public String commercialCurrency1;
    public String commercialExpense2;
    public String commercialAlias2;
    public String commercialValue2;
    public String commercialCurrency2;
    public String commercialExpense3;
    public String commercialAlias3;
    public String commercialValue3;
    public String commercialCurrency3;
    public String commercialExpense5;
    public String commercialAlias5;
    public String commercialValue5;
    public String commercialCurrency5;
    public String totalFees;
    public String totalFeesCurrency;

    // dynamic add-on commercial rows: [{"particular":"...","amount":"...","currency":"INR"}]
    @Column(name = "extra_rows_json", columnDefinition = "TEXT")
    public String extraRowsJson;

    // dynamic consultancy rows: [{"expense":"...","amount":"..."}]
    @Column(name = "consultancy_items_json", columnDefinition = "TEXT")
    public String consultancyItemsJson;
    public String totalConsultancyFees;

    public String labSetupCost;
    public String leadTime;
}