package service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroqClientPromptTest {

    @Test
    void definesTerminologyResponseRulesAndExamples() {
        String prompt = GroqClient.SYSTEM_PROMPT;

        assertThat(prompt)
                .contains("Use only the supplied authorized BIS-ISI context")
                .contains("\"done by\", \"created by\", and \"handled by\"")
                .contains("\"assigned\" and \"assigned to\"")
                .contains("\"process\" and \"project status\"")
                .contains("current filters")
                .contains("Status not set")
                .contains("1. Answer the question directly")
                .contains("2. Use exact server-calculated numbers")
                .contains("4. Do not calculate totals from truncated")
                .contains("5. If recordsTruncated is true")
                .contains("7. Never invent company names")
                .contains("8. Never reveal credentials")
                .contains("10. Use bullet points only")
                .contains("monthlyCounts.April = 29")
                .contains("There were 29 operations in April 2026")
                .contains("procedureCounts.Normal = 11")
                .contains("There are 11 Normal-procedure operations");
    }

    @Test
    void makesServerAggregatesAuthoritative() {
        assertThat(GroqClient.SYSTEM_PROMPT)
                .contains("Never recount or estimate values from records")
                .contains("operationsTeamMemberCount")
                .contains("matchingRecordCount is the complete authorized")
                .contains("includedRecordCount");
    }
}
