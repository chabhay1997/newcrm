package service;

import dto.AssistantOperationFact;
import dto.AssistantOperationSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantContextSelectorTest {

    private final AssistantContextSelector selector =
            new AssistantContextSelector();

    @Test
    void classifiesTheSupportedQuestionCategories() {
        assertThat(selector.classify("Give me the April summary"))
                .isEqualTo(AssistantQuestionCategory.MONTHLY_SUMMARY);
        assertThat(selector.classify("Operations done by Dev"))
                .isEqualTo(AssistantQuestionCategory.TEAM_PERFORMANCE);
        assertThat(selector.classify("Show ABC company details"))
                .isEqualTo(AssistantQuestionCategory.COMPANY_LOOKUP);
        assertThat(selector.classify("Which operations are overdue?"))
                .isEqualTo(AssistantQuestionCategory.DEADLINE);
        assertThat(selector.classify("How many Normal procedures?"))
                .isEqualTo(AssistantQuestionCategory.PROCEDURE_ANALYSIS);
        assertThat(selector.classify("Who are the Operations team members?"))
                .isEqualTo(AssistantQuestionCategory.TEAM_MEMBERS);
        assertThat(selector.classify("Give the Drafting process count"))
                .isEqualTo(AssistantQuestionCategory.PROCESS_STATUS);
        assertThat(selector.classify("Give me a summary"))
                .isEqualTo(AssistantQuestionCategory.OVERALL_SUMMARY);
    }

    @Test
    void procedureQuestionsExcludeCompanyRecordsAndUnrelatedCounts() {
        Map<String, Object> context = selector.select(
                summary(),
                "How many Simplified procedures are there?"
        );

        assertThat(context)
                .containsKeys(
                        "questionCategory",
                        "analyticsYear",
                        "appliedFilters",
                        "total",
                        "procedureCounts"
                )
                .doesNotContainKeys(
                        "records",
                        "createdByCounts",
                        "assignedToCounts",
                        "overdueRecords",
                        "operationsTeamMembers"
                );
    }

    @Test
    void companyQuestionsReceiveOnlyCappedSafeRecordContext() {
        Map<String, Object> context = selector.select(
                summary(),
                "Show company details for ABC Industries"
        );

        assertThat(context)
                .containsKeys(
                        "matchingRecordCount",
                        "includedRecordCount",
                        "recordsTruncated",
                        "recordLimit",
                        "records"
                )
                .doesNotContainKeys(
                        "statusCounts",
                        "monthlyCounts",
                        "procedureCounts",
                        "createdByCounts",
                        "assignedToCounts"
                );
    }

    @Test
    void deadlineQuestionsReceiveOnlyDeadlineSpecificSafeRecords() {
        Map<String, Object> context = selector.select(
                summary(),
                "Which operations are overdue?"
        );

        assertThat(context)
                .containsKeys(
                        "asOfDate",
                        "overdueCount",
                        "dueSoonCount",
                        "overdueRecords",
                        "dueSoonRecords",
                        "overdueRecordsTruncated",
                        "dueSoonRecordsTruncated"
                )
                .doesNotContainKeys(
                        "records",
                        "monthlyCounts",
                        "procedureCounts",
                        "createdByCounts"
                );
    }

    private AssistantOperationSummary summary() {
        AssistantOperationSummary summary = mock(
                AssistantOperationSummary.class
        );
        AssistantOperationFact fact = mock(AssistantOperationFact.class);

        when(summary.analyticsYear()).thenReturn(2026);
        when(summary.procedure()).thenReturn("All");
        when(summary.assignedBy()).thenReturn("All Users");
        when(summary.filterByUser()).thenReturn("All Users");
        when(summary.status()).thenReturn("All");
        when(summary.selectedProcesses()).thenReturn(List.of());
        when(summary.total()).thenReturn(29L);
        when(summary.statusCounts()).thenReturn(Map.of("Drafting", 1L));
        when(summary.monthlyCounts()).thenReturn(Map.of("April", 29L));
        when(summary.procedureCounts()).thenReturn(Map.of("Normal", 11L));
        when(summary.createdByCounts()).thenReturn(Map.of("Dev", 10L));
        when(summary.assignedToCounts()).thenReturn(Map.of("Siya", 5L));
        when(summary.processCounts()).thenReturn(Map.of("Drafting", 1L));
        when(summary.asOfDate()).thenReturn(LocalDate.of(2026, 9, 22));
        when(summary.overdueCount()).thenReturn(1L);
        when(summary.dueSoonCount()).thenReturn(1L);
        when(summary.operationsTeamMembers()).thenReturn(List.of("Dev"));
        when(summary.operationsTeamMemberCount()).thenReturn(1);
        when(summary.records()).thenReturn(List.of(fact));
        when(summary.overdueRecords()).thenReturn(List.of(fact));
        when(summary.dueSoonRecords()).thenReturn(List.of(fact));
        when(summary.matchingRecordCount()).thenReturn(1L);
        when(summary.includedRecordCount()).thenReturn(1);
        when(summary.recordLimit()).thenReturn(50);

        return summary;
    }
}
