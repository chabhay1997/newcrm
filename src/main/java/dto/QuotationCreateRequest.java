package dto;

import java.time.LocalDate;
import java.util.List;

public record QuotationCreateRequest(String invoiceNo, LocalDate date, String attention, String clientName,
                                    String companyName, String isCode, List<QuotationLineItemRequest> items) {
}
