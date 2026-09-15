package controller;

import dto.ApiResponse;
import dto.BankCreateRequest;
import dto.DataTableResponse;
import dto.InvoiceRowDTO;
import enums.InvoiceType;
import model.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.BankRepository;
import service.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import dto.InvoicePreviewUpdateRequest;

@Controller
public class InvoiceController {

    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceListingService invoiceListingService;
    @Autowired private InvoiceTermsService invoiceTermsService;
    @Autowired private BankService bankService;
    @Autowired private InvoicePdfService invoicePdfService;
    @Autowired private InvoiceExcelService invoiceExcelService;
    @Autowired private BankRepository bankRepository;

    // ---- Views ----

    @GetMapping({"/invoice", "/invoice/{type}"})
    public String index(@PathVariable(required = false) String type,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(name = "q", required = false) String searchQuery,
                        @RequestParam(name = "status", required = false) Integer statusFilter,
                        @RequestParam(name = "period", required = false) String period,
                        @RequestParam(name = "months", required = false) Integer monthRange,
                        @RequestParam(name = "year", required = false) Integer year,
                        Model model) {
        InvoiceType invoiceType = InvoiceType.fromSlug(type);
        Integer selectedStatus = statusFilter != null && statusFilter >= 1 && statusFilter <= 3
                ? statusFilter : null;
        if (period != null && !period.isBlank()) {
            if (period.startsWith("year:")) {
                try { year = Integer.valueOf(period.substring(5)); } catch (NumberFormatException ignored) { year = null; }
                monthRange = null;
            } else {
                try { monthRange = Integer.valueOf(period); } catch (NumberFormatException ignored) { monthRange = null; }
                year = null;
            }
        }
        Integer selectedMonthRange = monthRange != null && List.of(1, 3, 6, 12).contains(monthRange) ? monthRange : null;
        int currentYear = LocalDate.now().getYear();
        Integer selectedYear = year != null && year >= 2000 && year <= currentYear ? year : null;
        if (selectedYear != null) selectedMonthRange = null;
        final int pageSize = 25;
        int currentPage = Math.max(page, 1);
        DataTableResponse<InvoiceRowDTO> listing = invoiceListingService.listForDataTable(
                invoiceType.getSlug(), 1, (currentPage - 1) * pageSize, pageSize, selectedMonthRange, selectedYear, selectedStatus, searchQuery, "");
        int totalPages = (int) Math.ceil(listing.getRecordsFiltered() / (double) pageSize);
        if (totalPages > 0 && currentPage > totalPages) {
            currentPage = totalPages;
            listing = invoiceListingService.listForDataTable(
                    invoiceType.getSlug(), 1, (currentPage - 1) * pageSize, pageSize, selectedMonthRange, selectedYear, selectedStatus, searchQuery, "");
        }
        var overallTotals = invoiceService.computeTotals(invoiceType.getId(), null, selectedMonthRange, selectedYear);
        model.addAttribute("activePage", "accounts");
        model.addAttribute("type", invoiceType.getSlug());
        model.addAttribute("invoiceList", listing.getData());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageStart", (currentPage - 1) * pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("searchQuery", searchQuery == null ? "" : searchQuery.trim());
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedMonthRange", selectedMonthRange);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedPeriod", selectedYear != null ? "year:" + selectedYear : selectedMonthRange == null ? "" : selectedMonthRange.toString());
        model.addAttribute("availableYears", java.util.stream.IntStream.rangeClosed(2000, currentYear).boxed().sorted(java.util.Comparator.reverseOrder()).toList());
        model.addAttribute("displayRecordCount", listing.getRecordsFiltered());
        model.addAttribute("listingPath", type == null ? "/invoice" : "/invoice/" + invoiceType.getSlug());
        model.addAttribute("totalInvoiceCount", overallTotals.getCounts().getTotal());
        model.addAttribute("totalSalesAmount", overallTotals.getTotal());
        model.addAttribute("paidInvoiceCount", overallTotals.getCounts().getPaid());
        model.addAttribute("paidAmount", overallTotals.getPaid());
        model.addAttribute("halfInvoiceCount", overallTotals.getCounts().getHalf());
        model.addAttribute("halfAmount", overallTotals.getHalf());
        model.addAttribute("unpaidInvoiceCount", overallTotals.getCounts().getUnpaid());
        model.addAttribute("unpaidAmount", overallTotals.getUnpaid());
        return "invoice/index";
    }

    @GetMapping("/invoice/create")
    public String create(Model model) {
        Invoice invoice = new Invoice();
        invoice.setDate(LocalDate.now());
        model.addAttribute("invoice", invoice);
        model.addAttribute("lineItems", invoiceService.getLineItems(invoice));
        model.addAttribute("banks", bankRepository.findAll());
        model.addAttribute("activePage", "accounts");
        return "invoice/create";
    }

    @GetMapping("/invoice/export/{type}")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String type) throws IOException {
        InvoiceType invoiceType = InvoiceType.fromSlug(type);
        byte[] workbook = invoiceExcelService.exportInvoices(invoiceType);
        String filename = invoiceType.getSlug() + "-invoices-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(workbook.length)
                .body(workbook);
    }

    @PostMapping("/invoice/import/{type}")
    public String importExcel(@PathVariable String type,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        InvoiceType invoiceType = InvoiceType.fromSlug(type);
        try {
            InvoiceExcelService.ImportResult result = invoiceExcelService.importInvoices(invoiceType, file);
            redirectAttributes.addFlashAttribute("successMessage", result.imported() + " invoice(s) imported; " + result.skipped() + " row(s) skipped.");
        } catch (IllegalArgumentException | IOException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/invoice/" + invoiceType.getSlug();
    }

    @PostMapping("/invoice/create")
    public String createStore(
            @ModelAttribute Invoice invoice,
            @RequestParam(name = "title", required = false) List<String> titleList,
            @RequestParam(name = "particular", required = false) List<String> particularList,
            @RequestParam(name = "amtD", required = false) List<String> amtList,
            RedirectAttributes redirectAttributes) {
        if (invoice.getInvType() == null) {
            redirectAttributes.addFlashAttribute("invoiceTypeError", "Please select an Invoice Type before saving.");
            return "redirect:/invoice/create";
        }
        boolean isUpdate = invoice.getId() != null;
        InvoiceType redirectType = invoiceService.performaStore(invoice, titleList, particularList, amtList);
        redirectAttributes.addFlashAttribute("successMessage",
                isUpdate ? "Invoice changes saved successfully." : "Invoice saved successfully.");
        return "redirect:/invoice/" + redirectType.getSlug();
    }

    @GetMapping("/invoice/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        model.addAttribute("invoice", invoice);
        model.addAttribute("lineItems", invoiceService.getLineItems(invoice));
        model.addAttribute("banks", bankRepository.findAll());
        model.addAttribute("activePage", "accounts");
        model.addAttribute("type", invoice.getInvType() == null ? "evtl" : InvoiceType.fromId(invoice.getInvType()).getSlug());
        return "invoice/edit";
    }

    // ---- Listing (AJAX datatable) ----

    @PostMapping("/invoice/datatable/{type}")
    @ResponseBody
    public ResponseEntity<?> datatable(
            @PathVariable(required = false) String type,
            @RequestParam(defaultValue = "0") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "date_filter", required = false) Integer dateFilter,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "status_filter", required = false) Integer statusFilter,
            @RequestParam(name = "search[value]", required = false) String nameKeyword,
            HttpServletRequest request) {
        try {
            Integer safeStatus = (statusFilter != null && statusFilter >= 1 && statusFilter <= 3) ? statusFilter : null;
            DataTableResponse<InvoiceRowDTO> response = invoiceListingService.listForDataTable(
                    type, draw, start, length, dateFilter, year, safeStatus, nameKeyword, request.getContextPath());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Save / Update (performaStore) ----

    @PostMapping("/invoice/store")
    @ResponseBody
    public ResponseEntity<?> performaStore(
            @ModelAttribute Invoice invoice,
            @RequestParam(name = "title", required = false) List<String> titleList,
            @RequestParam(name = "particular", required = false) List<String> particularList,
            @RequestParam(name = "amtD", required = false) List<String> amtList) {
        try {
            if (invoice.getInvType() == null) {
                return ResponseEntity.ok(Map.of(
                        "status", false,
                        "message", "Please select an Invoice Type before saving."
                ));
            }
            boolean isUpdate = invoice.getId() != null;
            InvoiceType redirectType = invoiceService.performaStore(invoice, titleList, particularList, amtList);
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", isUpdate
                            ? "Invoice changes saved successfully."
                            : "Invoice saved successfully.",
                    "redirect", "/invoice/" + redirectType.getSlug()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", false, "message", e.getMessage()));
        }
    }

    // ---- Status update ----

    @PostMapping("/invoice/status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(
            @RequestParam Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String type) {
        try {
            invoiceService.updateStatus(id, status);
            Integer invTypeId = (type != null) ? InvoiceType.fromSlug(type).getId() : null;
            var totals = invoiceService.computeTotals(invTypeId, null, null);
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", "Status updated successfully",
                    "totals", totals
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", false, "message", e.getMessage()));
        }
    }

    // ---- Delete ----

    @PostMapping("/invoice/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestParam Long id) {
        try {
            invoiceService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice USD List not found"));
            invoiceService.delete(id);
            return ResponseEntity.ok(Map.of("status", true, "message", "Invoice USD List Deleted Successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/invoice/history/{id}")
    @ResponseBody
    public ResponseEntity<?> history(@PathVariable Long id) {
        invoiceService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return ResponseEntity.ok(invoiceService.editHistory(id).stream().map(entry -> Map.of(
                "editor", entry.getEditorName(),
                "editedAt", entry.getEditedAt().toString()
        )).toList());
    }

    @GetMapping("/invoice/quick-preview/{id}")
    @ResponseBody
    public ResponseEntity<?> quickPreview(@PathVariable Long id) {
        Invoice invoice = invoiceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        String businessName = invoice.getInvType() != null && invoice.getInvType() == 2
                ? "PROLIX INDIA & REGULATORY COMPLIANCE CONSULTANT"
                : invoice.getInvType() != null && invoice.getInvType() == 3 ? "GOVERNMENT" : "EVTL INDIA PVT. LTD.";
        return ResponseEntity.ok(Map.of(
                "id", invoice.getId(),
                "businessName", businessName,
                "invNo", invoice.getInvNo() == null ? "" : invoice.getInvNo(),
                "date", invoice.getDate() == null ? "" : invoice.getDate().toString(),
                "poNo", invoice.getPoNo() == null ? "" : invoice.getPoNo(),
                "name", invoice.getName() == null ? "" : invoice.getName(),
                "address", invoice.getAddress() == null ? "" : invoice.getAddress(),
                "gstin", invoice.getGstin() == null ? "" : invoice.getGstin(),
                "remarks", invoice.getRemarks() == null ? "" : invoice.getRemarks(),
                "items", invoiceService.getLineItems(invoice)
        ));
    }

    @PostMapping("/invoice/quick-preview/{id}")
    @ResponseBody
    public ResponseEntity<?> updateQuickPreview(@PathVariable Long id,
                                                 @RequestBody InvoicePreviewUpdateRequest request) {
        try {
            Invoice saved = invoiceService.updateFromPreview(id, request);
            return ResponseEntity.ok(Map.of("status", true, "message", "Invoice updated successfully.", "amount", saved.getFinalAmt()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", false, "message", e.getMessage()));
        }
    }

    // ---- Next invoice/PO number ----

    @GetMapping("/invoice/get-next-invoice")
    @ResponseBody
    public ResponseEntity<?> getNextInvoiceNumber(@RequestParam("inv_type") Integer invType) {
        if (invType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invoice type missing"));
        }
        String[] numbers = invoiceService.getNextInvoiceAndPoNo(invType);
        return ResponseEntity.ok(Map.of("invoice_no", numbers[0], "po_no", numbers[1]));
    }

    // ---- Terms & Conditions ----

    @GetMapping("/invoice/terms")
    @ResponseBody
    public ResponseEntity<?> getTermsByInvType(@RequestParam Integer invType) {
        var terms = invoiceTermsService.getTermsByType(invType)
                .stream()
                .map(t -> Map.of("terms_condtions", t.getTermsCondtions()))
                .toList();
        return ResponseEntity.ok(terms);
    }

    @PostMapping("/invoice/terms")
    @ResponseBody
    public ResponseEntity<?> termsConditions(@RequestBody Map<String, Object> body) {
        Object invTypeValue = body.get("invType");
        if (!(invTypeValue instanceof Number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invoice type is required."));
        }
        Integer invType = ((Number) invTypeValue).intValue();
        @SuppressWarnings("unchecked")
        List<String> terms = (List<String>) body.get("terms");
        if (terms == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Terms list is required."));
        }
        invoiceTermsService.saveTerms(invType, terms);
        return ResponseEntity.ok(Map.of("status", true));
    }

    // ---- Bank ----

    @PostMapping("/invoice/bank")
    @ResponseBody
    public ResponseEntity<?> bank(@ModelAttribute BankCreateRequest request) {
        boolean created = bankService.create(request);
        if (!created) {
            return ResponseEntity.ok(ApiResponse.fail("Bank account already exists."));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/invoice/choose-bank")
    @ResponseBody
    public ResponseEntity<?> chooseBank(@RequestParam Long bankChoose) {
        boolean success = bankService.chooseBank(bankChoose);
        if (!success) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Record with ID 1 not found."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ---- View / Download PDF ----

    @GetMapping("/invoice/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        model.addAttribute("invoice", invoice);
        return invoice.getInvType() != null && invoice.getInvType() == 3
                ? "invoice/government"
                : "invoice/invoice";
    }

    @GetMapping("/invoice/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Invoice invoice = invoiceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        InvoicePdfService.PdfResult pdf = invoicePdfService.renderInvoicePdf(invoice);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.fileName + "\"")
                .body(pdf.bytes);
    }

    // ---- Client search ----

    @GetMapping("/invoice/client-search")
    @ResponseBody
    public ResponseEntity<?> clientSearch(@RequestParam(required = false) String name) {
        // TODO: for large tables, replace with a proper repository query
        // (findTop10ByNameContainingIgnoreCase) instead of filtering in memory.
        return ResponseEntity.ok(List.of());
    }
}
