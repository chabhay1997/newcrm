package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.AssistantOperationFact;
import dto.AssistantOperationSummary;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssistantEvaluationSupport {

    private AssistantEvaluationSupport() {
    }

    static List<AssistantEvaluationCase> loadCases(
            ObjectMapper objectMapper
    ) throws IOException {
        try (InputStream input = AssistantEvaluationSupport.class
                .getResourceAsStream(
                        "/bis-isi-assistant-evaluation.json"
                )) {
            if (input == null) {
                throw new IOException("Assistant evaluation dataset is missing.");
            }

            return objectMapper.readValue(
                    input,
                    new TypeReference<>() {
                    }
            );
        }
    }

    static AssistantOperationSummary summary(String scenario) {
        boolean empty = "empty".equals(scenario);
        boolean dateRange = "date-range".equals(scenario);
        boolean truncated = "truncated".equals(scenario);

        long total = empty ? 0L : dateRange ? 10L : truncated ? 124L : 29L;
        Map<String, Long> statusCounts = processCounts(empty);
        Map<String, Long> monthlyCounts = monthlyCounts(empty, dateRange);
        Map<String, Long> procedureCounts = procedureCounts(empty);
        Map<String, Long> createdByCounts = new LinkedHashMap<>();
        createdByCounts.put("Divyanshu", empty ? 0L : total);
        createdByCounts.put("Dev", 0L);
        Map<String, Long> assignedToCounts = new LinkedHashMap<>();
        assignedToCounts.put("Vartika", empty ? 0L : 5L);

        AssistantOperationFact company = new AssistantOperationFact(
                101L,
                "ABC Industries",
                "IS 1234",
                LocalDate.of(2026, 4, 10),
                "Simplified",
                "Drafting",
                "Divyanshu",
                "Vartika",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 6, 4)
        );
        AssistantOperationFact dueSoon = new AssistantOperationFact(
                102L,
                "XYZ Industries",
                "IS 5678",
                LocalDate.of(2026, 9, 10),
                "Normal",
                "On Hold",
                "Divyanshu",
                "Vartika",
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 25)
        );
        List<AssistantOperationFact> records = empty
                ? List.of()
                : List.of(company, dueSoon);
        List<String> team = List.of(
                "Dev",
                "Prashansha",
                "Vartika",
                "Siya",
                "Vaishnavi",
                "Smriti",
                "Divyanshu"
        );

        return new AssistantOperationSummary(
                total,
                statusCounts,
                monthlyCounts,
                procedureCounts,
                createdByCounts,
                assignedToCounts,
                statusCounts,
                empty ? 0L : 4L,
                empty ? 0L : 2L,
                LocalDate.of(2026, 9, 22),
                team,
                team.size(),
                records,
                empty ? List.of() : List.of(company),
                empty ? List.of() : List.of(dueSoon),
                total,
                truncated ? 50 : records.size(),
                truncated,
                50,
                "Divyanshu",
                dateRange ? LocalDate.of(2026, 4, 1) : null,
                dateRange ? LocalDate.of(2026, 4, 30) : null,
                2026,
                4,
                "License Granted",
                "All",
                "All Users",
                "Divyanshu",
                "All",
                List.of()
        );
    }

    private static Map<String, Long> processCounts(boolean empty) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Status not set", empty ? 0L : 24L);
        counts.put("License Granted", empty ? 0L : 3L);
        counts.put("Inspection Completed", empty ? 0L : 1L);
        counts.put("On Hold", empty ? 0L : 1L);
        counts.put("Registration", 0L);
        return counts;
    }

    private static Map<String, Long> monthlyCounts(
            boolean empty,
            boolean dateRange
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String month : List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November",
                "December"
        )) {
            counts.put(month, 0L);
        }
        counts.put("April", empty ? 0L : dateRange ? 10L : 29L);
        return counts;
    }

    private static Map<String, Long> procedureCounts(boolean empty) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Simplified", empty ? 0L : 18L);
        counts.put("Normal", empty ? 0L : 11L);
        counts.put("Not set", 0L);
        return counts;
    }
}
