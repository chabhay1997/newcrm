package service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.AssistantChatMessage;
import dto.AssistantOperationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GroqClientLivePromptTest {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "RUN_LIVE_GROQ_TEST",
            matches = "true"
    )
    void answersExamplesAndFollowUpUsingIntentSpecificContext() {
        String apiKey = System.getenv("GROQ_API_KEY");
        assertThat(apiKey).isNotBlank();

        RestClient client = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .build();
        GroqClient groq = new GroqClient(
                client,
                JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        .build(),
                apiKey,
                "openai/gpt-oss-20b",
                300
        );

        AssistantOperationSummary context = exampleContext();
        AssistantContextSelector selector = new AssistantContextSelector();
        String aprilQuestion =
                "How many operations were completed in April?";
        String normalQuestion =
                "How many Normal procedures are there?";
        String followUpQuestion =
                "How many of those were License Granted?";

        String aprilAnswer = groq.answer(
                selector.select(context, aprilQuestion),
                aprilQuestion,
                List.of()
        );
        String normalAnswer = groq.answer(
                selector.select(context, normalQuestion),
                normalQuestion,
                List.of()
        );
        String followUpAnswer = groq.answer(
                selector.select(context, followUpQuestion),
                followUpQuestion,
                List.of(
                        new AssistantChatMessage(
                                "user",
                                aprilQuestion
                        ),
                        new AssistantChatMessage(
                                "assistant",
                                aprilAnswer
                        )
                )
        );

        assertThat(aprilAnswer)
                .contains("29")
                .containsIgnoringCase("April")
                .contains("2026");
        assertThat(normalAnswer)
                .contains("11")
                .containsIgnoringCase("Normal");
        assertThat(followUpAnswer)
                .contains("3")
                .containsIgnoringCase("License")
                .containsIgnoringCase("Granted");
    }

    private AssistantOperationSummary exampleContext() {
        Map<String, Long> monthlyCounts = new LinkedHashMap<>();
        monthlyCounts.put("April", 29L);

        Map<String, Long> procedureCounts = new LinkedHashMap<>();
        procedureCounts.put("Simplified", 18L);
        procedureCounts.put("Normal", 11L);

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
                29,
                Map.of("License Granted", 3L),
                monthlyCounts,
                procedureCounts,
                Map.of(),
                Map.of(),
                Map.of("License Granted", 3L),
                0,
                0,
                LocalDate.of(2026, 4, 30),
                team,
                team.size(),
                List.of(),
                List.of(),
                List.of(),
                29,
                0,
                false,
                50,
                "All Users",
                null,
                null,
                2026,
                4,
                "License Granted",
                "All",
                "All Users",
                "All Users",
                "All",
                List.of()
        );
    }
}
