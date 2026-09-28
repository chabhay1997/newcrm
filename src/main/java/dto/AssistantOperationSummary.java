package dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AssistantOperationSummary(
    long total,
    Map<String, Long> statusCounts,
    Map<String, Long> monthlyCounts,
    Map<String, Long> procedureCounts,
    Map<String, Long> createdByCounts,
    Map<String, Long> assignedToCounts,
    Map<String, Long> processCounts,
    long overdueCount,
    long dueSoonCount,
    LocalDate asOfDate,
    List<String> operationsTeamMembers,
    int operationsTeamMemberCount,
    List<AssistantOperationFact> records,
    List<AssistantOperationFact> overdueRecords,
    List<AssistantOperationFact> dueSoonRecords,
    long matchingRecordCount,
    int includedRecordCount,
    boolean recordsTruncated,
    int recordLimit,
    String selectedUser,
    LocalDate startDate,
    LocalDate endDate,
    int analyticsYear,
    Integer selectedMonth,
    String analyticsProcess,
    String procedure,
    String assignedBy,
    String filterByUser,
    String status,
    List<String> selectedProcesses
) {
    
}
