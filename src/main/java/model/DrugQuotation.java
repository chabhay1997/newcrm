package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "drug_quotations")
public class DrugQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String companyName;
    public String clientName;
    public String productName;
    public String productCategoryClass;
    public String certificateName; // custom heading, optional
    public String manufacturingLocation;
    public String productModels;

    public String commercialAlias1;
    public String commercialValue1;
    public String commercialCurrency1;
    public String commercialAlias2;
    public String commercialValue2;
    public String commercialCurrency2;
    public String commercialAlias3; // total row label
    public String commercialValue3; // total row amount (incl. GST)

    // dynamic extra commercial rows: [{"particular":"...","amount":"..."}]
    @Column(name = "extra_rows_json", columnDefinition = "TEXT")
    public String extraRowsJson;

    // dynamic list of strings
    @Column(name = "timeline_items_json", columnDefinition = "TEXT")
    public String timelineItemsJson;

    // dynamic list of strings
    @Column(name = "required_documents_json", columnDefinition = "TEXT")
    public String requiredDocumentsJson;
}