package dto;

public record BankRowDTO(
        Long id,
        String createdBy,
        String bankName,
        String accountNo,
        String ifscCode,
        String accountType,
        Integer status) {
}
