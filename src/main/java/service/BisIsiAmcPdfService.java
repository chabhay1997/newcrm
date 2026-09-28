package service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import model.BisIsiAmcQuotation;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class BisIsiAmcPdfService {
    public record PdfResult(String filename, byte[] content) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final TemplateEngine templates;
    private final BisIsiAmcQuotationService quotations;

    public BisIsiAmcPdfService(TemplateEngine templates, BisIsiAmcQuotationService quotations) {
        this.templates = templates;
        this.quotations = quotations;
    }

    public PdfResult render(BisIsiAmcQuotation quotation) {
        Context context = new Context();
        context.setVariable("quotationId", quotation.getId());
        context.setVariable("operationId", quotation.getOperationId());
        context.setVariable("referenceNumber", quotations.referenceNumber(quotation));
        context.setVariable("date", quotation.getProposalDate() == null ? "-------------" : quotation.getProposalDate().format(DATE));
        context.setVariable("kindAttention", value(quotation.getKindAttention(), "---------------------------------"));
        context.setVariable("isStandard", value(quotation.getIsStandard(), "-------------"));
        context.setVariable("product", value(quotation.getProduct(), "-------------"));
        context.setVariable("cmlNumber", value(quotation.getCmlNumber(), "-------------"));
        context.setVariable("licenceValidity", quotation.getLicenceValidityDate() == null ? "-------------" : quotation.getLicenceValidityDate().format(DATE));
        context.setVariable("markingFee", value(quotation.getActualMarkingFee(), "As per actual"));
        context.setVariable("sampleFee", value(quotation.getSampleTestingFee(), "As per actual"));
        context.setVariable("engineerFee", value(quotation.getEngineerVisitCharge(), "As per actual"));
        context.setVariable("consultancyFee", value(quotation.getConsultancyServiceFee(), "-------------") + " INR/-");
        context.setVariable("consultancyYearly", value(quotation.getConsultancyOneYear(), "-------------") + " INR/-");
        context.setVariable("letterhead", asset("/static/images/quotation/new.svg", "image/svg+xml"));
        context.setVariable("watermark", asset("/static/images/quotation/evtl-watermark.png", "image/png"));
        context.setVariable("stamp", asset("/static/images/invoice/stamp-black.png", "image/png"));

        String html = templates.process("operation/bis-isi-amc/quotation-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().useSVGDrawer(new BatikSVGDrawer()).withHtmlContent(html, null).toStream(output).run();
            return new PdfResult("AMC-SIT-Quotation-" + quotation.getOperationId() + "-" + LocalDate.now() + ".pdf", output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate AMC/SIT quotation PDF", exception);
        }
    }

    private String value(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }

    private String asset(String path, String mime) {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing quotation asset: " + path);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to read quotation asset: " + path, exception);
        }
    }
}
