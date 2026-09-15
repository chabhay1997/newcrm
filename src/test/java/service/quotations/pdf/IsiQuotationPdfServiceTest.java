package service.quotations.pdf;

import model.BisIsiQuotation;
import model.Lead;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IsiQuotationPdfServiceTest {
    @Test
    void rendersIsiQuotationWithBundledStationery() throws Exception {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        BisIsiQuotation quotation = new BisIsiQuotation();
        quotation.setReferenceNo("REF-7-R1");
        quotation.setQuotationDate(LocalDate.of(2026, 9, 8));
        quotation.setCompanyName("Example Industries");
        quotation.setCertificateName("Electric Water Heater IS 302");
        quotation.setClientName("Example Client");
        quotation.setCommercialValue1("1000");
        quotation.setCommercialValue2("7000");
        quotation.setCommercialValue3("12000");
        quotation.setCommercialValue4("1000");
        quotation.setCommercialValue5("5000");
        quotation.setConsultancyValue1("45000");
        quotation.setConsultancyFees("Engineer visit travel and food");
        quotation.setAsPerMsme(false);

        Lead lead = new Lead();
        lead.setCompanyName("Example Industries");

        byte[] pdf = new IsiQuotationPdfService(engine).generate(quotation, lead);

        assertTrue(pdf.length > 10_000, "Expected a rendered PDF with the bundled letterhead");
        assertTrue(new String(pdf, 0, 5).equals("%PDF-"), "Expected PDF output");
    }
}
