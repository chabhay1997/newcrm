package model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_u_s_d_s")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_inr")
    private Integer isInr;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "status")
    private Integer status = 3;

    @Column(name = "inv_type")
    private Integer invType;

    // Export(1)/Tax(2) heading toggle — separate from invType (Evtl/Prolix/Government)
    @Column(name = "inv_types")
    private Integer invTypes;

    @Column(name = "bank_type")
    private Long bankType;

    @Column(name = "name")
    private String name;

    @Column(name = "inv_no")
    private String invNo;

    @Column(name = "is_terms")
    private Integer isTerms;

    @Column(name = "is_sign")
    private Long isSign;

    @Column(name = "po_no")
    private String poNo;

    @Column(name = "address")
    private String address;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "pan_no")
    private String panNo;

    @Column(name = "sac_code")
    private String sacCode;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "title", columnDefinition = "LONGTEXT")
    @Lob
    private String title;

    @Column(name = "particular", columnDefinition = "LONGTEXT")
    @Lob
    private String particular;

    @Column(name = "amt_d", columnDefinition = "LONGTEXT")
    @Lob
    private String amtD;

    @Column(name = "net_amt")
    private String netAmt;

    @Column(name = "extra_services")
    private String extraServices;

    @Column(name = "extra_amt")
    private String extraAmt;

    @Column(name = "final_amt")
    private String finalAmt;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIsInr() { return isInr; }
    public void setIsInr(Integer isInr) { this.isInr = isInr; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getInvType() { return invType; }
    public void setInvType(Integer invType) { this.invType = invType; }

    public Integer getInvTypes() { return invTypes; }
    public void setInvTypes(Integer invTypes) { this.invTypes = invTypes; }

    public Long getBankType() { return bankType; }
    public void setBankType(Long bankType) { this.bankType = bankType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInvNo() { return invNo; }
    public void setInvNo(String invNo) { this.invNo = invNo; }

    public Integer getIsTerms() { return isTerms; }
    public void setIsTerms(Integer isTerms) { this.isTerms = isTerms; }

    public Long getIsSign() { return isSign; }
    public void setIsSign(Long isSign) { this.isSign = isSign; }

    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public String getSacCode() { return sacCode; }
    public void setSacCode(String sacCode) { this.sacCode = sacCode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getParticular() { return particular; }
    public void setParticular(String particular) { this.particular = particular; }

    public String getAmtD() { return amtD; }
    public void setAmtD(String amtD) { this.amtD = amtD; }

    public String getNetAmt() { return netAmt; }
    public void setNetAmt(String netAmt) { this.netAmt = netAmt; }

    public String getExtraServices() { return extraServices; }
    public void setExtraServices(String extraServices) { this.extraServices = extraServices; }

    public String getExtraAmt() { return extraAmt; }
    public void setExtraAmt(String extraAmt) { this.extraAmt = extraAmt; }

    public String getFinalAmt() { return finalAmt; }
    public void setFinalAmt(String finalAmt) { this.finalAmt = finalAmt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
