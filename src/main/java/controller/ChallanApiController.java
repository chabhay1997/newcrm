package controller;

import dto.ChallanPageResponse;
import dto.ChallanCreateRequest;
import dto.ChallanCreateResponse;
import dto.ChallanResponse;
import dto.ChallanDetailsResponse;
import dto.ChallanReturnAlertResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import service.ChallanService;
import service.ChallanPdfService;
import service.ChallanExcelExportService;
import service.ChallanExcelImportService;
import service.ChallanAnalyticsService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challans")
public class ChallanApiController {
    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final int MAX_SEARCH_LENGTH = 100;
    private final ChallanService challanService;
    private final ChallanPdfService challanPdfService;
    private final ChallanExcelExportService challanExcelExportService;
    private final ChallanExcelImportService challanExcelImportService;
    private final ChallanAnalyticsService challanAnalyticsService;

    public ChallanApiController(ChallanService challanService, ChallanPdfService challanPdfService,
                                ChallanExcelExportService challanExcelExportService,
                                ChallanExcelImportService challanExcelImportService,
                                ChallanAnalyticsService challanAnalyticsService) {
        this.challanService = challanService;
        this.challanPdfService = challanPdfService;
        this.challanExcelExportService = challanExcelExportService;
        this.challanExcelImportService = challanExcelImportService;
        this.challanAnalyticsService = challanAnalyticsService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<dto.ChallanAnalyticsResponse> challanAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateFilters(1, "", startDate, endDate);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(challanAnalyticsService.analytics(startDate, endDate));
    }

    @GetMapping("/return-alerts")
    public ResponseEntity<List<ChallanReturnAlertResponse>> sampleReturnAlerts() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(challanService.upcomingSampleReturns());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> importChallans(@RequestParam("file") MultipartFile file,
                                                               Authentication authentication) {
        try {
            var result = challanExcelImportService.importChallans(file, authentication.getName());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                    "imported", result.imported(),
                    "skipped", result.skipped(),
                    "message", result.imported() + " challan(s) imported; " + result.skipped() + " row(s) skipped."
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read the Excel file.");
        }
    }

    @GetMapping
    public ResponseEntity<ChallanPageResponse> getChallans(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(name = "q", defaultValue = "") String query,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateFilters(page, query, startDate, endDate);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(challanService.findChallans(page, query, startDate, endDate));
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportAllChallans(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateFilters(1, query, startDate, endDate);
        byte[] workbook = challanExcelExportService.exportFiltered(query, startDate, endDate);
        String filename = "challans-" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")) + ".xlsx";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(workbook.length)
                .body(workbook);
    }

    private void validateFilters(int page, String query, LocalDate startDate, LocalDate endDate) {
        if (page < 1 || page > MAX_PAGE_NUMBER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be between 1 and 10000");
        }
        if (query != null && query.length() > MAX_SEARCH_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must not exceed 100 characters");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Starting Date must not be after Ending Date");
        }
    }

    @PostMapping(consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ChallanCreateResponse> createChallan(
            @ModelAttribute ChallanCreateRequest request,
            @RequestParam(name = "uploads", required = false) List<MultipartFile> uploads,
            Authentication authentication) {
        try {
            var saved = challanService.createChallan(request, uploads, authentication.getName());
            var response = new ChallanResponse(saved.getId(), saved.getChallanNo(), saved.getClientName(),
                    saved.getItemName(), saved.getBrandName(), saved.getQty(), saved.getAmount(),
                    saved.getDate(), authentication.getName(), saved.getCreatedAt());
            int dashboardPage = (int) Math.ceil(challanService.countChallans() / (double) ChallanService.PAGE_SIZE);
            return ResponseEntity.created(URI.create("/api/challans/" + saved.getId()))
                    .cacheControl(CacheControl.noStore())
                    .body(new ChallanCreateResponse(response, Math.max(dashboardPage, 1), "Challan created successfully"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create challan");
        }
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ChallanCreateResponse> updateChallan(
            @PathVariable long id, @ModelAttribute ChallanCreateRequest request,
            @RequestParam(name = "uploads", required = false) List<MultipartFile> uploads,
            Authentication authentication) {
        try {
            var saved = challanService.updateChallan(id, request, uploads);
            var response = new ChallanResponse(saved.getId(), saved.getChallanNo(), saved.getClientName(),
                    saved.getItemName(), saved.getBrandName(), saved.getQty(), saved.getAmount(),
                    saved.getDate(), authentication.getName(), saved.getCreatedAt());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new ChallanCreateResponse(response, challanService.pageContaining(saved.getId()),
                            "Challan updated successfully"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update challan");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallan(@PathVariable long id) {
        if (id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challan ID must be positive");
        }
        challanService.deleteChallan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, String>>> challanHistory(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(challanService.editHistory(id).stream().map(entry -> Map.of(
                        "editor", entry.getEditorName(),
                        "editedAt", entry.getEditedAt().toString()
                )).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallanDetailsResponse> challanDetails(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(challanService.findDetails(id));
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadChallan(@PathVariable long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challan ID must be positive");
        var pdf = challanPdfService.downloadData(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
