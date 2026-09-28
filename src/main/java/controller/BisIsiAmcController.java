package controller;

import dto.BisIsiAmcQuotationForm;
import model.BisIsiOperation;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.BisIsiAmcExcelService;
import service.BisIsiAmcQuotationService;
import service.BisIsiAmcPdfService;
import service.BisIsiService;
import service.OperationAccessService;

@Controller
public class BisIsiAmcController {
    private final BisIsiService operations;
    private final OperationAccessService access;
    private final BisIsiAmcExcelService excel;
    private final BisIsiAmcQuotationService quotations;
    private final BisIsiAmcPdfService pdf;

    public BisIsiAmcController(BisIsiService operations, OperationAccessService access, BisIsiAmcExcelService excel,
                               BisIsiAmcQuotationService quotations, BisIsiAmcPdfService pdf) {
        this.operations = operations;
        this.access = access;
        this.excel = excel;
        this.quotations = quotations;
        this.pdf = pdf;
    }

    @GetMapping("/operation/bis-isi-amc")
    public String index(Authentication authentication, Model model,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "0") int page) {
        User user = access.require(authentication);
        int selectedSize = size == 10 || size == 25 || size == 50 || size == 100 ? size : 25;
        String query = search == null ? "" : search.trim();
        Page<BisIsiOperation> result = operations.listAmc(user, query, selectedSize, Math.max(0, page));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages())
            result = operations.listAmc(user, query, selectedSize, result.getTotalPages() - 1);
        model.addAttribute("activePage", "operation-bis-isi-amc");
        model.addAttribute("operations", result);
        model.addAttribute("amcQuotes", quotations.forOperations(result.getContent().stream().map(BisIsiOperation::getId).toList()));
        model.addAttribute("search", query);
        model.addAttribute("size", selectedSize);
        return "operation/bis-isi-amc/index";
    }

    public record SearchSuggestion(String value, String detail) {}

    @GetMapping("/operation/bis-isi-amc/suggestions")
    @ResponseBody
    public java.util.List<SearchSuggestion> suggestions(Authentication authentication, @RequestParam String query) {
        User user = access.require(authentication);
        String term = query.trim().toLowerCase(java.util.Locale.ROOT);
        if (term.isEmpty()) return java.util.List.of();
        java.util.Map<String, SearchSuggestion> matches = new java.util.LinkedHashMap<>();
        for (BisIsiOperation operation : operations.listAmc(user, term, 100, 0).getContent()) {
            String detail = operation.getCompanyName() + " · " + operation.getIndianStandard();
            for (String value : new String[]{operation.getCompanyName(), operation.getClientName(),
                    operation.getIndianStandard(), operation.getCmlNumber()}) {
                if (value != null && value.toLowerCase(java.util.Locale.ROOT).contains(term))
                    matches.putIfAbsent(value.toLowerCase(java.util.Locale.ROOT), new SearchSuggestion(value, detail));
                if (matches.size() >= 8) return java.util.List.copyOf(matches.values());
            }
        }
        return java.util.List.copyOf(matches.values());
    }

    @GetMapping("/operation/bis-isi-amc/{id}/quotation")
    @ResponseBody
    public java.util.Map<String, Object> quotation(Authentication authentication, @PathVariable long id,
                                                    @RequestParam(required = false) Integer revision) {
        return quotations.form(access.require(authentication), id, revision);
    }

    @PostMapping("/operation/bis-isi-amc/{id}/quotation")
    public String saveQuotation(Authentication authentication, @PathVariable long id,
                                @ModelAttribute BisIsiAmcQuotationForm form,
                                @RequestParam(defaultValue = "") String returnSearch,
                                @RequestParam(defaultValue = "25") int returnSize,
                                @RequestParam(defaultValue = "0") int returnPage,
                                RedirectAttributes flash) {
        try {
            var saved = quotations.save(access.require(authentication), id, form);
            flash.addFlashAttribute("success", "AMC/SIT quotation " + saved.getReferenceNumber() + " saved successfully.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        flash.addAttribute("search", returnSearch);
        flash.addAttribute("size", returnSize);
        flash.addAttribute("page", returnPage);
        return "redirect:/operation/bis-isi-amc";
    }

    @GetMapping("/operation/bis-isi-amc/{id}/quotation/download")
    public ResponseEntity<byte[]> downloadQuotation(Authentication authentication, @PathVariable long id,
                                                     @RequestParam(name = "quotation_id", required = false) Long quotationId) {
        var quotation = quotations.forDownload(access.require(authentication), id, quotationId);
        var document = pdf.render(quotation);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(document.content().length).body(document.content());
    }

    @GetMapping("/operation/bis-isi-amc/export")
    public ResponseEntity<byte[]> export(Authentication authentication, @RequestParam(required = false) String search) {
        User user = access.require(authentication);
        byte[] bytes = excel.export(user, search == null ? "" : search.trim());
        String filename = "bis-isi-amc-" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length).body(bytes);
    }
}
