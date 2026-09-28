package controller;

import dto.QuotationPageResponse;
import dto.QuotationCreateRequest;
import dto.QuotationCreateResponse;
import dto.QuotationEditRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import service.QuotationService;
import service.QuotationCreationService;
import service.QuotationPdfService;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-equipment/quotations")
public class QuotationApiController {
    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final int MAX_SEARCH_LENGTH = 100;

    private final QuotationService quotationService;
    private final QuotationCreationService quotationCreationService;
    private final QuotationPdfService quotationPdfService;

    public QuotationApiController(QuotationService quotationService, QuotationCreationService quotationCreationService, QuotationPdfService quotationPdfService) {
        this.quotationService = quotationService;
        this.quotationCreationService = quotationCreationService;
        this.quotationPdfService = quotationPdfService;
    }

    @GetMapping
    public ResponseEntity<QuotationPageResponse> getQuotations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "month", required = false) String monthValue) {
        if (page < 1 || page > MAX_PAGE_NUMBER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be between 1 and 10000");
        }
        if (query != null && query.length() > MAX_SEARCH_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must not exceed 100 characters");
        }
        YearMonth month = parseMonth(monthValue);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(quotationService.findQuotations(page, query, month));
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must use YYYY-MM format");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<dto.QuotationResponse> quotation(@PathVariable long id) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(quotationService.findQuotation(id)); }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<dto.QuotationResponse> updateQuotation(@PathVariable long id, @RequestBody QuotationEditRequest request, Authentication authentication) {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(quotationService.updateQuotation(id, request, authentication.getName())); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuotation(@PathVariable long id) { quotationService.deleteQuotation(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, String>>> history(@PathVariable long id) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(quotationService.history(id).stream().map(entry -> Map.of("editor", entry.getEditorName(), "editedAt", entry.getEditedAt().toString())).toList()); }

    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable long id) {
        var pdf = quotationPdfService.download(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.filename() + "\"").contentType(MediaType.APPLICATION_PDF).contentLength(pdf.content().length).body(pdf.content());
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuotationPdfService.PreviewData> preview(@PathVariable long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(quotationPdfService.preview(id));
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<QuotationCreateResponse> createQuotation(@RequestBody QuotationCreateRequest request,
                                                                     Authentication authentication) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noStore())
                    .body(quotationCreationService.create(request, authentication.getName()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}