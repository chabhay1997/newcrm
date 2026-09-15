package service;

import model.Bank;
import model.GlobalSetting;
import model.Invoice;
import model.InvoiceTermsCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import repository.BankRepository;
import repository.GlobalSettingRepository;
import repository.InvoiceTermsConditionRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvoicePdfService {

    @Autowired private TemplateEngine templateEngine;
    @Autowired private BankRepository bankRepository;
    @Autowired private GlobalSettingRepository globalSettingRepository;
    @Autowired private InvoiceTermsConditionRepository termsRepository;
    @Autowired private BankNameResolver bankNameResolver;
    @Autowired private InvoiceService invoiceService;

    public record PdfLineItem(int serialNumber, String title, String particular, String amount) {}

    public static class PdfResult {
        public final byte[] bytes;
        public final String fileName;
        public PdfResult(byte[] bytes, String fileName) { this.bytes = bytes; this.fileName = fileName; }
    }

    /** Mirrors download(): resolves bank, terms, picks template by inv_type, renders PDF. */
    public PdfResult renderInvoicePdf(Invoice invoice) {
        Bank bank = resolveBank(invoice);

        List<String> terms = termsRepository.findByInvTypeOrderByIdAsc(invoice.getInvType())
                .stream()
                .map(InvoiceTermsCondition::getTermsCondtions)
                .collect(Collectors.toList());

        Context context = new Context();
        context.setVariable("invoice", invoice);
        context.setVariable("bank", bank);
        context.setVariable("invoiceTerms", terms);
        List<PdfLineItem> lineItems = invoiceService.getLineItems(invoice).stream()
                .map(item -> new PdfLineItem(0, item.title(), item.particular(), formatAmount(item.amount())))
                .collect(Collectors.toList());
        context.setVariable("invoicePages", paginateLineItems(lineItems));
        context.setVariable("heading", invoice.getInvTypes() != null && invoice.getInvTypes() == 2
                ? "Tax Invoice" : "Export Invoice");
        context.setVariable("companyName", companyName(invoice.getInvType()));
        context.setVariable("companyAddress", companyAddress(invoice.getInvType()));
        context.setVariable("companyEmail", invoice.getInvType() != null && invoice.getInvType() == 1
                ? "accounts@evtlindia.com" : "");
        context.setVariable("bankName", bankNameResolver.resolve(bank.getBankName()));
        context.setVariable("accountType", accountTypeName(bank.getAccType()));
        context.setVariable("currencySymbol", invoice.getIsInr() != null && invoice.getIsInr() == 1 ? "" : "$");
        context.setVariable("currencyLabel", invoice.getIsInr() != null && invoice.getIsInr() == 1 ? "(INR)" : "(USD)");
        context.setVariable("netAmountFormatted", formatAmount(invoice.getNetAmt()));
        context.setVariable("extraAmountFormatted", formatAmount(invoice.getExtraAmt()));
        context.setVariable("finalAmountFormatted", formatAmount(invoice.getFinalAmt()));
        context.setVariable("totalInWords", amountInWords(invoice.getFinalAmt()));
        context.setVariable("logoDataUri", imageDataUri(logoResource(invoice.getInvType())));
        context.setVariable("signatureDataUri", imageDataUri(signatureResource(invoice.getInvType())));
        context.setVariable("governmentHeaderDataUri", imageDataUri("/static/images/invoice/government-bis-header.png"));

        String template = invoice.getInvType() != null && invoice.getInvType() == 3
                ? "invoice/government" : "invoice/invoice";
        String html = templateEngine.process(template, context);

        byte[] pdfBytes = renderHtmlToPdf(html);

        String safeName = (invoice.getName() == null ? "invoice" : invoice.getName())
                .replaceAll("[^A-Za-z0-9_-]+", "_");
        safeName = safeName.isBlank() ? "invoice" : safeName;
        String fileName = safeName + "_Invoice_" + LocalDate.now() + ".pdf";

        return new PdfResult(pdfBytes, fileName);
    }

    private List<PdfLineItem> buildLineItems(Invoice invoice) {
        String[] titles = splitLines(invoice.getTitle());
        String[] particulars = splitLines(invoice.getParticular());
        String[] amounts = splitLines(invoice.getAmtD());
        int size = Math.max(titles.length, Math.max(particulars.length, amounts.length));
        List<PdfLineItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(new PdfLineItem(i + 1, valueAt(titles, i), valueAt(particulars, i), formatAmount(valueAt(amounts, i))));
        }
        return items;
    }

    /** A PDF page may contain at most fifteen invoice entries. */
    private List<List<PdfLineItem>> paginateLineItems(List<PdfLineItem> lineItems) {
        if (lineItems.isEmpty()) return List.of(Collections.emptyList());
        List<List<PdfLineItem>> pages = new ArrayList<>();
        for (int start = 0; start < lineItems.size(); start += 15) {
            List<PdfLineItem> page = new ArrayList<>();
            for (int i = start; i < Math.min(start + 15, lineItems.size()); i++) {
                PdfLineItem item = lineItems.get(i);
                page.add(new PdfLineItem(i + 1, item.title(), item.particular(), item.amount()));
            }
            pages.add(page);
        }
        return pages;
    }

    private String[] splitLines(String value) {
        return value == null || value.isEmpty() ? new String[0] : value.split("\\R", -1);
    }

    private String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    private String companyName(Integer invoiceType) {
        if (invoiceType != null && invoiceType == 2) return "PROLIX INDIA & REGULATORY COMPLIANCE CONSULTANT";
        if (invoiceType != null && invoiceType == 3) return "GOVERNMENT";
        return "EMPHATIC VANS & TESTING LABS PRIVATE LIMITED";
    }

    private String companyAddress(Integer invoiceType) {
        if (invoiceType != null && invoiceType == 2) {
            return "796/B-1, FF, Niti Khand 1, Indirapuram, Ghaziabad, Uttar Pradesh - 201014";
        }
        if (invoiceType != null && invoiceType == 3) return "";
        return "F-88-89 B, S/F, STREET NO-08, MANGAL BAZAR, LAXMI NAGAR, East Delhi, 110092";
    }

    private String logoResource(Integer invoiceType) {
        if (invoiceType != null && invoiceType == 3) return "";
        return invoiceType != null && invoiceType == 2
                ? "/static/images/invoice/prolix.jpeg"
                : "/static/images/invoice/evtl-watermark.png";
    }

    private String signatureResource(Integer invoiceType) {
        if (invoiceType != null && invoiceType == 3) return "";
        return invoiceType != null && invoiceType == 2
                ? "/static/images/invoice/prolix.png"
                : "/static/images/invoice/dir-sign1.png";
    }

    private String imageDataUri(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) return "";
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) return "";
            String mime = resourcePath.endsWith(".jpeg") ? "image/jpeg" : "image/png";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String accountTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "Savings Account";
            case 2 -> "Current Account";
            case 3 -> "Cash Credit Account";
            case 4 -> "Overdraft Account";
            case 5 -> "Salary Account";
            case 6 -> "Fixed Deposit Account";
            case 7 -> "Recurring Deposit Account";
            case 8 -> "NRE Account";
            case 9 -> "NRO Account";
            case 10 -> "FCNR Account";
            case 11 -> "Demat Account";
            case 12 -> "Loan Account";
            default -> "Account Type " + type;
        };
    }

    private String formatAmount(String value) {
        BigDecimal amount;
        try { amount = new BigDecimal(value == null || value.isBlank() ? "0" : value.replace(",", "").trim()); }
        catch (NumberFormatException ignored) { amount = BigDecimal.ZERO; }
        String[] parts = amount.setScale(2, RoundingMode.HALF_UP).toPlainString().split("\\.");
        String whole = parts[0];
        boolean negative = whole.startsWith("-");
        if (negative) whole = whole.substring(1);
        String grouped;
        if (whole.length() <= 3) grouped = whole;
        else {
            String lastThree = whole.substring(whole.length() - 3);
            String prefix = whole.substring(0, whole.length() - 3);
            grouped = prefix.replaceAll("(?<=\\d)(?=(\\d{2})+$)", ",") + "," + lastThree;
        }
        return (negative ? "-" : "") + grouped + "." + parts[1];
    }

    private String amountInWords(String value) {
        long amount;
        try { amount = new BigDecimal(value == null || value.isBlank() ? "0" : value.replace(",", "").trim())
                .setScale(0, RoundingMode.HALF_UP).longValueExact(); }
        catch (Exception ignored) { amount = 0; }
        return numberToWords(amount).toUpperCase();
    }

    private String numberToWords(long number) {
        if (number == 0) return "Zero";
        if (number < 0) return "Minus " + numberToWords(-number);
        StringBuilder words = new StringBuilder();
        long[] values = {10_000_000L, 100_000L, 1_000L, 100L};
        String[] labels = {"Crore", "Lakh", "Thousand", "Hundred"};
        for (int i = 0; i < values.length; i++) {
            if (number >= values[i]) {
                words.append(numberToWords(number / values[i])).append(' ').append(labels[i]).append(' ');
                number %= values[i];
            }
        }
        if (number > 0 && words.length() > 0) words.append("and ");
        if (number >= 20) {
            String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
            words.append(tens[(int) number / 10]);
            if (number % 10 > 0) words.append(' ').append(numberToWords(number % 10));
        } else if (number > 0) {
            String[] small = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                    "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
            words.append(small[(int) number]);
        }
        return words.toString().trim();
    }

    private Bank resolveBank(Invoice invoice) {
        if (invoice.getBankType() != null) {
            Optional<Bank> selectedBank = bankRepository.findById(invoice.getBankType());
            if (selectedBank.isPresent()) {
                return selectedBank.get();
            }

            // Older Government invoices can reference bank IDs from the previous
            // table. Use the designated BIS account rather than failing the PDF.
            if (invoice.getInvType() != null && invoice.getInvType() == 3) {
                Optional<Bank> bisBank = bankRepository
                        .findFirstByBankDetailsContainingIgnoreCaseOrderByIdAsc("Bureau of Indian Standards");
                if (bisBank.isPresent()) {
                    return bisBank.get();
                }
            }
        }

        Optional<Long> globallySelectedBankId = globalSettingRepository.findById(1L)
                .map(GlobalSetting::getBankChoose)
                .filter(id -> id != null);

        if (globallySelectedBankId.isPresent()) {
            Optional<Bank> globallySelectedBank = bankRepository.findById(globallySelectedBankId.get());
            if (globallySelectedBank.isPresent()) {
                return globallySelectedBank.get();
            }
        }

        return bankRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "No active bank is available. Add a bank before downloading this invoice."));
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
}
