package dto;

import java.time.LocalDate;

public record ChallanReturnAlertResponse(Long id, String challanNo, String clientName,
                                         String itemName, LocalDate sampleReturnDate,
                                         long daysRemaining) {
}
