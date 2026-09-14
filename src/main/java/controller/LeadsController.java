package controller;

import model.Lead;
import repository.LeadRepository;
import util.LeadStatus;
import util.LeadSource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.BisIsiQuotation;
import repository.quotations.BisIsiQuotationRepository;
import model.BisFmcsQuotation;
import repository.quotations.BisFmcsQuotationRepository;
import model.CdscoQuotation;
import repository.quotations.CdscoQuotationRepository;
import model.BisCrsQuotation;
import repository.quotations.BisCrsQuotationRepository;
import model.CosmeticsQuotation;
import repository.quotations.CosmeticsQuotationRepository;
import service.quotations.pdf.IsiQuotationPdfService;

@Controller
public class LeadsController {

    private final LeadRepository leadRepository;
    private final BisIsiQuotationRepository bisIsiQuotationRepository;
    private final BisFmcsQuotationRepository bisFmcsQuotationRepository;
    private final CdscoQuotationRepository cdscoQuotationRepository;
    private final BisCrsQuotationRepository bisCrsQuotationRepository;
    private final CosmeticsQuotationRepository cosmeticsQuotationRepository;
    private final IsiQuotationPdfService isiQuotationPdfService;

    public LeadsController(LeadRepository leadRepository, BisIsiQuotationRepository bisIsiQuotationRepository,
            BisFmcsQuotationRepository bisFmcsQuotationRepository, CdscoQuotationRepository cdscoQuotationRepository,
            BisCrsQuotationRepository bisCrsQuotationRepository, CosmeticsQuotationRepository cosmeticsQuotationRepository,
            IsiQuotationPdfService isiQuotationPdfService) {
        this.leadRepository = leadRepository;
        this.bisIsiQuotationRepository = bisIsiQuotationRepository;
        this.bisFmcsQuotationRepository = bisFmcsQuotationRepository;
        this.cdscoQuotationRepository = cdscoQuotationRepository;
        this.bisCrsQuotationRepository = bisCrsQuotationRepository;
        this.cosmeticsQuotationRepository = cosmeticsQuotationRepository;
        this.isiQuotationPdfService = isiQuotationPdfService;
    }

    @GetMapping("/leads")
    public String showLeads(
            Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sourceId) {

        List<Lead> leads = leadRepository.findByFilters(status, sourceId);

        // Status counts for pie chart
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (LeadStatus s : LeadStatus.values()) statusCounts.put(s.getLabel(), 0L);
        for (Object[] row : leadRepository.countGroupedByStatus()) {
            String label = LeadStatus.labelOf((String) row[0]);
            if (statusCounts.containsKey(label)) {
                statusCounts.put(label, (Long) row[1]);
            }
        }

        // Source counts for pie chart
        Map<String, Long> sourceCounts = new LinkedHashMap<>();
        for (LeadSource s : LeadSource.values()) sourceCounts.put(s.getLabel(), 0L);
        for (Object[] row : leadRepository.countGroupedBySource()) {
            String label = LeadSource.labelOf((Long) row[0]);
            if (sourceCounts.containsKey(label)) {
                sourceCounts.put(label, (Long) row[1]);
            }
        }

        model.addAttribute("activePage", "leads");
        model.addAttribute("totalLeads", leadRepository.count());
        model.addAttribute("leads", leads);
        model.addAttribute("allStatuses", LeadStatus.values());
        model.addAttribute("allSources", LeadSource.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSourceId", sourceId);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("sourceCounts", sourceCounts);
        return "leads/leads";
    }

    @PostMapping("/leads/add")
    public String addLead(@ModelAttribute Lead lead) {
        leadRepository.save(lead);
        return "redirect:/leads";
    }

    @PostMapping("/leads/update/{id}")
    public String updateLead(@org.springframework.web.bind.annotation.PathVariable Long id, @ModelAttribute Lead lead) {
        lead.setId(id);
        leadRepository.save(lead);
        return "redirect:/leads";
    }

    @PostMapping("/leads/delete/{id}")
    public String deleteLead(@org.springframework.web.bind.annotation.PathVariable Long id) {
        leadRepository.deleteById(id);
        return "redirect:/leads";
    }

    @PostMapping("/leads/{id}/delete-ajax")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> deleteLeadAjax(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            leadRepository.deleteById(id);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/leads/{id}/json")
    @org.springframework.web.bind.annotation.ResponseBody
    public Lead getLeadJson(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return leadRepository.findById(id).orElse(null);
    }

    @GetMapping("/leads/{id}/isi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getIsiQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisIsiQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisIsiQuotationRepository.findById(revisionId).filter(q -> id.equals(q.getLeadId())).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/isi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveIsiQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisIsiQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.setLeadId(id);
        if (quotation.getId() != null && bisIsiQuotationRepository.findById(quotation.getId()).filter(q -> id.equals(q.getLeadId())).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.getId() == null) quotation.setReferenceNo("REF-" + id + "-R" + (bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1));
        bisIsiQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{leadId}/isi-quotation/{quotationId}/pdf")
    public ResponseEntity<byte[]> downloadIsiQuotationPdf(
            @PathVariable Long leadId,
            @PathVariable Long quotationId) {
        Lead lead = leadRepository.findById(leadId).orElse(null);
        BisIsiQuotation quotation = bisIsiQuotationRepository.findById(quotationId)
                .filter(item -> leadId.equals(item.getLeadId()))
                .orElse(null);

        if (lead == null || quotation == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] document = isiQuotationPdfService.generate(quotation, lead);
            String filename = safeFilename(
                    "ISI-Quotation-" + (quotation.getReferenceNo() == null ? quotationId : quotation.getReferenceNo()) + ".pdf");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(document);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String safeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    @GetMapping("/leads/{id}/fmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getFmcsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisFmcsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisFmcsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/fmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveFmcsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisFmcsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisFmcsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisFmcsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cdsco-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCdscoQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cdscoQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cdscoQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cdsco-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCdscoQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CdscoQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && cdscoQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        cdscoQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/crs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCrsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisCrsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisCrsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/crs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCrsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisCrsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisCrsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisCrsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cosmetics-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCosmeticsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cosmeticsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cosmeticsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cosmetics-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCosmeticsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CosmeticsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && cosmeticsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        cosmeticsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @PostMapping("/leads/{id}/quick-update")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> quickUpdateLead(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam String field,
            @RequestParam String value) {

        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);

        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }

        try {
            if ("status".equals(field)) {
                lead.setStatus(value);
            } else if ("sourceId".equals(field)) {
                if (value == null || value.trim().isEmpty()) {
                    lead.setSourceId(null);
                } else {
                    lead.setSourceId(Long.parseLong(value));
                }
            } else if ("quotationType".equals(field)) {
                lead.setQuotationType(value);
            } else {
                response.put("success", false);
                response.put("message", "Invalid field");
                return response;
            }

            leadRepository.save(lead);
            response.put("success", true);
            response.put("field", field);
            response.put("value", value);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

}
