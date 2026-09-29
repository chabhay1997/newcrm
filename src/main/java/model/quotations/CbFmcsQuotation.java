package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "cb_fmcs")
public class CbFmcsQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false) public Long leadId;
    @Column(name = "reference_no") public String referenceNo;
    @Column(name = "quotation_date") public LocalDate quotationDate;
    @Column(name = "certificate_type") public String certificateType;
    @Column(name = "client_name") public String clientName;
    @Column(name = "company_name") public String companyName;
    @Column(name = "product_name") public String productName;
    @Column(name = "product_category_class") public String productCategoryClass;
    @Column(name = "certificate_name") public String certificateName;
    @Column(name = "manufacturing_location") public String manufacturingLocation;
    @Column(name = "product_models") public String productModels;

    // 1. Total Service Contract Value
    @Column(name = "cont_val_inr") public String contValInr;
    @Column(name = "no_of_applications") public String noOfApplications;
    @Column(name = "contract_value") public String contractValue;
    @Column(name = "paid_to_remark") public String paidToRemark;

    // 2. BIS Govt. Expenses
    @Column(name = "applicant_value") public String applicantValue;
    @Column(name = "applicant_remark") public String applicantRemark;

    @Column(name = "a1_label") public String a1Label;
    @Column(name = "a1_inr") public String a1Inr;
    @Column(name = "a1_usd") public String a1Usd;
    @Column(name = "a1_remark") public String a1Remark;
    @Column(name = "a2_label") public String a2Label;
    @Column(name = "a2_inr") public String a2Inr;
    @Column(name = "a2_usd") public String a2Usd;
    @Column(name = "a2_remark") public String a2Remark;
    @Column(name = "a3_label") public String a3Label;
    @Column(name = "a3_inr") public String a3Inr;
    @Column(name = "a3_usd") public String a3Usd;
    @Column(name = "a3_remark") public String a3Remark;
    @Column(name = "a4_label") public String a4Label;
    @Column(name = "a4_inr") public String a4Inr;
    @Column(name = "a4_usd") public String a4Usd;
    @Column(name = "a4_remark") public String a4Remark;
    @Column(name = "a5_label") public String a5Label;
    @Column(name = "a5_inr") public String a5Inr;
    @Column(name = "a5_usd") public String a5Usd;
    @Column(name = "a5_remark") public String a5Remark;
    @Column(name = "a6_label") public String a6Label;
    @Column(name = "a6_inr") public String a6Inr;
    @Column(name = "a6_usd") public String a6Usd;
    @Column(name = "a6_remark") public String a6Remark;
    @Column(name = "a7_label") public String a7Label;
    @Column(name = "a7_inr") public String a7Inr;
    @Column(name = "a7_usd") public String a7Usd;
    @Column(name = "a7_remark") public String a7Remark;
    @Column(name = "a8_label") public String a8Label;
    @Column(name = "a8_inr") public String a8Inr;
    @Column(name = "a8_usd") public String a8Usd;
    @Column(name = "a8_remark") public String a8Remark;
    @Column(name = "a9_label") public String a9Label;
    @Column(name = "a9_inr") public String a9Inr;
    @Column(name = "a9_usd") public String a9Usd;
    @Column(name = "a9_remark") public String a9Remark;
    @Column(name = "a10_label") public String a10Label;
    @Column(name = "a10_inr") public String a10Inr;
    @Column(name = "a10_usd") public String a10Usd;
    @Column(name = "a10_remark") public String a10Remark;
    @Column(name = "a11_label") public String a11Label;
    @Column(name = "a11_inr") public String a11Inr;
    @Column(name = "a11_usd") public String a11Usd;
    @Column(name = "a11_remark") public String a11Remark;
    @Column(name = "a12_label") public String a12Label;
    @Column(name = "a12_inr") public String a12Inr;
    @Column(name = "a12_usd") public String a12Usd;
    @Column(name = "a12_remark") public String a12Remark;
    @Column(name = "a13_label") public String a13Label;
    @Column(name = "a13_inr") public String a13Inr;
    @Column(name = "a13_usd") public String a13Usd;
    @Column(name = "a13_remark") public String a13Remark;
    @Column(name = "a14_label") public String a14Label;
    @Column(name = "a14_inr") public String a14Inr;
    @Column(name = "a14_usd") public String a14Usd;
    @Column(name = "a14_remark") public String a14Remark;
    @Column(name = "a15_label") public String a15Label;
    @Column(name = "a15_inr") public String a15Inr;
    @Column(name = "a15_usd") public String a15Usd;
    @Column(name = "a15_remark") public String a15Remark;
    @Column(name = "a16_label") public String a16Label;
    @Column(name = "a16_inr") public String a16Inr;
    @Column(name = "a16_usd") public String a16Usd;
    @Column(name = "a16_remark") public String a16Remark;
    @Column(name = "a17_label") public String a17Label;
    @Column(name = "a17_inr") public String a17Inr;
    @Column(name = "a17_usd") public String a17Usd;
    @Column(name = "a17_remark") public String a17Remark;
    @Column(name = "bank_inr") public String bankInr;
    @Column(name = "bank_usd") public String bankUsd;
    @Column(name = "total_inr") public String totalInr;
    @Column(name = "total_usd") public String totalUsd;

    // 3. Travelling expenses (borne by applicant)
    @Column(name = "b1_label") public String b1Label;
    @Column(name = "b1_inr") public String b1Inr;
    @Column(name = "b1_usd") public String b1Usd;
    @Column(name = "b1_remark") public String b1Remark;
    @Column(name = "b2_label") public String b2Label;
    @Column(name = "b2_inr") public String b2Inr;
    @Column(name = "b2_usd") public String b2Usd;
    @Column(name = "b2_remark") public String b2Remark;
    @Column(name = "b3_label") public String b3Label;
    @Column(name = "b3_inr") public String b3Inr;
    @Column(name = "b3_usd") public String b3Usd;
    @Column(name = "b3_remark") public String b3Remark;
    @Column(name = "total_binr") public String totalBInr;
    @Column(name = "total_busd") public String totalBUsd;

    @Column(name = "grand_total_inr") public String grandTotalInr;
    @Column(name = "grand_total_usd") public String grandTotalUsd;

    @Column(name = "extra_rows", columnDefinition = "TEXT") public String extraRows;
    @Column(name = "row_order") public String rowOrder;
}