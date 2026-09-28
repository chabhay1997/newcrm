package dto;

import java.time.LocalDate;

/**
 * Allowlisted BIS-ISI operation data that may be sent to the AI provider.
 * Never add contact, credential, authentication, or session fields here.
 */
public record AssistantOperationFact(
        Long operationId,
        String companyName,
        String indianStandard,
        LocalDate operationDate,
        String procedure,
        String projectStatus,
        String createdBy,
        String assignedTo,
        LocalDate targetDate,
        LocalDate finalDate
) {
}
