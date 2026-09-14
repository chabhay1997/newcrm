package service.quotations.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import model.BisIsiQuotation;
import model.Lead;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class IsiQuotationPdfService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final SpringTemplateEngine templateEngine;

    public IsiQuotationPdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generate(BisIsiQuotation quotation, Lead lead) throws IOException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("quotation", quotation);
        context.setVariable("lead", lead);
        context.setVariable("quotationDate", quotation.getQuotationDate() == null
                ? ""
                : quotation.getQuotationDate().format(DATE_FORMAT));
        context.setVariable("commercialItems", List.of(
                item("BIS application fee", quotation.getCommercialAlias1(), quotation.getCommercialValue1()),
                item("Charge for visit (BIS Officer)", quotation.getCommercialAlias2(), quotation.getCommercialValue2()),
                item("Minimum marking fee for one year", quotation.getCommercialAlias3(), quotation.getCommercialValue3()),
                item("License fee", quotation.getCommercialAlias4(), quotation.getCommercialValue4()),
                item("Sample testing charges", quotation.getCommercialAlias5(), quotation.getCommercialValue5())
        ));
        context.setVariable("consultancyItems", List.of(
                item(defaultValue(quotation.getConsultancyCharge(), "Consultancy charge for entire contract"), quotation.getConsultancyAlias1(), quotation.getConsultancyValue1()),
                item(defaultValue(quotation.getConsultancyFees(), "Engineer visit travel and food"), quotation.getConsultancyAlias2(), quotation.getConsultancyValue2()),
                item(defaultValue(quotation.getOtherExpense(), "Other expenses BIS Officer"), quotation.getConsultancyAlias3(), quotation.getConsultancyValue3())
        ));
        context.setVariable("commercialTotal", total(
                quotation.getCommercialValue1(), quotation.getCommercialValue2(), quotation.getCommercialValue3(),
                quotation.getCommercialValue4(), quotation.getCommercialValue5()));
        context.setVariable("consultancyTotal", total(
                quotation.getConsultancyValue1(), quotation.getConsultancyValue2(), quotation.getConsultancyValue3()));
        context.setVariable("letterheadDataUri", assetDataUri("static/letterhead.png", "image/png"));
        context.setVariable("watermarkDataUri", watermarkDataUri());

        String html = templateEngine.process("quotations/isi/isi-pdf", context);
        ByteArrayOutputStream rendered = new ByteArrayOutputStream();
        new PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(html, null)
                .toStream(rendered)
                .run();
        return rendered.toByteArray();
    }

    private String watermarkDataUri() throws IOException {
        return assetDataUri("static/evtl-watermark.svg", "image/svg+xml");
    }

    private String assetDataUri(String path, String contentType) throws IOException {
        ClassPathResource asset = new ClassPathResource(path);
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(asset.getInputStream().readAllBytes());
    }

    private static QuotationItem item(String expense, String alias, String value) {
        return new QuotationItem(expense, alias, value);
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String total(String... values) {
        double sum = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            try {
                sum += Double.parseDouble(value.replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                // Keep non-numeric custom values visible in the line item, but exclude them from totals.
            }
        }
        return String.format(Locale.ENGLISH, "%,.2f", sum);
    }

    public record QuotationItem(String expense, String alias, String value) {}
}
