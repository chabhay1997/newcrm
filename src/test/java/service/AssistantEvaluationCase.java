package service;

import dto.AssistantChatMessage;

import java.util.List;

record AssistantEvaluationCase(
        String id,
        String scenario,
        List<String> tags,
        String question,
        AssistantQuestionCategory expectedCategory,
        List<String> expectedFacts,
        List<String> forbiddenFacts,
        List<AssistantChatMessage> history
) {
    AssistantEvaluationCase {
        tags = tags == null ? List.of() : List.copyOf(tags);
        expectedFacts = expectedFacts == null
                ? List.of()
                : List.copyOf(expectedFacts);
        forbiddenFacts = forbiddenFacts == null
                ? List.of()
                : List.copyOf(forbiddenFacts);
        history = history == null ? List.of() : List.copyOf(history);
    }
}
