package controller;

import dto.QuotationPageResponse;
import dto.QuotationCreateRequest;
import dto.QuotationCreateResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import service.QuotationService;
import service.QuotationCreationService;

@RestController
@RequestMapping("/api/lab-equipment/quotations")
public class QuotationApiController {
    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final int MAX_SEARCH_LENGTH = 100;

    private final QuotationService quotationService;
    private final QuotationCreationService quotationCreationService;

    public QuotationApiController(QuotationService quotationService, QuotationCreationService quotationCreationService) {
        this.quotationService = quotationService;
        this.quotationCreationService = quotationCreationService;
    }

    @GetMapping
    public ResponseEntity<QuotationPageResponse> getQuotations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "q", defaultValue = "") String query) {
        if (page < 1 || page > MAX_PAGE_NUMBER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be between 1 and 10000");
        }
        if (query != null && query.length() > MAX_SEARCH_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must not exceed 100 characters");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(quotationService.findQuotations(page, query));
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
