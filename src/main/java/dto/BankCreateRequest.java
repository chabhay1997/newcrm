package dto;

public class BankCreateRequest {
    private String bankDetails;
    private Integer bankName;
    private String branch;
    private String bankAcc;
    private String ifscCode;
    private Integer accType;
    private String micrCode;
    private String swiftCode;

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
}