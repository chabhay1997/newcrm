package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "lmpc_quotations")
public class BisLmpcQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    public String referenceNo;
    public LocalDate quotationDate;
    public String clientName;
    public String certificationName;
    public String leadTime;
    @Column(name = "lmpc_regis")
    public String lmpcRegis;
    public String serviceLiaison;
}