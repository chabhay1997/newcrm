package model;

import jakarta.persistence.*;

@Entity
@Table(name = "isi_checklists", uniqueConstraints = @UniqueConstraint(columnNames = "operation_id"))
public class IsiChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    private String companyName;
    private String clientName;
    private String indianStandard;
    private String contactNumber;
    private String location;

    @Column(name = "procedure_code")
    private String procedure;

    private Long assignedEngineerId;

    public Long getOperationId() { return operationId; }
    public void setOperationId(Long value) { operationId = value; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long value) { createdBy = value; }
    public void setCompanyName(String value) { companyName = value; }
    public void setClientName(String value) { clientName = value; }
    public void setIndianStandard(String value) { indianStandard = value; }
    public void setContactNumber(String value) { contactNumber = value; }
    public void setLocation(String value) { location = value; }
    public void setProcedure(String value) { procedure = value; }
    public void setAssignedEngineerId(Long value) { assignedEngineerId = value; }
}
