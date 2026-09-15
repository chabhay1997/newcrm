package dto;

import java.time.LocalDate;

public record QuotationResponse(Long id, String createdBy, String invoiceNo, LocalDate date,
                                String attention, String clientName, String isCode, String companyName) {
}
