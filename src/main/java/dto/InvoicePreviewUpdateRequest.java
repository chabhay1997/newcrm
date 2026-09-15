package dto;

import java.time.LocalDate;
import java.util.List;

public record InvoicePreviewUpdateRequest(
        String invNo,
        LocalDate date,
        String poNo,
        String name,
        String address,
        String gstin,
        String remarks,
        List<PreviewLineItem> items
) {
    public record PreviewLineItem(String title, String particular, String amount) { }
}
