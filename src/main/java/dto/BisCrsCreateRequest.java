package dto;

import java.time.LocalDate;

public record BisCrsCreateRequest(
        String manufacturerName,
        String productName,
        String licenceNumber,
        String isStandard,
        LocalDate licenceDate,
        LocalDate expiryDate,
        LocalDate notifyDate,
        String brandName,
        String airName,
        String airAddress,
        String airEmailId,
        String airContactNumber,
        String currentStatus
) { }
