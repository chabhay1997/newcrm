package service;

import dto.DataTableResponse;
import dto.InvoiceRowDTO;
import dto.InvoiceTotalsDTO;
import enums.InvoiceType;
import model.Invoice;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import repository.InvoiceRepository;
import repository.UserRepository;
import specification.InvoiceSpecifications;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceListingService {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InvoiceService invoiceService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private static final Map<Integer, String> STATUS_LABELS = Map.of(
            1, "Paid", 2, "Half-Paid", 3, "UnPaid"
    );

    public DataTableResponse<InvoiceRowDTO> listForDataTable(
            String typeSlug, int draw, int start, int length,
            Integer monthRange, Integer year, Integer statusFilter, String nameKeyword,
            String contextPath) {

        InvoiceType type = typeSlug != null ? InvoiceType.fromSlug(typeSlug) : null;
        Integer invTypeId = type != null ? type.getId() : null;

        Specification<Invoice> baseSpec = InvoiceSpecifications.build(invTypeId, null, monthRange, year, nameKeyword);
        long recordsTotal = invoiceRepository.count(baseSpec);

        Specification<Invoice> filteredSpec = InvoiceSpecifications.build(invTypeId, statusFilter, monthRange, year, nameKeyword);
        long recordsFiltered = invoiceRepository.count(filteredSpec);

        Pageable pageable = PageRequest.of(start / Math.max(length, 1), Math.max(length, 1), Sort.by(Sort.Direction.DESC, "id"));
        List<Invoice> page = invoiceRepository.findAll(filteredSpec, pageable).getContent();

        List<InvoiceRowDTO> rows = page.stream()
                .map(inv -> toRowDto(inv, contextPath))
                .toList();

        InvoiceTotalsDTO totals = invoiceService.computeTotals(invTypeId, statusFilter, monthRange, year);

        return new DataTableResponse<>(draw, recordsTotal, recordsFiltered, rows, totals);
    }

    private InvoiceRowDTO toRowDto(Invoice invoice, String contextPath) {
        String userName = userRepository.findById(invoice.getCreatedBy() == null ? -1L : invoice.getCreatedBy())
                .map(User::getName)
                .orElse("N/A");
        String date = invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATE_FMT) : "N/A";
        String createdByHtml = escape(userName);

        String invTypeLabel = enums.InvoiceType.labelFor(invoice.getInvType());

        String downloadUrl = contextPath + "/invoice/download/" + invoice.getId();
        String nameHtml = "<div class=\"invoice-name-actions\">"
                + "<span class=\"invoice-customer-name\">" + escape(invoice.getName()) + "</span>"
                + "<button type=\"button\" class=\"invoice-preview-trigger\" title=\"Preview and edit invoice\" aria-label=\"Preview and edit invoice for " + escape(invoice.getName()) + "\">"
                + "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/></svg>"
                + "</button>"
                + "<a href=\"" + downloadUrl + "\" class=\"invoice-download-link\" title=\"Download invoice\" aria-label=\"Download invoice\">"
                + "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M12 3v12m0 0 5-5m-5 5-5-5M5 19h14\"/></svg>"
                + "</a></div>";

        String statusHtml = buildStatusSelect(invoice);

        String actionHtml = util.AuthUtil.isAdmin()
                ? "<button class=\"delete btn btn-danger btn-sm p-2 ml-1\" data-id=\"" + invoice.getId() + "\">"
                  + "<i class=\"fa fa-trash\"></i></button>"
                : "";

        return new InvoiceRowDTO(
                invoice.getId(),
                createdByHtml,
                date,
                nameHtml,
                escape(invoice.getInvNo()),
                escape(invoice.getPoNo()),
                escape(invoice.getFinalAmt()),
                invTypeLabel,
                statusHtml,
                actionHtml
        );
    }

    private String buildStatusSelect(Invoice invoice) {
        StringBuilder options = new StringBuilder("<option value=''>-- Select --</option>");
        List.of(1, 2, 3).forEach(key -> {
            String value = STATUS_LABELS.get(key);
            String selected = key.equals(invoice.getStatus()) ? "selected" : "";
            options.append("<option value='").append(key).append("' ").append(selected).append(">").append(value).append("</option>");
        });
        String statusClass = invoice.getStatus() == null ? "" : " status-" + invoice.getStatus();
        return "<select class=\"form-control status-dropdown" + statusClass + "\" data-id=\"" + invoice.getId() + "\">"
                + options + "</select>";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
