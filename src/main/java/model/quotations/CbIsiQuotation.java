package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "quotations")
public class CbIsiQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "lead_fk_id", nullable = false)
    public Long leadId;
    @Column(name = "certificate_type")
    public Integer certificateType;
    @Column(name = "reference_no")
    public String referenceNo;
    @Column(name = "quotation_date")
    public LocalDate quotationDate;
    @Column(name = "company_name")
    public String companyName;
    @Column(name = "client_name")
    public String clientName;
    @Column(name = "certificate_name")
    public String certificateName;
    @Column(name = "custom_heading")
    public String customHeading; // optional proposal heading

    // dynamic certification standards:
    // [{"standard_code":"IS 17631","product_name":"Work Chair","application_fee":1000,
    //   "bis_officer_visit_charges":7000,"minimum_marking_fee":293000,"micro_concession_percent":80,
    //   "minimum_marking_fee_micro":58600,"license_fee":1000,"sample_testing_charges":48000,
    //   "services_fee":40000,"other_expenses":"As Per Actual","engineer_visit_travel_food":"Manufacture Site"}]
    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    public String scopeRowsJson;
}