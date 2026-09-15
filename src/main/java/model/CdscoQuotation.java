package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;

@Entity
@Table(name = "cdsco_leads")
public class CdscoQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String certificateType;
    public String clientName;
    public String companyName;
    public String productName;
    public String productCategoryClass;
    public String certificateName;
    public String manufacturingLocation;
    public String productModels;
    @Transient
    public String commercialSet;
    @Transient
    public String docsSet;
    @Column(name = "commercial_alias_1") public String commercialAlias1;
    @Column(name = "commercial_value_1") public String commercialValue1;
    @Column(name = "commercial_currency_1") public String commercialCurrency1;
    @Column(name = "commercial_alias_2") public String commercialAlias2;
    @Column(name = "commercial_value_2") public String commercialValue2;
    @Column(name = "commercial_currency_2") public String commercialCurrency2;
    @Column(name = "commercial_alias_3") public String commercialAlias3;
    @Column(name = "commercial_value_3") public String commercialValue3;
    @Column(name = "commercial_currency_3") public String commercialCurrency3;
    @Column(name = "commercial_alias_4") public String commercialAlias4;
    @Column(name = "commercial_value_4") public String commercialValue4;
    @Column(name = "commercial_currency_4") public String commercialCurrency4;
    @Column(name = "commercial_alias_5") public String commercialAlias5;
    @Column(name = "commercial_value_5") public String commercialValue5;
    @Column(name = "commercial_currency_5") public String commercialCurrency5;
    @Column(name = "commercial_alias_6") public String commercialAlias6;
    @Column(name = "commercial_value_6") public String commercialValue6;
    @Column(name = "commercial_currency_6") public String commercialCurrency6;
    @Column(name = "commercial_alias_7") public String commercialAlias7;
    @Column(name = "commercial_value_7") public String commercialValue7;
    @Column(name = "commercial_currency_7") public String commercialCurrency7;
    @Column(name = "commercial_alias_8") public String commercialAlias8;
    @Column(name = "commercial_value_8") public String commercialValue8;
    @Column(name = "commercial_currency_8") public String commercialCurrency8;
    @Column(name = "commercial_alias_9") public String commercialAlias9;
    @Column(name = "commercial_value_9") public String commercialValue9;
    @Column(name = "commercial_currency_9") public String commercialCurrency9;
    @Column(name = "consultancy_alias_1") public String consultancyAlias1;
    @Column(name = "consultancy_value_1") public String consultancyValue1;
    @Column(name = "consultancy_currency_1") public String consultancyCurrency1;
    @Column(name = "consultancy_alias_2") public String consultancyAlias2;
    @Column(name = "consultancy_value_2") public String consultancyValue2;
    @Column(name = "consultancy_currency_2") public String consultancyCurrency2;
    @Column(name = "consultancy_alias_3") public String consultancyAlias3;
    @Column(name = "consultancy_value_3") public String consultancyValue3;
    @Column(name = "consultancy_currency_3") public String consultancyCurrency3;
    @Column(columnDefinition = "TEXT")
    public String extraRows;
    @Column(columnDefinition = "TEXT")
    public String requiredDocuments;
    @Column(columnDefinition = "TEXT")
    public String timelineItems;
    @Column(columnDefinition = "TEXT")
    public String commercialRowOrder;
}
