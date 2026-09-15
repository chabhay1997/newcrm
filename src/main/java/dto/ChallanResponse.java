package dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChallanResponse(Long id, String challanNo, String clientName, String itemName,
                              String brandName, String qty, String amount, LocalDate date,
                              String createdBy, LocalDateTime createdAt) {
}
