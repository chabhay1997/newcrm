package service.leads_assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Lead;
import model.LeadAssistant;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import repository.LeadRepository;
import repository.leads_assistant.LeadAssistantRepository;
import util.LeadSource;
import util.LeadStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LeadAssistantService {

    private static final Logger log = LoggerFactory.getLogger(LeadAssistantService.class);
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "groq/compound-mini";
    private static final int GROQ_CONTEXT_LEAD_LIMIT = 50;
    private static final String OUT_OF_SCOPE_NOTICE = "This is out of reach for my static knowledge. I need to discuss it with an admin. ";
    private static final String[] COMMON_WORDS = {
            "hello", "hi", "hey", "yes", "no", "thanks", "thank", "bye", "goodbye",
            "help", "who", "what", "are", "you", "your", "name", "can", "do", "how",
            "many", "total", "count", "lead", "leads", "status", "source", "summary",
            "find", "search", "show", "converted", "pending", "follow", "up", "wrong",
            "call", "picked", "website", "referral", "pipeline", "customer", "client", "prospect",
            "hot", "ok", "okay", "and", "about", "all", "the", "is", "ups"
    };

    private final LeadRepository leadRepository;
    private final LeadAssistantRepository leadAssistantRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, String> pendingLeadLists = new ConcurrentHashMap<>();
    private final Map<String, List<String>> pendingMemorySearches = new ConcurrentHashMap<>();
    private final Map<String, LeadMemoryResult> selectedMemoryResults = new ConcurrentHashMap<>();

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.model:groq/compound-mini}")
    private String groqModel;

    public LeadAssistantService(LeadRepository leadRepository, LeadAssistantRepository leadAssistantRepository, ObjectMapper objectMapper) {
        this.leadRepository = leadRepository;
        this.leadAssistantRepository = leadAssistantRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> respond(String sessionId, String message, boolean enhanced) {
        List<Map<String, Object>> leadSnapshot = snapshotLeads();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", enhanced && hasGroqKey() ? "groq" : "static");
        response.put("leadCount", leadSnapshot.size());
        saveMessage(sessionId, "user", message, enhanced && hasGroqKey() ? "groq" : "static", null, null);

        if (!enhanced || !hasGroqKey()) {
            String reply = staticReply(sessionId, message, leadSnapshot);
            response.put("reply", reply);
            LeadMemoryResult memoryResult = selectedMemoryResults.remove(sessionId);
            String leadRecordsJson = null;
            Integer leadRecordTotal = null;
            if (memoryResult != null && memoryResult.total > 0) {
                response.put("leadRecords", memoryResult.records);
                response.put("leadRecordTotal", memoryResult.total);
                leadRecordTotal = memoryResult.total;
                try {
                    leadRecordsJson = objectMapper.writeValueAsString(memoryResult.records);
                } catch (Exception ignored) {
                    leadRecordsJson = null;
                }
            }
            saveMessage(sessionId, "assistant", reply, "static", leadRecordsJson, leadRecordTotal);
            if (enhanced && !hasGroqKey()) {
                response.put("notice", "Add GROQ_API_KEY on the server to enable Enhanced mode.");
            }
            return response;
        }

        if (isLatestLeadQuestion(message)) {
            Map<String, Object> latestLead = findLatestLead(leadSnapshot);
            if (latestLead != null) {
                String reply = describeLatestLead(latestLead);
                response.put("reply", reply);
                saveMessage(sessionId, "assistant", reply, "groq", null, null);
                return response;
            }
        }

        try {
            String reply = callGroq(message, leadSnapshot);
            response.put("reply", reply);
            saveMessage(sessionId, "assistant", reply, "groq", null, null);
        } catch (Exception exception) {
            log.warn("Groq enhanced-mode request failed", exception);
            String reply = staticReply(sessionId, message, leadSnapshot);
            response.put("reply", reply);
            saveMessage(sessionId, "assistant", reply, "static", null, null);
            String reason = exception.getMessage();
            response.put("notice", "Enhanced mode failed: "
                    + (reason == null || reason.isBlank() ? exception.getClass().getSimpleName() : reason));
        }
        return response;
    }

    public List<Map<String, Object>> history(String sessionId) {
        List<Map<String, Object>> history = new ArrayList<>();
        for (LeadAssistant message : leadAssistantRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(sessionId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            item.put("mode", message.getMode());
            item.put("createdAt", message.getCreatedAt());
            if (message.getLeadRecordsJson() != null && !message.getLeadRecordsJson().isBlank()) {
                try {
                    item.put("leadRecords", objectMapper.readValue(message.getLeadRecordsJson(), List.class));
                    item.put("leadRecordTotal", message.getLeadRecordTotal());
                } catch (Exception ignored) {
                    // Leave leadRecords out if the stored JSON can't be parsed.
                }
            }
            history.add(item);
        }
        return history;
    }

    public void clearHistory(String sessionId) {
        leadAssistantRepository.softDeleteBySessionId(sessionId, java.time.LocalDateTime.now());
        pendingLeadLists.remove(sessionId);
        pendingMemorySearches.remove(sessionId);
        selectedMemoryResults.remove(sessionId);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60 * 60 * 1000)
    public void purgeExpiredLeadAssistantHistory() {
        leadAssistantRepository.deleteByDeletedAtBefore(java.time.LocalDateTime.now().minusHours(24));
    }

    private void saveMessage(String sessionId, String role, String content, String mode, String leadRecordsJson, Integer leadRecordTotal) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) return;
        LeadAssistant message = new LeadAssistant();
        message.setSessionId(sessionId.trim());
        message.setRole(role);
        message.setContent(content.trim());
        message.setMode(mode);
        message.setLeadRecordsJson(leadRecordsJson);
        message.setLeadRecordTotal(leadRecordTotal);
        message.setCreatedAt(java.time.LocalDateTime.now());
        leadAssistantRepository.save(message);
    }

    private boolean hasGroqKey() {
        return groqApiKey != null && !groqApiKey.isBlank();
    }

    private String staticReply(String sessionId, String message, List<Map<String, Object>> leads) {
        String normalized = normalizeIntentText(message);
        int leadCount = leads.size();
        String pendingStatus = pendingLeadLists.get(sessionId);
        if (pendingStatus != null) {
            DateRange range = parseDateRange(message);
            if (range == null) {
                return "Please enter a date range like \"this month to this month\", \"last month to this month\", or \"2026-09-01 to 2026-09-09\".";
            }
            List<Map<String, Object>> filtered = leads.stream()
                    .filter(lead -> pendingStatus.equalsIgnoreCase(String.valueOf(lead.get("status"))))
                    .filter(lead -> isWithinRange(lead.get("leadDate"), range))
                    .toList();
            if (filtered.size() > 20) {
                return "There are " + filtered.size() + " " + pendingStatus + " leads in that period. Please choose a shorter date range so I can show up to 20 detailed records.";
            }
            pendingLeadLists.remove(sessionId);
            return detailedLeadList(filtered, pendingStatus + " leads from " + range.label);
        }
        String requestedStatus = requestedListStatus(normalized);
        if (requestedStatus != null) {
            List<Map<String, Object>> matching = leads.stream()
                    .filter(lead -> requestedStatus.equalsIgnoreCase(String.valueOf(lead.get("status"))))
                    .toList();
            if (matching.size() > 20) {
                pendingLeadLists.put(sessionId, requestedStatus);
                return "Sure. I found " + matching.size() + " " + requestedStatus + " leads. To list their details, please give a period in this format: \"this month to this month\", \"last month to this month\", or \"2026-09-01 to 2026-09-09\".";
            }
            return detailedLeadList(matching, requestedStatus + " leads");
        }
        List<String> pendingTerms = pendingMemorySearches.get(sessionId);
        if (pendingTerms != null) {
            LeadMemoryResult result = searchLeadMemoryByTerms(pendingTerms, leads);
            int requestedCount = requestedMemoryRecordCount(normalized, result.total);
            if (requestedCount < 1) {
                return "I found " + result.total + " matching leads. Reply \"yes\" to show all of them, or enter a number such as \"1\" or \"2\".";
            }
            pendingMemorySearches.remove(sessionId);
            List<Map<String, Object>> selected = result.records.stream().limit(requestedCount).toList();
            selectedMemoryResults.put(sessionId, new LeadMemoryResult(result.total, selected, result.terms, true));
            return "Here are " + selected.size() + " matching lead detail" + (selected.size() == 1 ? "." : "s.");
        }
        LeadMemoryResult memoryResult = searchLeadMemory(message, leads);
        if (memoryResult.isLookup) {
            String label = memoryLookupLabel(message);
            if (memoryResult.total == 0) return "No lead was found with that " + label + ".";
            if (memoryResult.total == 1) {
                selectedMemoryResults.put(sessionId, memoryResult);
                return "I found 1 matching lead. Here are the exact details.";
            }
            pendingMemorySearches.put(sessionId, memoryResult.terms);
            return "I found " + memoryResult.total + " leads matching that " + label + ". Should I show all "
                    + memoryResult.total + ", or reply with how many to show (for example, \"1\" or \"2\")?";
        }
        if (normalized.matches(".*\\b(hello|hi|hey|good morning|good afternoon|good evening)\\b.*")) {
            return "Hello. I can help you review and organize your leads.";
        }
        if (normalized.matches(".*\\b(who are you|what are you|your name|whats your name)\\b.*")) {
            return "I am your Lead Assistant. I can help you understand the current leads, statuses, sources, and pipeline.";
        }
        if (normalized.matches(".*\\b(how are you|how do you do)\\b.*")) {
            return "I am ready to help with your leads. What would you like to check?";
        }
        if (normalized.matches(".*\\b(thanks|thank you|thank)\\b.*")) {
            return "You are welcome. Let me know if you would like to check any lead details.";
        }
        if (normalized.matches(".*\\b(bye|goodbye|see you)\\b.*")) {
            return "Goodbye. I will be here when you need help with your leads.";
        }
        if (normalized.matches("^(yes|no|okay|ok)$")) {
            return "Got it. What would you like to know about your leads?";
        }
        if (normalized.matches(".*\\b(what is|what are|meaning of|mean)\\b.*\\bhot follow ups?\\b.*")) {
            return "Hot Follow Up is a priority status for leads that need prompt follow-up. There are "
                    + countStatus(leads, "Hot Follow Up") + " hot follow-up leads right now.";
        }
        if (normalized.matches(".*\\b(what is|what are|meaning of|mean)\\b.*\\bfollow ups?\\b.*")) {
            return "Following Up means the lead needs another contact or next step. There are "
                    + countStatus(leads, "Following Up") + " leads in that status right now.";
        }
        if (normalized.contains("converted") && (normalized.contains("follow") || normalized.contains("pending") || normalized.contains("left"))) {
            return "Converted: " + countStatus(leads, "Converted") + ". Following Up: " + countStatus(leads, "Following Up")
                    + ". Pending: " + countStatus(leads, "Pending") + ".";
        }
        if (normalized.contains("converted")) {
            return "There are " + countStatus(leads, "Converted") + " converted leads.";
        }
        if (normalized.contains("follow")) {
            return "There are " + countStatus(leads, "Following Up") + " leads currently marked for follow-up, plus "
                    + countStatus(leads, "Hot Follow Up") + " hot follow-ups.";
        }
        if (normalized.contains("pending") || normalized.contains("needs attention")) {
            return "There are " + countStatus(leads, "Pending") + " pending leads that need attention.";
        }
        if (normalized.contains("wrong lead") || normalized.contains("wrong ones") || normalized.contains("wrong")) {
            return "There are " + countStatus(leads, "Wrong Lead") + " leads marked as wrong leads.";
        }
        if (normalized.contains("call not picked") || normalized.contains("missed call")) {
            return "There are " + countStatus(leads, "Call Not Picked") + " leads where the call was not picked.";
        }
        if (normalized.contains("source") || normalized.contains("website") || normalized.contains("referral")) {
            return sourceSummary(leads);
        }
        if (normalized.contains("summary") || normalized.contains("overview") || normalized.contains("breakdown")) {
            return statusSummary(leads);
        }
        if (isCountQuestion(normalized)) {
            return "There are currently " + leadCount + " leads in the system.";
        }
        if (isCapabilityQuestion(normalized)) {
            return "I am your Lead Assistant. I can count and find leads, explain statuses and sources, summarize the pipeline, and show current lead information.";
        }
        if (normalized.contains("delete") || normalized.contains("create") || normalized.contains("update")) {
            return OUT_OF_SCOPE_NOTICE + "I can prepare that lead change, but an admin must confirm it before anything is created, updated, or deleted.";
        }
        if (normalized.contains("find") || normalized.contains("search") || normalized.contains("show me")) {
            return findLeads(message, leads);
        }
        // A lead-related question should still receive useful live CRM context even
        // when it does not match one of the more specific phrases above.
        if (isLeadQuestion(normalized)) {
            return "There are currently " + leadCount + " leads in the system. " + statusSummary(leads);
        }
        return OUT_OF_SCOPE_NOTICE + "I am currently trained for lead counts, statuses, sources, summaries, and basic lead searches.";
    }

    private String requestedListStatus(String message) {
        boolean requestsNames = message.matches(".*\\b(name|names|list|show|display)\\b.*");
        if (!requestsNames) return null;
        if (message.contains("wrong")) return "Wrong Lead";
        if (message.contains("hot follow")) return "Hot Follow Up";
        if (message.contains("follow")) return "Following Up";
        if (message.contains("pending")) return "Pending";
        if (message.contains("converted")) return "Converted";
        if (message.contains("call not picked") || message.contains("missed call")) return "Call Not Picked";
        return null;
    }

    /** Searches the live CRM snapshot for an explicitly requested name, company, phone,
     * email, product, remark, requirement, address, or other stored lead detail. */
    private LeadMemoryResult searchLeadMemory(String message, List<Map<String, Object>> leads) {
        if (message == null || message.isBlank() || !isMemoryLookupRequest(message.toLowerCase())) {
            return LeadMemoryResult.empty();
        }
        String lower = message.toLowerCase();

        // An email address in the message is unambiguous. Match on that value
        // alone so surrounding filler words ("give me details about this email -")
        // can never break the lookup.
        java.util.regex.Matcher emailMatcher = java.util.regex.Pattern
                .compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}").matcher(lower);
        if (emailMatcher.find()) {
            return searchLeadMemoryByTerms(List.of(emailMatcher.group()), leads);
        }

        // A phone number is likewise unambiguous. Compare digits only, since a
        // stored number may or may not include the country code, spaces, or dashes,
        // and the message may include extra punctuation like "no." after "phone".
        java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern
                .compile("(\\+?\\d[\\d\\s-]{7,}\\d)").matcher(message);
        if (phoneMatcher.find()) {
            String queryDigits = phoneMatcher.group().replaceAll("[^0-9]", "");
            String last10 = queryDigits.length() > 10 ? queryDigits.substring(queryDigits.length() - 10) : queryDigits;
            List<Map<String, Object>> matches = leads.stream().filter(lead -> {
                String phoneDigits = (String.valueOf(lead.get("phone")) + " " + String.valueOf(lead.get("companyMobile")))
                        .replaceAll("[^0-9]", "");
                return !queryDigits.isBlank() && (phoneDigits.contains(queryDigits)
                        || (!last10.isBlank() && phoneDigits.contains(last10)));
            }).toList();
            return new LeadMemoryResult(matches.size(), matches.stream().limit(20).toList(), List.of(queryDigits), true);
        }

        Set<String> ignored = Set.of("check", "find", "search", "show", "display", "give", "tell", "have", "any", "lead", "leads",
                "with", "name", "named", "called", "details", "detail", "about", "the", "a", "an", "is", "there", "for", "of", "please",
                "company", "email", "phone", "number", "wrong", "pending", "converted", "follow", "up", "hot", "status", "if", "we", "do", "does", "exist", "exists",
                "now", "again", "then", "just", "ok", "okay", "me", "my", "can", "you", "to", "and", "get", "provide", "want",
                "this", "that", "these", "those", "no", "regarding", "related", "info", "information", "record", "records",
                "kindly", "could", "would", "one", "some", "his", "her", "its", "our", "their");
        List<String> terms = new ArrayList<>();
        for (String token : message.toLowerCase().replaceAll("[^a-z0-9@.+-]+", " ").trim().split("\\s+")) {
            if (token.length() > 1 && !ignored.contains(token)) terms.add(token);
        }
        if (terms.isEmpty()) return new LeadMemoryResult(0, List.of(), terms, true);
        return searchLeadMemoryByTerms(terms, leads);
    }

    private LeadMemoryResult searchLeadMemoryByTerms(List<String> terms, List<Map<String, Object>> leads) {
        List<Map<String, Object>> matches = leads.stream().filter(lead -> {
            String searchable = lead.values().stream().filter(value -> value != null)
                    .map(String::valueOf).reduce("", (left, right) -> left + " " + right).toLowerCase();
            return terms.stream().allMatch(searchable::contains);
        }).toList();
        return new LeadMemoryResult(matches.size(), matches.stream().limit(20).toList(), terms, true);
    }

    private int requestedMemoryRecordCount(String message, int total) {
        if (message.matches(".*\\b(yes|all|show all|all of them)\\b.*")) return total;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b([1-9][0-9]*)\\b").matcher(message);
        if (!matcher.find()) return 0;
        try {
            return Math.min(Integer.parseInt(matcher.group(1)), total);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String memoryLookupLabel(String message) {
        String normalized = message.toLowerCase();
        if (normalized.contains("@") || normalized.contains("email")) return "email address";
        if (normalized.contains("company")) return "company name";
        if (normalized.contains("phone") || normalized.contains("number")) return "phone number";
        if (normalized.contains("name") || normalized.contains("named") || normalized.contains("called")) return "name";
        return "details";
    }

    private boolean isMemoryLookupRequest(String message) {
        return message.matches(".*\\b(find|search|check|show|display|details?|named|called|company|email|phone)\\b.*")
                || message.matches(".*\\bdo we have\\b.*") || message.matches(".*\\bany leads?\\b.*");
    }

    private record LeadMemoryResult(int total, List<Map<String, Object>> records, List<String> terms, boolean isLookup) {
        private static LeadMemoryResult empty() {
            return new LeadMemoryResult(0, List.of(), List.of(), false);
        }
    }

    private String detailedLeadList(List<Map<String, Object>> leads, String title) {
        if (leads.isEmpty()) return "There are no " + title + " to show.";
        StringBuilder reply = new StringBuilder(title.substring(0, 1).toUpperCase() + title.substring(1) + " (" + leads.size() + "): ");
        for (int index = 0; index < leads.size(); index++) {
            if (index > 0) reply.append(" | ");
            Map<String, Object> lead = leads.get(index);
            reply.append("#").append(lead.get("id")).append(" ")
                    .append(valueOrFallback(lead.get("company"), lead.get("name")))
                    .append(" — ").append(valueOrFallback(lead.get("name"), "No contact name"))
                    .append(", ").append(valueOrFallback(lead.get("phone"), "No phone"))
                    .append(", ").append(valueOrFallback(lead.get("email"), "No email"))
                    .append(", ").append(lead.get("leadDate"));
        }
        return reply.toString();
    }

    private boolean isWithinRange(Object leadDate, DateRange range) {
        if (!(leadDate instanceof LocalDate date)) return false;
        return !date.isBefore(range.start) && !date.isAfter(range.end);
    }

    private DateRange parseDateRange(String message) {
        String input = message == null ? "" : message.toLowerCase().trim();
        if (input.matches("this month\\s+to\\s+this month")) {
            YearMonth month = YearMonth.now();
            return new DateRange(month.atDay(1), month.atEndOfMonth(), "this month");
        }
        if (input.matches("last month\\s+to\\s+this month")) {
            YearMonth current = YearMonth.now();
            return new DateRange(current.minusMonths(1).atDay(1), current.atEndOfMonth(), "last month to this month");
        }
        String[] parts = input.split("\\s+to\\s+", 2);
        if (parts.length == 2) {
            try {
                LocalDate start = LocalDate.parse(parts[0].trim());
                LocalDate end = LocalDate.parse(parts[1].trim());
                return end.isBefore(start) ? null : new DateRange(start, end, start + " to " + end);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private record DateRange(LocalDate start, LocalDate end, String label) { }

    /**
     * Normalizes informal typing only for intent recognition.  The original message
     * remains available to lead searching, so a customer's name is never silently
     * spell-corrected before it is looked up.
     */
    private String normalizeIntentText(String message) {
        if (message == null || message.isBlank()) return "";
        String cleaned = message.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        StringBuilder normalized = new StringBuilder();
        for (String rawWord : cleaned.split("\\s+")) {
            if (rawWord.isBlank()) continue;
            if (normalized.length() > 0) normalized.append(' ');
            normalized.append(correctCommonWord(collapseRepeatedLetters(rawWord)));
        }
        return normalized.toString();
    }

    private String collapseRepeatedLetters(String word) {
        return word.replaceAll("(.)\\1{2,}", "$1");
    }

    private String correctCommonWord(String word) {
        // Common stretched greetings need one intentional correction beyond the
        // conservative edit-distance rule below (for example, "hloooo").
        if ("hlo".equals(word) || "helo".equals(word)) return "hello";
        if ("worng".equals(word)) return "wrong";
        if ("folow".equals(word)) return "follow";
        for (String commonWord : COMMON_WORDS) {
            if (commonWord.equals(word)) return word;
        }
        String closest = word;
        int smallestDistance = Integer.MAX_VALUE;
        for (String commonWord : COMMON_WORDS) {
            int distance = editDistance(word, commonWord);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closest = commonWord;
            }
        }
        // One edit handles ordinary typos ("yos" -> "yes") without changing
        // meaningful short CRM words such as "hot" into an unrelated greeting.
        int allowedDistance = 1;
        return smallestDistance <= allowedDistance ? closest : word;
    }

    private int editDistance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        for (int column = 0; column <= second.length(); column++) previous[column] = column;
        for (int row = 1; row <= first.length(); row++) {
            int[] current = new int[second.length() + 1];
            current[0] = row;
            for (int column = 1; column <= second.length(); column++) {
                int cost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(Math.min(current[column - 1] + 1, previous[column] + 1), previous[column - 1] + cost);
            }
            previous = current;
        }
        return previous[second.length()];
    }

    private boolean isCountQuestion(String message) {
        return message.contains("how many") || message.contains("total") || message.contains("count")
                || message.matches(".*\\b(no|number|number of|no\\.)\\s*(of\\s*)?(lead|leads)\\b.*")
                || message.matches(".*\\b(leads?)\\s*(kitne|count)\\b.*");
    }

    private boolean isLeadQuestion(String message) {
        return message.matches(".*\\b(lead|leads|pipeline|crm|customer|client|prospect|status|source|company|companies|email|phone|product|requirement|remark|amount|follow[- ]?up|converted|pending)\\b.*");
    }

    private boolean isCapabilityQuestion(String message) {
        return message.contains("help") || message.contains("what can")
                || message.matches(".*\\bwhat\\s+(do\\s+)?you\\s+do\\b.*")
                || message.matches(".*\\bwhat\\s+you\\s+do\\b.*")
                || message.matches(".*\\bhow\\s+can\\s+you\\b.*")
                || message.matches(".*\\b(your|you)\\s+(work|job|role)\\b.*");
    }

    private long countStatus(List<Map<String, Object>> leads, String status) {
        return leads.stream().filter(lead -> status.equalsIgnoreCase(String.valueOf(lead.get("status")))).count();
    }

    private String statusSummary(List<Map<String, Object>> leads) {
        StringBuilder summary = new StringBuilder("Lead summary: ");
        boolean added = false;
        for (LeadStatus status : LeadStatus.values()) {
            long count = countStatus(leads, status.getLabel());
            if (count > 0) {
                if (added) summary.append(" | ");
                summary.append(status.getLabel()).append(": ").append(count);
                added = true;
            }
        }
        return added ? summary.toString() : "There are no leads to summarize yet.";
    }

    private String sourceSummary(List<Map<String, Object>> leads) {
        StringBuilder summary = new StringBuilder("Lead sources: ");
        boolean added = false;
        for (LeadSource source : LeadSource.values()) {
            long count = leads.stream().filter(lead -> source.getLabel().equalsIgnoreCase(String.valueOf(lead.get("source")))).count();
            if (count > 0) {
                if (added) summary.append(" | ");
                summary.append(source.getLabel()).append(": ").append(count);
                added = true;
            }
        }
        return added ? summary.toString() : "No lead source data is available.";
    }

    private String findLeads(String message, List<Map<String, Object>> leads) {
        String query = message.toLowerCase()
                .replace("find", "").replace("search", "").replace("show me", "")
                .replace("lead", "").replace("leads", "").replace("all", "").trim();
        if (query.isBlank()) {
            if (leads.isEmpty()) return "There are no leads to show yet.";
            StringBuilder reply = new StringBuilder("Recent leads: ");
            for (int index = 0; index < Math.min(5, leads.size()); index++) {
                if (index > 0) reply.append(" | ");
                Map<String, Object> lead = leads.get(index);
                reply.append("#").append(lead.get("id")).append(" ")
                        .append(valueOrFallback(lead.get("company"), lead.get("name")))
                        .append(" ( ").append(lead.get("status")).append(" )");
            }
            return reply.toString();
        }
        List<Map<String, Object>> matches = leads.stream().filter(lead -> {
            String searchable = (String.valueOf(lead.get("name")) + " " + String.valueOf(lead.get("company")) + " "
                    + String.valueOf(lead.get("email")) + " " + String.valueOf(lead.get("phone"))).toLowerCase();
            return query.isBlank() || searchable.contains(query);
        }).limit(5).toList();
        if (matches.isEmpty()) return "I could not find a matching lead.";
        StringBuilder reply = new StringBuilder("Matching leads: ");
        for (int index = 0; index < matches.size(); index++) {
            if (index > 0) reply.append(" | ");
            Map<String, Object> lead = matches.get(index);
            reply.append("#").append(lead.get("id")).append(" ").append(valueOrFallback(lead.get("company"), lead.get("name")))
                    .append(" ( ").append(lead.get("status")).append(" )");
        }
        return reply.toString();
    }

    private String valueOrFallback(Object primary, Object fallback) {
        String value = String.valueOf(primary);
        return value == null || value.isBlank() || "null".equals(value) ? String.valueOf(fallback) : value;
    }

    private String callGroq(String message, List<Map<String, Object>> leadSnapshot) throws Exception {
        String crmContext = objectMapper.writeValueAsString(compactGroqContext(message, leadSnapshot));
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "You are Lead Assistant inside a CRM. Answer concisely and clearly. "
                + "Use only the supplied lead snapshot for CRM facts. Never claim that a lead was created, updated, or deleted. "
                + "For any mutation request, explain that the user must confirm the exact action first. "
                + "Do not expose internal database fields or API secrets. CRM context: " + crmContext);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", message == null ? "" : message.trim());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", groqModel == null || groqModel.isBlank() ? DEFAULT_MODEL : groqModel);
        payload.put("temperature", 0.2);
        payload.put("max_completion_tokens", 500);
        payload.put("messages", List.of(system, user));

        HttpRequest request = HttpRequest.newBuilder(URI.create(GROQ_ENDPOINT))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (result.statusCode() < 200 || result.statusCode() >= 300) {
            throw new IllegalStateException("Groq returned HTTP " + result.statusCode());
        }

        JsonNode root = objectMapper.readTree(result.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Groq returned an empty response");
        }
        return content.trim();
    }

    /** Keeps the provider request small even when the CRM contains many leads. */
    private Map<String, Object> compactGroqContext(String question, List<Map<String, Object>> leadSnapshot) {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> sourceCounts = new LinkedHashMap<>();
        List<Map<String, Object>> sampleLeads = new ArrayList<>();
        List<Map<String, Object>> matchingLeads = findGroqRelevantLeads(question, leadSnapshot);

        for (Map<String, Object> lead : leadSnapshot) {
            String status = String.valueOf(lead.get("status"));
            String source = String.valueOf(lead.get("source"));
            statusCounts.merge(status, 1, Integer::sum);
            sourceCounts.merge(source, 1, Integer::sum);

            if (sampleLeads.size() < GROQ_CONTEXT_LEAD_LIMIT) {
                Map<String, Object> compactLead = new LinkedHashMap<>();
                compactLead.put("name", lead.get("name"));
                compactLead.put("company", lead.get("company"));
                compactLead.put("status", lead.get("status"));
                compactLead.put("source", lead.get("source"));
                compactLead.put("product", lead.get("product"));
                compactLead.put("leadDate", lead.get("leadDate"));
                sampleLeads.add(compactLead);
            }
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("totalLeadCount", leadSnapshot.size());
        context.put("statusCounts", statusCounts);
        context.put("sourceCounts", sourceCounts);

        if (isLatestLeadQuestion(question)) {
            Map<String, Object> latestLead = findLatestLead(leadSnapshot);
            if (latestLead != null) {
                context.put("latestLead", detailedGroqLead(latestLead));
                context.put("latestLeadNote", "This is the most recently created lead, determined by creation timestamp.");
            }
        }

        if (!matchingLeads.isEmpty()) {
            context.put("matchedLeads", matchingLeads);
            context.put("matchNote", "These records were selected because they match the user's question.");
        } else {
            context.put("sampleLeads", sampleLeads);
            context.put("sampleNote", "No particular lead was identified. Only the first " + GROQ_CONTEXT_LEAD_LIMIT
                    + " compact lead records are included; use totals for overall counts.");
        }
        return context;
    }

    /** True when the question is asking for the most recently added lead. */
    private boolean isLatestLeadQuestion(String question) {
        if (question == null) return false;
        String normalized = question.toLowerCase();
        return normalized.matches(".*\\b(latest|newest|most recent|last added|recently added|just added|new lead)\\b.*");
    }

    /** Finds the lead with the most recent creation timestamp (falls back to leadDate if missing). */
    private Map<String, Object> findLatestLead(List<Map<String, Object>> leads) {
        Map<String, Object> latest = null;
        Comparable latestKey = null;
        for (Map<String, Object> lead : leads) {
            Object createdAt = lead.get("createdAt");
            Object key = createdAt != null ? createdAt : lead.get("leadDate");
            if (!(key instanceof Comparable comparableKey)) continue;
            if (latestKey == null || comparableKey.compareTo(latestKey) > 0) {
                latestKey = comparableKey;
                latest = lead;
            }
        }
        return latest;
    }

    private String describeLatestLead(Map<String, Object> lead) {
        Object created = lead.get("createdAt") != null ? lead.get("createdAt") : lead.get("leadDate");
        return "The latest lead is #" + lead.get("id") + " "
                + valueOrFallback(lead.get("company"), lead.get("name"))
                + " — " + valueOrFallback(lead.get("name"), "No contact name")
                + ", " + valueOrFallback(lead.get("phone"), "No phone")
                + ", " + valueOrFallback(lead.get("email"), "No email")
                + ", status: " + lead.get("status")
                + ", source: " + lead.get("source")
                + ", added on " + created + ".";
    }

    /** Retrieves the records relevant to a specific lead question before calling Groq. */
    private List<Map<String, Object>> findGroqRelevantLeads(String question, List<Map<String, Object>> leads) {
        if (question == null || question.isBlank()) return List.of();
        Set<String> ignored = Set.of("about", "check", "could", "detail", "details", "find", "give", "have",
                "information", "lead", "leads", "please", "provide", "search", "show", "tell", "that",
                "their", "there", "this", "what", "which", "with", "would", "your");
        List<String> terms = new ArrayList<>();
        for (String term : question.toLowerCase().split("[^a-z0-9@.+-]+")) {
            if (term.length() >= 3 && !ignored.contains(term)) terms.add(term);
        }
        if (terms.isEmpty()) return List.of();

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> lead : leads) {
            String searchable = String.join(" ",
                    String.valueOf(lead.get("name")), String.valueOf(lead.get("company")),
                    String.valueOf(lead.get("email")), String.valueOf(lead.get("phone")),
                    String.valueOf(lead.get("companyMobile")), String.valueOf(lead.get("officialEmail")),
                    String.valueOf(lead.get("product")), String.valueOf(lead.get("requirements")),
                    String.valueOf(lead.get("remarks")), String.valueOf(lead.get("address"))).toLowerCase();
            boolean matchesQuestion = terms.stream().anyMatch(searchable::contains);
            if (matchesQuestion && matches.size() < 20) matches.add(detailedGroqLead(lead));
        }
        return matches;
    }

    private Map<String, Object> detailedGroqLead(Map<String, Object> lead) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : List.of("name", "company", "email", "phone", "companyMobile", "officialEmail",
                "status", "source", "category", "product", "requirements", "remarks", "address", "leadDate", "createdAt", "amount")) {
            Object value = lead.get(field);
            result.put(field, value instanceof String text ? text.length() > 500 ? text.substring(0, 500) + "…" : text : value);
        }
        return result;
    }

    private List<Map<String, Object>> snapshotLeads() {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (Lead lead : leadRepository.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", lead.getId());
            item.put("name", lead.getIsName());
            item.put("company", lead.getCompanyName());
            item.put("email", lead.getEmail());
            item.put("phone", lead.getPhone());
            item.put("companyMobile", lead.getCompanyMobile());
            item.put("officialEmail", lead.getOfficialMailId());
            item.put("status", LeadStatus.labelOf(lead.getStatus()));
            item.put("source", LeadSource.labelOf(lead.getSourceId()));
            item.put("category", lead.getLeadCategory());
            item.put("product", lead.getProductName());
            item.put("requirements", lead.getRequirements());
            item.put("remarks", lead.getRemarks());
            item.put("address", lead.getAddress());
            item.put("leadDate", lead.getLeadDate());
            item.put("createdAt", lead.getCreatedAt());
            item.put("updatedAt", lead.getUpdatedAt());
            item.put("amount", lead.getAmount());
            snapshot.add(item);
        }
        return snapshot;
    }
}
