package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "wpc_quotations")
public class BisWpcQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String certificateName;
    public String clientName;
    public Integer qty;
    public String equipment;

    // repeatable Amount + Description rows, stored as a JSON array string
    // e.g. [{"amt":"5000","desc":"Testing charge"}, {"amt":"1200","desc":"Inspection fee"}]
    @Column(name = "items_json", columnDefinition = "TEXT")
    public String itemsJson;
}