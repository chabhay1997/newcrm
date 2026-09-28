package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import model.QuotationTermsCondition;
import model.TestingEquipment;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import repository.QuotationTermsConditionRepository;
import repository.TestingEquipmentRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class QuotationPdfService {
    public record Line(int serial, String description, String detail, String hsnCode, String quantity, String actualPrice, String price) { }
    public record PreviewLine(int serial, String description, String detail, String hsnCode, String quantity, String actualPrice, String price, String total) { }
    public record PreviewData(Long id, String invoiceNo, java.time.LocalDate date, String attention,
                              String clientName, String companyName, String isCode,
                              List<PreviewLine> items, String subtotal) { }
    public record PdfResult(String filename, byte[] content) { }
    private final TestingEquipmentRepository equipmentRepository;
    private final QuotationTermsConditionRepository termsRepository;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    public QuotationPdfService(TestingEquipmentRepository equipmentRepository, QuotationTermsConditionRepository termsRepository, TemplateEngine templateEngine, ObjectMapper objectMapper) { this.equipmentRepository = equipmentRepository; this.termsRepository = termsRepository; this.templateEngine = templateEngine; this.objectMapper = objectMapper; }

    public PdfResult download(long id) {
        TestingEquipment quotation = equipmentRepository.findById(Math.toIntExact(id)).orElseThrow(() -> new IllegalArgumentException("Quotation not found"));
        List<Line> items = new ArrayList<>();
        List<TestingEquipment> records = quotation.getInvoiceNo() == null ? List.of(quotation) : equipmentRepository.findByInvoiceNoOrderByIdAsc(quotation.getInvoiceNo());
        for (TestingEquipment record : records) addItems(items, record.getDesQtyPrice());
        if (items.isEmpty()) items.add(new Line(1, value(quotation.getEquipmentName()), value(quotation.getDescription()), value(quotation.getHsnCode()), "1", money(quotation.getActualPrice()), money(quotation.getActualPrice())));
        BigDecimal total = items.stream().map(line -> number(line.quantity()).multiply(number(line.price()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Context context = new Context(); context.setVariable("quotation", quotation); context.setVariable("items", items); context.setVariable("total", money(total)); context.setVariable("terms", termsRepository.findFirstByOrderByIdDesc().orElse(null)); context.setVariable("date", quotation.getDate() == null ? "-" : quotation.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); context.setVariable("year", java.time.Year.now().getValue()); context.setVariable("background", asset("/static/images/quotation/new.svg", "image/svg+xml")); context.setVariable("termsBackground", asset("/static/images/quotation/emp12.svg", "image/svg+xml")); context.setVariable("watermark", asset("/static/images/quotation/evtl-watermark.png", "image/png")); context.setVariable("stamp", asset("/static/images/quotation/stamp.png", "image/png"));
        String html = templateEngine.process("lab-equipment/quotation-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) { new PdfRendererBuilder().useSVGDrawer(new BatikSVGDrawer()).withHtmlContent(html, null).toStream(output).run(); String base = value(quotation.getClientName()).replaceAll("[^A-Za-z0-9_-]+", "_"); return new PdfResult((base.isBlank() ? "quotation" : base) + "-" + date(context, quotation) + ".pdf", output.toByteArray()); } catch (Exception exception) { throw new IllegalStateException("Unable to generate quotation PDF", exception); }
    }
    public PreviewData preview(long id) {
        TestingEquipment quotation = equipmentRepository.findById(Math.toIntExact(id))
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found"));
        List<Line> lines = new ArrayList<>();
        List<TestingEquipment> records = quotation.getInvoiceNo() == null
                ? List.of(quotation)
                : equipmentRepository.findByInvoiceNoOrderByIdAsc(quotation.getInvoiceNo());
        for (TestingEquipment record : records) addItems(lines, record.getDesQtyPrice());
        if (lines.isEmpty()) lines.add(new Line(1, value(quotation.getEquipmentName()), value(quotation.getDescription()),
                value(quotation.getHsnCode()), "1", money(quotation.getActualPrice()), money(quotation.getActualPrice())));
        List<PreviewLine> items = lines.stream().map(line -> new PreviewLine(line.serial(), line.description(),
                line.detail(), line.hsnCode(), line.quantity(), line.actualPrice(), line.price(),
                money(number(line.quantity()).multiply(number(line.price()))))).toList();
        BigDecimal subtotal = items.stream().map(item -> number(item.total())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PreviewData(quotation.getId() == null ? null : quotation.getId().longValue(),
                quotation.getInvoiceNo(), quotation.getDate(), quotation.getAttention(), quotation.getClientName(),
                quotation.getCompanyName(), quotation.getIsCode(), items, money(subtotal));
    }
    private void addItems(List<Line> target, String json) { try { JsonNode root = objectMapper.readTree(json == null ? "{}" : json); if (root.isArray()) for (JsonNode node : root) add(target, node); else add(target, root); } catch (Exception ignored) { } }
    private void add(List<Line> target, JsonNode node) {
        if (node == null || node.isMissingNode()) return;
        String description = text(node, "desc");
        String detail = text(node, "description", "detail");
        if (description.isBlank()) {
            description = detail;
            detail = "";
        }
        target.add(new Line(target.size() + 1, description, detail,
                text(node, "hsnCode", "hsn_code"), text(node, "quantity"),
                money(number(text(node, "actualPrice", "actual_price"))), money(number(text(node, "price")))));
    }
    private String text(JsonNode node, String... names) { for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText(); return ""; }
    private BigDecimal number(String value) { try { return new BigDecimal(value == null || value.isBlank() ? "0" : value.replace(",", "")); } catch (Exception ignored) { return BigDecimal.ZERO; } }
    private String money(BigDecimal amount) { return String.format(Locale.US, "%,.2f", amount == null ? BigDecimal.ZERO : amount); }
    private String value(String text) { return text == null ? "" : text; }
    private String date(Context ignored, TestingEquipment quotation) { return quotation.getDate() == null ? "quotation" : quotation.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")); }
    private String asset(String path, String mime) { try (var input = getClass().getResourceAsStream(path)) { return input == null ? "" : "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(input.readAllBytes()); } catch (Exception ignored) { return ""; } }
}
