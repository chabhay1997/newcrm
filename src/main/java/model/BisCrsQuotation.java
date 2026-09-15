package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "b_i_s_c_r_s")
public class BisCrsQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public String certificateType;
    public String productName;
    public String clientName;
    @Column(name = "date")
    public LocalDate quotationDate;
    @Column(name = "qty") public String quantity;
    public String subject;
    @Column(name = "step_1") public String step1;
    @Column(name = "step_2") public String step2;
    @Column(name = "step_3") public String step3;
    public String bisAppFees;
    public String bisCustom;
    public String proTestFees;
    public String proCustom;
    public String applicationFee;
    public String testingFee;
    public String serviceFee;
    public Boolean liaisoningFeeApplicable;
    public String liaisoningFee;
    @Column(name = "is_currency") public Boolean currencyUsd;
}
