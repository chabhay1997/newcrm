package controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import service.LeadProviderIntegrationService;

@RestController
public class LeadProviderIntegrationController {

    private final LeadProviderIntegrationService integrationService;
    private final ObjectMapper objectMapper;

    public LeadProviderIntegrationController(
            LeadProviderIntegrationService integrationService,
            ObjectMapper objectMapper) {
        this.integrationService = integrationService;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(
            value = "/api/justdial/lead",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> receiveJustdialLead(
            HttpServletRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) payload.put(key, values[0]);
        });

        try {
            if (request.getContentType() != null
                    && request.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)
                    && request.getContentLength() != 0) {
                Map<String, Object> body = objectMapper.readValue(
                        request.getInputStream(), new TypeReference<>() {});
                if (body != null) payload.putAll(body);
            }
            integrationService.storeJustdialLead(payload);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("RECEIVED");
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "ERROR", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "ERROR", "message", "Unable to store Justdial lead"));
        }
    }
}