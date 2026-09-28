package service;

import dto.AssistantOperationSummary;
import dto.AssistantChatMessage;
import dto.BisIsiAssistantRequest;
import model.User;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BisIsiAssistantService {

    static final int MAX_HISTORY_MESSAGES = 6;
    static final int MAX_HISTORY_MESSAGE_CHARACTERS = 2_000;
    static final int MAX_HISTORY_TOTAL_CHARACTERS = 8_000;

    private final BisIsiService bisIsiService;
    private final GroqClient groqClient;
    private final AssistantContextSelector contextSelector;

    public BisIsiAssistantService(
            BisIsiService bisIsiService,
            GroqClient groqClient,
            AssistantContextSelector contextSelector
    ) {
        this.bisIsiService = bisIsiService;
        this.groqClient = groqClient;
        this.contextSelector = contextSelector;
    }

    public String answer(
            User currentUser,
            BisIsiAssistantRequest request
    ) {
        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The assistant request is required."
            );
        }

        List<AssistantChatMessage> history = validatedHistory(
                request.history()
        );

        AssistantOperationSummary summary =
                bisIsiService.assistantSummary(
                        currentUser,
                        request
                );

        return groqClient.answer(
                contextSelector.select(summary, request.message()),
                request.message(),
                history
        );
    }

    private List<AssistantChatMessage> validatedHistory(
            List<AssistantChatMessage> submittedHistory
    ) {
        if (submittedHistory == null || submittedHistory.isEmpty()) {
            return List.of();
        }

        if (submittedHistory.size() > MAX_HISTORY_MESSAGES) {
            throw badHistory(
                    "Conversation history cannot exceed "
                            + MAX_HISTORY_MESSAGES + " messages."
            );
        }

        List<AssistantChatMessage> validated = new ArrayList<>();
        int totalCharacters = 0;

        for (AssistantChatMessage message : submittedHistory) {
            if (message == null
                    || !("user".equals(message.role())
                    || "assistant".equals(message.role()))) {
                throw badHistory(
                        "Conversation history roles must be user or assistant."
                );
            }

            if (message.content() == null
                    || message.content().isBlank()) {
                throw badHistory(
                        "Conversation history messages cannot be empty."
                );
            }

            String content = message.content().trim();

            if (content.length() > MAX_HISTORY_MESSAGE_CHARACTERS) {
                throw badHistory(
                        "A conversation history message cannot exceed "
                                + MAX_HISTORY_MESSAGE_CHARACTERS
                                + " characters."
                );
            }

            totalCharacters += content.length();

            if (totalCharacters > MAX_HISTORY_TOTAL_CHARACTERS) {
                throw badHistory(
                        "Conversation history cannot exceed "
                                + MAX_HISTORY_TOTAL_CHARACTERS
                                + " total characters."
                );
            }

            validated.add(new AssistantChatMessage(
                    message.role(),
                    content
            ));
        }

        return List.copyOf(validated);
    }

    private ResponseStatusException badHistory(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
