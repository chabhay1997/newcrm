package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GroqClientLiveEvaluationTest {

    @TestFactory
    @EnabledIfEnvironmentVariable(
            named = "RUN_LIVE_GROQ_EVAL",
            matches = "true"
    )
    Stream<DynamicTest> evaluatesExpectedAndForbiddenFacts()
            throws IOException {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        List<AssistantEvaluationCase> allCases =
                AssistantEvaluationSupport.loadCases(objectMapper);
        int limit = evaluationLimit(allCases.size());
        String apiKey = System.getenv("GROQ_API_KEY");

        assertThat(apiKey).isNotBlank();

        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .build();
        GroqClient groq = new GroqClient(
                restClient,
                objectMapper,
                apiKey,
                "openai/gpt-oss-20b",
                300
        );
        AssistantContextSelector selector = new AssistantContextSelector();

        return allCases.stream()
                .limit(limit)
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.id(),
                        () -> evaluate(groq, selector, testCase)
                ));
    }

    private void evaluate(
            GroqClient groq,
            AssistantContextSelector selector,
            AssistantEvaluationCase testCase
    ) {
        var summary = AssistantEvaluationSupport.summary(
                testCase.scenario()
        );
        var context = selector.select(summary, testCase.question());
        String answer = groq.answer(
                context,
                testCase.question(),
                testCase.history()
        );
        String normalizedAnswer = normalize(answer);

        for (String expected : testCase.expectedFacts()) {
            assertThat(normalizedAnswer)
                    .as("%s expected fact: %s", testCase.id(), expected)
                    .contains(normalize(expected));
        }
        for (String forbidden : testCase.forbiddenFacts()) {
            assertThat(normalizedAnswer)
                    .as("%s forbidden fact: %s", testCase.id(), forbidden)
                    .doesNotContain(normalize(forbidden));
        }
    }

    private int evaluationLimit(int availableCases) {
        String configured = System.getenv("GROQ_EVAL_CASE_LIMIT");

        if (configured == null || configured.isBlank()) {
            return availableCases;
        }

        try {
            return Math.max(
                    1,
                    Math.min(Integer.parseInt(configured), availableCases)
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "GROQ_EVAL_CASE_LIMIT must be a positive integer.",
                    exception
            );
        }
    }

    private String normalize(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Z}]+", " ")
                .trim();
    }
}
