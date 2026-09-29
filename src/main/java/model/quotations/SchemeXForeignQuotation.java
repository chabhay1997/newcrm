package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "scheme_foreigns")
public class SchemeXForeignQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String clientName;
    public String productName;
    public String hsnNo;
    public String leadTime;
    public String consulCharge;
    public String airTickets;
    public String advance;
    public String completion;
    public String offVisit;
    public String duringGrant;
}