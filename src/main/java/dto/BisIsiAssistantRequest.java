package dto;

import java.time.LocalDate;
import java.util.List;

public record BisIsiAssistantRequest (
    String message,
    LocalDate startDate,
    LocalDate endDate,
    String procedure,
    Long engineer,
    Long creator,
    String status,
    List<String> processes,
    Integer analyticsYear,
    Integer selectedMonth,
    String analyticsProcess,
    List<AssistantChatMessage> history
){
    
}
