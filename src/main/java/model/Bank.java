package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_details")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_details", length = 155)
    private String bankDetails;

    // tinyint(3) UNSIGNED — numeric code pointing to a bank name list (see BankNameResolver)
    @Column(name = "bank_name")
    private Integer bankName;

    @Column(name = "branch")
    private String branch;

    @Column(name = "bank_Acc", length = 51)
    private String bankAcc;

    @Column(name = "ifsc_code", length = 21)
    private String ifscCode;

    // tinyint(3) UNSIGNED — numeric code pointing to account type list
    @Column(name = "acc_type")
    private Integer accType;

    @Column(name = "micr_code", length = 21)
    private String micrCode;

    @Column(name = "swift_code")
    private String swiftCode;

    // The legacy bank_details table does not retain an author reference.
    @Transient
    private Long createdBy;

    // Legacy bank records are all available for use; there is no status column.
    @Transient
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBankDetails() { return bankDetails; }
    public void setBankDetails(String bankDetails) { this.bankDetails = bankDetails; }

    public Integer getBankName() { return bankName; }
    public void setBankName(Integer bankName) { this.bankName = bankName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getBankAcc() { return bankAcc; }
    public void setBankAcc(String bankAcc) { this.bankAcc = bankAcc; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public Integer getAccType() { return accType; }
    public void setAccType(Integer accType) { this.accType = accType; }

    public String getMicrCode() { return micrCode; }
    public void setMicrCode(String micrCode) { this.micrCode = micrCode; }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
