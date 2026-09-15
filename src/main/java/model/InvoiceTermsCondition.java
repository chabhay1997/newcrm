package model;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_terms_conditions")
public class InvoiceTermsCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invType")
    private Integer invType;

    @Column(name = "terms_condtions")
    @Lob
    private String termsCondtions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getInvType() { return invType; }
    public void setInvType(Integer invType) { this.invType = invType; }

    public String getTermsCondtions() { return termsCondtions; }
    public void setTermsCondtions(String termsCondtions) { this.termsCondtions = termsCondtions; }
}