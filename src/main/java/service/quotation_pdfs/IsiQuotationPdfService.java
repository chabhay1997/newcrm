package service.quotation_pdfs;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import model.quotations.BisIsiQuotation;
import model.Lead;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

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
        context.setVariable("letterheadDataUri", svgPngDataUri("static/newevtl.svg", 1.0f, 1785, 2526));
        context.setVariable("watermarkDataUri", svgPngDataUri("static/evtl-watermark.svg", 0.08f, 1818, 754));

        String html = templateEngine.process("quotations/isi/isi-pdf", context);
        ByteArrayOutputStream rendered = new ByteArrayOutputStream();
        new PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(html, null)
                .toStream(rendered)
                .run();
        return rendered.toByteArray();
    }

    private String svgPngDataUri(String path, float opacity, int width, int height) throws IOException {
        ClassPathResource asset = new ClassPathResource(path);
        ByteArrayOutputStream svgPng = new ByteArrayOutputStream();
        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);
            transcoder.transcode(
                    new TranscoderInput(asset.getInputStream()),
                    new TranscoderOutput(svgPng));
        } catch (org.apache.batik.transcoder.TranscoderException e) {
            throw new IOException("Unable to rasterize SVG asset " + path, e);
        }

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(svgPng.toByteArray()));
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(output, "png", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
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