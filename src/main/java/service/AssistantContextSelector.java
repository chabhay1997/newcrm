package service;

import dto.AssistantOperationSummary;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class AssistantContextSelector {

    private static final Set<String> MONTH_TERMS = Set.of(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november",
            "december", "month", "monthly"
    );
    private static final Set<String> TEAM_NAMES = Set.of(
            "dev", "prashansha", "vartika", "siya", "vaishnavi",
            "smriti", "divyanshu"
    );

    public Map<String, Object> select(
            AssistantOperationSummary summary,
            String question
    ) {
        AssistantQuestionCategory category = classify(question);
        Map<String, Object> context = baseContext(summary, category);

        switch (category) {
            case MONTHLY_SUMMARY -> {
                context.put("total", summary.total());
                context.put("monthlyCounts", summary.monthlyCounts());
                context.put("statusCounts", summary.statusCounts());
                context.put("processCounts", summary.processCounts());
                context.put("selectedMonth", summary.selectedMonth());
                context.put("analyticsProcess", summary.analyticsProcess());
            }
            case TEAM_PERFORMANCE -> {
                context.put("total", summary.total());
                context.put("createdByCounts", summary.createdByCounts());
                context.put("assignedToCounts", summary.assignedToCounts());
            }
            case COMPANY_LOOKUP -> {
                context.put(
                        "matchingRecordCount",
                        summary.matchingRecordCount()
                );
                context.put(
                        "includedRecordCount",
                        summary.includedRecordCount()
                );
                context.put("recordsTruncated", summary.recordsTruncated());
                context.put("recordLimit", summary.recordLimit());
                context.put("records", summary.records());
            }
            case DEADLINE -> {
                context.put("asOfDate", summary.asOfDate());
                context.put("overdueCount", summary.overdueCount());
                context.put("dueSoonCount", summary.dueSoonCount());
                context.put("overdueRecords", summary.overdueRecords());
                context.put("dueSoonRecords", summary.dueSoonRecords());
                context.put(
                        "overdueRecordsTruncated",
                        summary.overdueCount()
                                > summary.overdueRecords().size()
                );
                context.put(
                        "dueSoonRecordsTruncated",
                        summary.dueSoonCount()
                                > summary.dueSoonRecords().size()
                );
                context.put("recordLimit", summary.recordLimit());
            }
            case PROCEDURE_ANALYSIS -> {
                context.put("total", summary.total());
                context.put("procedureCounts", summary.procedureCounts());
            }
            case TEAM_MEMBERS -> {
                context.put(
                        "operationsTeamMembers",
                        summary.operationsTeamMembers()
                );
                context.put(
                        "operationsTeamMemberCount",
                        summary.operationsTeamMemberCount()
                );
            }
            case PROCESS_STATUS -> {
                context.put("total", summary.total());
                context.put("statusCounts", summary.statusCounts());
                context.put("processCounts", summary.processCounts());
            }
            case OVERALL_SUMMARY -> {
                context.put("total", summary.total());
                context.put("statusCounts", summary.statusCounts());
                context.put("monthlyCounts", summary.monthlyCounts());
                context.put("procedureCounts", summary.procedureCounts());
            }
        }

        return Collections.unmodifiableMap(context);
    }

    AssistantQuestionCategory classify(String question) {
        String normalized = question == null
                ? ""
                : question.toLowerCase(Locale.ROOT);

        if (containsAny(
                normalized,
                "operations team", "operation team", "team member",
                "team members", "roster", "how many members"
        )) {
            return AssistantQuestionCategory.TEAM_MEMBERS;
        }

        if (containsAny(
                normalized,
                "overdue", "due soon", "due this", "deadline",
                "target date", "final date", "upcoming due"
        )) {
            return AssistantQuestionCategory.DEADLINE;
        }

        if (containsAny(
                normalized,
                "company", "companies", "industry", "industries",
                "indian standard", "is number", "project lookup",
                "company lookup", "company details"
        )) {
            return AssistantQuestionCategory.COMPANY_LOOKUP;
        }

        if (containsAny(normalized, "procedure", "simplified", "normal")) {
            return AssistantQuestionCategory.PROCEDURE_ANALYSIS;
        }

        if (MONTH_TERMS.stream().anyMatch(normalized::contains)
                || containsAny(
                        normalized,
                        "current month", "selected month", "month to month"
                )) {
            return AssistantQuestionCategory.MONTHLY_SUMMARY;
        }

        if (containsAny(
                normalized,
                "assigned", "created by", "done by", "handled by",
                "filter by user", "team performance", "user performance"
        ) || TEAM_NAMES.stream().anyMatch(normalized::contains)) {
            return AssistantQuestionCategory.TEAM_PERFORMANCE;
        }

        if (containsAny(
                normalized,
                "process", "project status", "status", "drafting",
                "license granted", "inspection", "registration",
                "testing", "on hold", "query raised", "trf"
        )) {
            return AssistantQuestionCategory.PROCESS_STATUS;
        }

        return AssistantQuestionCategory.OVERALL_SUMMARY;
    }

    private Map<String, Object> baseContext(
            AssistantOperationSummary summary,
            AssistantQuestionCategory category
    ) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("startDate", summary.startDate());
        filters.put("endDate", summary.endDate());
        filters.put("procedure", summary.procedure());
        filters.put("assignedBy", summary.assignedBy());
        filters.put("filterByUser", summary.filterByUser());
        filters.put("status", summary.status());
        filters.put("selectedProcesses", summary.selectedProcesses());

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("questionCategory", category.name());
        context.put("analyticsYear", summary.analyticsYear());
        context.put(
                "appliedFilters",
                Collections.unmodifiableMap(filters)
        );
        return context;
    }

    private boolean containsAny(String source, String... terms) {
        return List.of(terms).stream().anyMatch(source::contains);
    }
}
