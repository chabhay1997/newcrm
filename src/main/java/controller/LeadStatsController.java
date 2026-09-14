package controller;

import repository.LeadRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LeadStatsController {

    private final LeadRepository leadRepository;

    public LeadStatsController(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @GetMapping("/api/leads/stats")
    public Map<String, Object> getLeadStats() {
        long totalLeads = leadRepository.count();

        Map<String, Object> response = new HashMap<>();
        response.put("totalLeads", totalLeads);
        return response;
    }
}