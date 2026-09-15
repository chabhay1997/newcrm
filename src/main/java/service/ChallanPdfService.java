package service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import model.Challan;
import model.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repository.ChallanRepository;
import repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChallanPdfService {
    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private final ChallanRepository challanRepository;
    private final UserRepository userRepository;

    public ChallanPdfService(ChallanRepository challanRepository, UserRepository userRepository) {
        this.challanRepository = challanRepository;
        this.userRepository = userRepository;
    }

    public ChallanPdf downloadData(long id) {
        Challan c = challanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challan not found"));
        User creator = c.getCreatedBy() == null ? null : userRepository.findById(c.getCreatedBy()).orElse(null);
        String html = buildHtml(c, creator, calculateAmounts(c));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            String name = blank(c.getChallanNo()) ? "challan" : c.getChallanNo();
            return new ChallanPdf(name.replaceAll("[^A-Za-z0-9_-]+", "_") + ".pdf", output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate challan PDF", exception);
        }
    }

    private String buildHtml(Challan c, User creator, Amounts amounts) {
        String creatorName = creator == null ? "" : creator.getName();
        String creatorEmail = creator == null ? "" : creator.getEmail();
        String creatorPhone = creator == null ? "" : creator.getPhone();
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8"/><style>
            @page{size:A4;margin:14mm 12mm 16mm}*{box-sizing:border-box}body{font-family:Arial,sans-serif;color:#2f3e5d;font-size:11.2pt;margin:0}
            h1{font-size:15pt;margin:0 0 10px;font-weight:700}.box{border:1px solid #111}.row{width:100%%;display:table;table-layout:fixed}.cell{display:table-cell;vertical-align:middle}
            .company{width:60%%;height:98px;border-right:1px solid #111;padding:4px 8px}.company-logo{width:21%%;text-align:center}.company-logo img{width:92px}.company-text{width:79%%;font-size:9.5pt;line-height:1.18}.company-name{font-weight:700;font-size:13.2pt;line-height:1.12}
            .meta{width:40%%;padding:8px 5px;font-size:11.5pt}.meta-label{display:inline-block;width:105px;font-weight:700}.party{height:96px;border-top:1px solid #111}.bill{width:60%%;border-right:1px solid #111;padding:3px 8px;vertical-align:top;line-height:1.25}.created{width:40%%;padding:3px 10px;vertical-align:top;line-height:1.28}.title{font-size:15pt;font-weight:700}.client-name{margin:1px 0 5px;color:#1f3558;font-size:15pt;font-weight:700;line-height:1.15}.strong{font-weight:700;font-size:12.5pt}
            table{border-collapse:collapse;width:100%%;table-layout:fixed}th,td{border:1px solid #111;padding:4px;text-align:center;vertical-align:top}th{font-weight:700;font-size:11.8pt;white-space:nowrap}.items{border-left:0;border-right:0}.items th:first-child,.items td:first-child{border-left:0}.items th:last-child,.items td:last-child{border-right:0}.item-row td{height:306px;font-size:12.5pt}.item-row .description{text-align:left;padding-left:16px;font-size:10pt}.total-row td{height:25px;font-size:12pt;vertical-align:middle}.remarks{border-top:0;padding:4px 8px;min-height:25px;font-size:11.5pt}
            .footer{margin-top:12px}.words{height:28px;border-bottom:1px solid #111;padding:4px 2px;font-size:12.2pt}.words-label{font-weight:700;float:left}.words-value{float:right}.footer-row{height:100px}.terms{width:50%%;border-right:1px solid #111;padding:3px 12px;vertical-align:top;line-height:1.22}.terms-title{font-weight:700;font-size:12.5pt}.sign{width:50%%;text-align:center;vertical-align:bottom;padding-bottom:5px}.stamp{width:110px;margin:0 auto -45px}.sign-text{font-size:11.8pt;line-height:1.15}.company-sign{font-size:10.2pt;white-space:nowrap}.dash{color:#555}
            </style></head><body><h1>DELIVERY CHALLAN</h1><div class="box">
            <div class="row"><div class="cell company"><div class="row"><div class="cell company-logo"><img src="%s" alt="EVTL INDIA"/></div><div class="cell company-text"><div class="company-name">EMPHATIC VANS &amp; TESTING LABS<br/>PRIVATE LIMITED</div>F-88-89 B, S/F, STREET NO-08, MANGAL BAZAR,<br/>LAXMI NAGAR, East Delhi, 110092<br/>GSTIN: 07AAFCE4483M1Z7<br/>PAN NUMBER: AAFCE4483M<br/>EMAIL: accounts@evtlindia.com</div></div></div><div class="cell meta"><div><span class="meta-label">Challan No</span>%s</div><div><span class="meta-label">Challan Date</span>%s</div></div></div>
            <div class="row party"><div class="cell bill"><div class="title">Bill To</div><div class="client-name">%s</div><div>Address:- %s</div><div>PinCode:- %s</div></div><div class="cell created"><div class="title">Created By</div><div class="strong">%s</div><div>%s</div><div>%s</div></div></div>
            <table class="items"><colgroup><col style="width:7%%"/><col style="width:60%%"/><col style="width:5.5%%"/><col style="width:11%%"/><col style="width:5.5%%"/><col style="width:11%%"/></colgroup><thead><tr><th>S.No</th><th>Item Name</th><th>QTY</th><th>AMOUNT</th><th>TAX</th><th>TOT AMT</th></tr></thead><tbody><tr class="item-row"><td>1</td><td class="description">%s%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr><tr class="total-row"><td></td><td>Total</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr></tbody></table><div class="remarks">Remarks:- %s</div></div>
            <div class="box footer"><div class="words"><span class="words-label">Total Amount (In Words):</span><span class="words-value">%s</span></div><div class="row footer-row"><div class="cell terms"><div class="terms-title">Terms and Conditions</div><div>1.&#160;&#160; This is not for sale.</div><div>2.&#160;&#160; Sample will be return after completion of days 90.</div></div><div class="cell sign"><img class="stamp" src="%s" alt="Authorized stamp"/><div class="sign-text">Authorized Signatory For</div><div class="company-sign">EMPHATIC VANS &amp; TESTING LABS PRIVATE LIMITED</div></div></div></div></body></html>
            """.formatted(image("static/images/invoice/evtl-watermark.png"), value(c.getChallanNo()), c.getDate() == null ? dash() : DateTimeFormatter.ISO_LOCAL_DATE.format(c.getDate()), value(c.getClientName()), value(c.getAddress()), value(c.getPincode()), value(creatorName), value(creatorEmail), value(creatorPhone), value(c.getItemName()), brand(c.getBrandName()), value(c.getQty()), money(amounts.unit), tax(c.getGst()), totalMoney(amounts.total), value(c.getQty()), money(amounts.unit), tax(c.getGst()), totalMoney(amounts.total), value(c.getRemark()), numberToWords(amounts.total), image("static/images/invoice/stamp-black.png"));
    }

    private Amounts calculateAmounts(Challan c) {
        BigDecimal quantity = firstNumber(c.getQty(), BigDecimal.ONE);
        BigDecimal unit = decimal(c.getAmount());
        BigDecimal gst = decimal(c.getGst());
        BigDecimal computed = quantity.multiply(unit).multiply(BigDecimal.ONE.add(gst.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
        BigDecimal total = blank(c.getTotalAmount()) ? computed : decimal(c.getTotalAmount());
        return new Amounts(unit, total.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal firstNumber(String text, BigDecimal fallback) { if (text == null) return fallback; Matcher m = FIRST_NUMBER.matcher(text.replace(",", "")); return m.find() ? decimal(m.group()) : fallback; }
    private BigDecimal decimal(String text) { if (blank(text)) return BigDecimal.ZERO; try { return new BigDecimal(text.trim().replace(",", "")); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; } }
    private String money(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private String totalMoney(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private String tax(String text) { return blank(text) ? "%" : value(text) + "%"; }
    private String brand(String text) { return blank(text) ? "" : " (" + value(text) + ")"; }
    private String dash() { return "<span class=\"dash\">-</span>"; }
    private boolean blank(String text) { return text == null || text.isBlank(); }
    private String image(String path) { try { byte[] bytes = new ClassPathResource(path).getInputStream().readAllBytes(); return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes); } catch (IOException exception) { throw new IllegalStateException("Missing PDF asset: " + path, exception); } }
    private String value(String text) { if (blank(text)) return dash(); return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;").replace("\r\n", "<br/>").replace("\n", "<br/>"); }

    static String numberToWords(BigDecimal amount) { long number = amount.setScale(0, RoundingMode.HALF_UP).longValue(); if (number == 0) return "ZERO"; if (number < 0) return "MINUS " + numberToWords(BigDecimal.valueOf(-number)); return words(number).trim().toUpperCase(); }
    private static String words(long number) { StringBuilder result = new StringBuilder(); append(result, number / 10_000_000, "crore"); number %= 10_000_000; append(result, number / 100_000, "lakh"); number %= 100_000; append(result, number / 1_000, "thousand"); number %= 1_000; append(result, number / 100, "hundred"); number %= 100; if (number > 0) { if (!result.isEmpty()) result.append(' '); result.append(twoDigits((int) number)); } return result.toString(); }
    private static void append(StringBuilder result, long count, String scale) { if (count == 0) return; if (!result.isEmpty()) result.append(' '); result.append(count < 100 ? twoDigits((int) count) : words(count)).append(' ').append(scale); }
    private static String twoDigits(int number) { String[] small = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"}; String[] tens = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"}; return number < 20 ? small[number] : tens[number / 10] + (number % 10 == 0 ? "" : " " + small[number % 10]); }

    private record Amounts(BigDecimal unit, BigDecimal total) { }
    public record ChallanPdf(String filename, byte[] content) { }
}
