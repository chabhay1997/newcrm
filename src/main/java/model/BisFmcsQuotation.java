package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "bis_fmcs")
public class BisFmcsQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String certificateType;
    public String productName;
    public String clientName;
    public String companyName;
    public String processTime;
    public String contValInr;
    public String contractValue;
    public String inrStandard;
    public String applicantValue;
    public String applicantRemark;
    @Column(name = "a1_inr") public String a1Inr;
    @Column(name = "a1_usd") public String a1Usd;
    @Column(name = "a2_inr") public String a2Inr;
    @Column(name = "a2_usd") public String a2Usd;
    @Column(name = "a3_inr") public String a3Inr;
    @Column(name = "a3_usd") public String a3Usd;
    @Column(name = "a4_inr") public String a4Inr;
    @Column(name = "a4_usd") public String a4Usd;
    @Column(name = "a5_inr") public String a5Inr;
    @Column(name = "a5_usd") public String a5Usd;
    @Column(name = "a6_inr") public String a6Inr;
    @Column(name = "a6_usd") public String a6Usd;
    @Column(name = "a7_inr") public String a7Inr;
    @Column(name = "a7_usd") public String a7Usd;
    @Column(name = "bank_inr") public String bankInr;
    @Column(name = "bank_usd") public String bankUsd;
    @Column(name = "total_inr") public String totalInr;
    @Column(name = "total_usd") public String totalUsd;
    @Column(name = "b1_inr") public String b1Inr;
    @Column(name = "b1_usd") public String b1Usd;
    @Column(name = "b2_inr") public String b2Inr;
    @Column(name = "b2_usd") public String b2Usd;
    @Column(name = "b3_inr") public String b3Inr;
    @Column(name = "b3_usd") public String b3Usd;
    @Column(name = "total_binr") public String totalBInr;
    @Column(name = "total_busd") public String totalBUsd;
    public String grandTotalInr;
    public String grandTotalUsd;
    @Column(name = "extra_rows", columnDefinition = "TEXT")
    public String extraRows;
    @Column(name = "row_order") public String rowOrder;
}
