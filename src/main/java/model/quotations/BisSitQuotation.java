package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "s_i_t_s")
public class BisSitQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String clientName;
    public String duration;
    public Integer noOfCertificate;

    // up to 3 certification/product names, comma-joined
    @Column(name = "certification_name", columnDefinition = "TEXT")
    public String certificationName;

    public String actualFee;
    public String renewalFeeCount;
    public String renewalFee;
    public String annualLicCount;
    public String annualFee;
    public String bisOfficerLiaisoningFee;
    public String sampleTestingFee;
    public String engVisitFee;
    public String consultancyServiceFee;
}