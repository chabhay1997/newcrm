package dto;

import java.util.List;

public record QuotationPageResponse(List<QuotationResponse> records, int currentPage, int pageSize,
<<<<<<< HEAD
                                    int totalPages, long totalRecords, boolean hasPrevious, boolean hasNext,
                                    QuotationSummary summary) {
=======
                                    int totalPages, long totalRecords, boolean hasPrevious, boolean hasNext) {
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
}
