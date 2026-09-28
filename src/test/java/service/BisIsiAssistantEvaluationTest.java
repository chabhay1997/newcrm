package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BisIsiAssistantEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssistantContextSelector selector =
            new AssistantContextSelector();

    @Test
    void evaluationSetHasUniqueCompleteCasesAndRequiredCoverage()
            throws IOException {
        List<AssistantEvaluationCase> cases =
                AssistantEvaluationSupport.loadCases(objectMapper);

        assertThat(cases).hasSizeBetween(30, 50);
        assertThat(cases)
                .allSatisfy(testCase -> {
                    assertThat(testCase.id()).isNotBlank();
                    assertThat(testCase.scenario()).isNotBlank();
                    assertThat(testCase.question()).isNotBlank();
                    assertThat(testCase.expectedCategory()).isNotNull();
                    assertThat(testCase.expectedFacts()).isNotEmpty();
                });
        assertThat(cases.stream().map(AssistantEvaluationCase::id))
                .doesNotHaveDuplicates();

        Set<String> tags = cases.stream()
                .flatMap(testCase -> testCase.tags().stream())
                .collect(Collectors.toSet());
        assertThat(tags).contains(
                "count",
                "selected-user",
                "month",
                "year",
                "empty",
                "missing-status",
                "date-range",
                "follow-up",
                "prompt-injection",
                "outside-scope",
                "sensitive"
        );
    }

    @Test
    void everyEvaluationQuestionRoutesToItsExpectedCategory()
            throws IOException {
        List<AssistantEvaluationCase> cases =
                AssistantEvaluationSupport.loadCases(objectMapper);

        assertThat(cases).allSatisfy(testCase ->
                assertThat(selector.classify(testCase.question()))
                        .as(testCase.id())
                        .isEqualTo(testCase.expectedCategory())
        );
    }

    @Test
    void evaluationContextsRemainIntentSpecificAndSafe()
            throws IOException {
        List<AssistantEvaluationCase> cases =
                AssistantEvaluationSupport.loadCases(objectMapper);

        for (AssistantEvaluationCase testCase : cases) {
            Map<String, Object> context = selector.select(
                    AssistantEvaluationSupport.summary(testCase.scenario()),
                    testCase.question()
            );

            assertThat(context)
                    .as(testCase.id())
                    .containsEntry(
                            "questionCategory",
                            testCase.expectedCategory().name()
                    );

            if (testCase.expectedCategory()
                    != AssistantQuestionCategory.COMPANY_LOOKUP) {
                assertThat(context).doesNotContainKey("records");
            }
            if (testCase.expectedCategory()
                    != AssistantQuestionCategory.DEADLINE) {
                assertThat(context)
                        .doesNotContainKeys(
                                "overdueRecords",
                                "dueSoonRecords"
                        );
            }
        }
    }
}
