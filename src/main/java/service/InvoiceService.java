package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.InvoiceStatus;
import enums.InvoiceType;
import dto.InvoiceTotalsDTO;
import dto.InvoiceLineItemDTO;
import dto.InvoicePreviewUpdateRequest;
import model.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import repository.InvoiceRepository;
import repository.InvoiceEditHistoryRepository;
import repository.UserRepository;
import model.InvoiceEditHistory;
import org.springframework.security.core.context.SecurityContextHolder;
import specification.InvoiceSpecifications;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private InvoiceEditHistoryRepository invoiceEditHistoryRepository;
    @Autowired private UserRepository userRepository;

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    // ---- CRUD ----

    public Optional<Invoice> findById(Long id) {
        return invoiceRepository.findById(id);
    }

    public Invoice saveInvoice(Invoice invoice, List<String> titleList, List<String> particularList, List<String> amtList) {
        invoice.setTitle(joinList(titleList));
        invoice.setParticular(joinList(particularList));
        invoice.setAmtD(joinList(amtList));

        if (invoice.getCreatedAt() == null) {
            invoice.setCreatedAt(LocalDateTime.now());
        }
        invoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    public List<InvoiceLineItemDTO> getLineItems(Invoice invoice) {
        String[] titles = splitStoredValues(invoice.getTitle());
        String[] particulars = splitStoredValues(invoice.getParticular());
        String[] amounts = splitStoredValues(invoice.getAmtD());
        int size = Math.max(titles.length, Math.max(particulars.length, amounts.length));
        if (size == 0) return List.of(new InvoiceLineItemDTO("", "", ""));
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> new InvoiceLineItemDTO(valueAt(titles, i), valueAt(particulars, i), valueAt(amounts, i)))
                .toList();
    }

    private String[] splitStoredValues(String value) {
        if (value == null || value.isEmpty()) return new String[0];
        if (value.trim().startsWith("[")) {
            try {
                List<String> values = objectMapper.readValue(value, new TypeReference<List<String>>() {});
                return values.toArray(String[]::new);
            } catch (Exception ignored) {
                // Fall through to the legacy newline format.
            }
        }
        return value.split("\\R", -1);
    }

    private String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    /** Mirrors performaStore(): create-or-update by id, resolve redirect slug by inv_type. */
    public InvoiceType performaStore(Invoice invoice, List<String> titleList, List<String> particularList, List<String> amtList) {
        boolean isNew = invoice.getId() == null;

        Invoice toSave = invoice;
        if (!isNew) {
            Invoice existing = invoiceRepository.findById(invoice.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoice.getId()));
            copyFieldsOnto(existing, invoice);
            toSave = existing;
        } else {
            toSave.setCreatedBy(util.AuthUtil.currentUserId());
            if (toSave.getDate() == null) {
                toSave.setDate(java.time.LocalDate.now());
            }
        }

        Invoice saved = saveInvoice(toSave, titleList, particularList, amtList);
        if (!isNew) recordEdit(saved.getId());

        Integer invType = saved.getInvType();
        return InvoiceType.fromId(invType);
    }

    private void copyFieldsOnto(Invoice target, Invoice source) {
        target.setName(source.getName());
        target.setInvNo(source.getInvNo());
        target.setPoNo(source.getPoNo());
        target.setInvType(source.getInvType());
        target.setInvTypes(source.getInvTypes());
        target.setAddress(source.getAddress());
        target.setRemarks(source.getRemarks());
        target.setDate(source.getDate());
        target.setPanNo(source.getPanNo());
        target.setSacCode(source.getSacCode());
        target.setGstin(source.getGstin());
        target.setIsSign(source.getIsSign());
        target.setIsTerms(source.getIsTerms());
        target.setIsInr(source.getIsInr());
        target.setBankType(source.getBankType());
        target.setNetAmt(source.getNetAmt());
        target.setExtraServices(source.getExtraServices());
        target.setExtraAmt(source.getExtraAmt());
        target.setFinalAmt(source.getFinalAmt());
    }

    public void delete(Long id) {
        invoiceRepository.deleteById(id);
    }

    public Invoice updateStatus(Long id, Integer status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        invoice.setStatus(status);
        Invoice saved = invoiceRepository.save(invoice);
        recordEdit(saved.getId());
        return saved;
    }

    public List<InvoiceEditHistory> editHistory(Long invoiceId) {
        return invoiceEditHistoryRepository.findByInvoiceIdOrderByEditedAtDesc(invoiceId);
    }

    public Invoice updateFromPreview(Long id, InvoicePreviewUpdateRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        invoice.setInvNo(clean(request.invNo()));
        invoice.setDate(request.date());
        invoice.setPoNo(clean(request.poNo()));
        invoice.setName(clean(request.name()));
        invoice.setAddress(clean(request.address()));
        invoice.setGstin(clean(request.gstin()));
        invoice.setRemarks(clean(request.remarks()));
        List<InvoicePreviewUpdateRequest.PreviewLineItem> items = request.items() == null ? List.of() : request.items();
        BigDecimal netAmount = items.stream()
                .map(InvoicePreviewUpdateRequest.PreviewLineItem::amount)
                .map(this::toSafeBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal extraAmount = toSafeBigDecimal(invoice.getExtraAmt());
        invoice.setNetAmt(netAmount.toPlainString());
        invoice.setFinalAmt(netAmount.add(extraAmount).toPlainString());
        Invoice saved = saveInvoice(invoice,
                items.stream().map(InvoicePreviewUpdateRequest.PreviewLineItem::title).toList(),
                items.stream().map(InvoicePreviewUpdateRequest.PreviewLineItem::particular).toList(),
                items.stream().map(InvoicePreviewUpdateRequest.PreviewLineItem::amount).toList());
        recordEdit(saved.getId());
        return saved;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal toSafeBigDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replace(",", "").trim()); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private void recordEdit(Long invoiceId) {
        String email = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        String editor = email == null ? "Unknown user" : userRepository.findByEmail(email)
                .map(model.User::getName).orElse(email);
        invoiceEditHistoryRepository.save(new InvoiceEditHistory(invoiceId, editor, LocalDateTime.now()));
    }

    // ---- Next invoice / PO number (mirrors getNextInvoiceNumber) ----

    public String[] getNextInvoiceAndPoNo(Integer invTypeId) {
        InvoiceType type = InvoiceType.fromId(invTypeId);

        int lastNumber = invoiceRepository.findTopByInvTypeOrderByIdDesc(type.getId())
                .map(Invoice::getInvNo)
                .map(this::extractTrailingNumber)
                .orElse(0);

        String nextNumber = String.format("%02d", lastNumber + 1);

        return new String[] {
                type.getInvoicePrefix() + nextNumber,
                type.getPoPrefix() + nextNumber
        };
    }

    private int extractTrailingNumber(String invNo) {
        if (invNo == null) return 0;
        Matcher m = TRAILING_NUMBER.matcher(invNo);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    // ---- Totals (mirrors the ->with(['totals' => ...]) block) ----

    public InvoiceTotalsDTO computeTotals(Integer invType, Integer statusFilter, Integer dateFilterDays) {
        return computeTotals(invType, statusFilter, dateFilterDays, null);
    }

    public InvoiceTotalsDTO computeTotals(Integer invType, Integer statusFilter, Integer monthRange, Integer year) {
        Specification<Invoice> spec = InvoiceSpecifications.build(invType, statusFilter, monthRange, year, null);
        List<Invoice> matched = invoiceRepository.findAll(spec);

        double total = sumFinalAmt(matched);
        double paid = sumFinalAmt(filterByStatus(matched, InvoiceStatus.PAID));
        double half = sumFinalAmt(filterByStatus(matched, InvoiceStatus.HALF));
        double unpaid = sumFinalAmt(filterByStatus(matched, InvoiceStatus.UNPAID));

        InvoiceTotalsDTO.Counts counts = new InvoiceTotalsDTO.Counts(
                matched.size(),
                filterByStatus(matched, InvoiceStatus.PAID).size(),
                filterByStatus(matched, InvoiceStatus.HALF).size(),
                filterByStatus(matched, InvoiceStatus.UNPAID).size()
        );

        return new InvoiceTotalsDTO(total, paid, half, unpaid, counts);
    }

    private List<Invoice> filterByStatus(List<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream().filter(i -> status.getId() == (i.getStatus() == null ? 0 : i.getStatus())).toList();
    }

    private double sumFinalAmt(List<Invoice> invoices) {
        return invoices.stream()
                .map(Invoice::getFinalAmt)
                .map(this::toSafeDouble)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private double toSafeDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return new BigDecimal(value.trim()).doubleValue();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ignored) {
            return String.join("\n", values);
        }
    }
}
