package dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChallanDetailsResponse(Long id, String challanNo, String clientName, String clientNumber,
                                     String itemName, String brandName, String state, String qty,
                                     String amount, String gst, String totalAmount, LocalDate date,
                                     LocalDate sampleReturnDate, String address, String pincode,
                                     String remark, String createdBy, LocalDateTime createdAt) {
}
