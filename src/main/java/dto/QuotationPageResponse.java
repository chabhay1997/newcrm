package dto;

import java.util.List;

public record QuotationPageResponse(List<QuotationResponse> records, int currentPage, int pageSize,
                                    int totalPages, long totalRecords, boolean hasPrevious, boolean hasNext) {
}
