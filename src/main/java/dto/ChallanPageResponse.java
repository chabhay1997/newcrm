package dto;

import java.util.List;

public record ChallanPageResponse(List<ChallanResponse> records, int currentPage, int pageSize,
                                  int totalPages, long totalRecords, boolean hasPrevious,
                                  boolean hasNext) {
}
