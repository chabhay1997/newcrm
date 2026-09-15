package dto;

public class InvoiceRowDTO {
    private Long id;
    private String createdBy;
    private String createdDate;
    private String name;      // html (link + download icon)
    private String invNo;
    private String poNo;
    private String finalAmount;
    private String officeName;
    private String status;    // html select
    private String action;    // html button

    public InvoiceRowDTO(Long id, String createdBy, String createdDate, String name, String invNo, String poNo,
                         String finalAmount, String officeName, String status, String action) {
        this.id = id;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.name = name;
        this.invNo = invNo;
        this.poNo = poNo;
        this.finalAmount = finalAmount;
        this.officeName = officeName;
        this.status = status;
        this.action = action;
    }

    public Long getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedDate() { return createdDate; }
    public String getName() { return name; }
    public String getInvNo() { return invNo; }
    public String getPoNo() { return poNo; }
    public String getFinalAmount() { return finalAmount; }
    public double getAmount() {
        if (finalAmount == null || finalAmount.isBlank()) return 0D;
        try { return Double.parseDouble(finalAmount.replace(",", "").trim()); }
        catch (NumberFormatException ignored) { return 0D; }
    }
    public String getOfficeName() { return officeName; }
    public String getStatus() { return status; }
    public String getAction() { return action; }
}
