package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.leads_assistant.LeadAssistantService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/leads/assistant")
public class LeadAssistantController {

    private final LeadAssistantService leadAssistantService;

    public LeadAssistantController(LeadAssistantService leadAssistantService) {
        this.leadAssistantService = leadAssistantService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Lead Assistant is ready");
        return response;
    }

    @GetMapping("/history")
    public Map<String, Object> history(@org.springframework.web.bind.annotation.RequestParam String sessionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("messages", leadAssistantService.history(sessionId));
        return response;
    }

    @PostMapping("/new")
    public Map<String, Object> newChat(@RequestBody Map<String, Object> request) {
        String sessionId = request.get("sessionId") == null ? "" : String.valueOf(request.get("sessionId"));
        leadAssistantService.clearHistory(sessionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        return response;
    }

    @PostMapping("/delete")
    public Map<String, Object> deleteHistory(@RequestBody Map<String, Object> request) {
        String sessionId = request.get("sessionId") == null ? "" : String.valueOf(request.get("sessionId"));
        leadAssistantService.clearHistory(sessionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        return response;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        String message = request.get("message") == null ? "" : String.valueOf(request.get("message"));
        String sessionId = request.get("sessionId") == null ? "" : String.valueOf(request.get("sessionId"));
        boolean enhanced = Boolean.TRUE.equals(request.get("enhanced"));
        Map<String, Object> response = leadAssistantService.respond(sessionId, message, enhanced);
        response.put("success", true);
        return response;
    }
}
