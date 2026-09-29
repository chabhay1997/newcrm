package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.leads_assistant.LeadAssistantService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/leads/assistant")
public class LeadAssistantController {

    private static final Logger log = LoggerFactory.getLogger(LeadAssistantController.class);

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

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> transcribe(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No audio was recorded."));
        }
        if (file.getSize() > 25L * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("success", false, "message", "The recording is too large. Please record a shorter question."));
        }
        try {
            String transcript = leadAssistantService.transcribeVoice(
                    file.getBytes(), file.getOriginalFilename(), file.getContentType());
            return ResponseEntity.ok(Map.of("success", true, "text", transcript));
        } catch (Exception exception) {
            log.warn("Lead voice transcription failed: {}", exception.getMessage());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) detail = "Voice transcription failed. Please try again.";
            if (detail.length() > 240) detail = detail.substring(0, 240);
            response.put("message", detail);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
    }
}
