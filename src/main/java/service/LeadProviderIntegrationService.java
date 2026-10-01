package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.mail.internet.MimeMessage;
import model.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import repository.LeadRepository;

@Service
public class LeadProviderIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(LeadProviderIntegrationService.class);
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter INDIA_MART_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final LeadRepository leadRepository;
    private final RestClient restClient;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String indiaMartApiKey;
    private final String mailFromAddress;
    private final String mailFromName;

    public LeadProviderIntegrationService(
            LeadRepository leadRepository,
            RestClient.Builder restClientBuilder,
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${indiamart.api-key:}") String indiaMartApiKey,
            @Value("${lead.mail.from-address:${spring.mail.username:}}") String mailFromAddress,
            @Value("${lead.mail.from-name:EVTL CRM}") String mailFromName) {
        this.leadRepository = leadRepository;
        this.restClient = restClientBuilder.build();
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.indiaMartApiKey = indiaMartApiKey;
        this.mailFromAddress = mailFromAddress;
        this.mailFromName = mailFromName;
    }

    @Scheduled(fixedDelayString = "${indiamart.poll-interval-ms:300000}")
    public void pollIndiaMartLeads() {
        if (indiaMartApiKey.isBlank()) return;

        LocalDate today = LocalDate.now(INDIA_ZONE);
        String startTime = today.atStartOfDay().format(INDIA_MART_DATE_FORMAT);
        String endTime = LocalDateTime.now(INDIA_ZONE).format(INDIA_MART_DATE_FORMAT);

        try {
            Map<String, Object> payload = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("mapi.indiamart.com")
                            .path("/wservce/crm/crmListing/v2/")
                            .queryParam("glusr_crm_key", indiaMartApiKey)
                            .queryParam("start_time", startTime)
                            .queryParam("end_time", endTime)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Object response = payload == null ? null : payload.get("RESPONSE");
            if (!(response instanceof List<?> leads)) return;

            for (Object item : leads) {
                if (item instanceof Map<?, ?> providerLead) {
                    try {
                        storeIndiaMartLead(providerLead, today);
                    } catch (Exception exception) {
                        logger.error("Unable to store IndiaMART lead {}", providerLead.get("UNIQUE_QUERY_ID"), exception);
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("IndiaMART lead polling failed");
            logger.debug("IndiaMART polling exception", exception);
        }
    }

    public boolean storeJustdialLead(Map<String, ?> payload) {
        String leadId = value(payload, "leadid");
        if (leadId == null || leadId.isBlank()) {
            throw new IllegalArgumentException("leadid is missing");
        }
        if (leadRepository.existsByUniqueQueryId(leadId)) return false;

        Lead lead = new Lead();
        lead.setUniqueQueryId(leadId);
        lead.setCompanyName(value(payload, "company"));
        lead.setIsName(value(payload, "name"));
        lead.setEmail(value(payload, "email"));
        lead.setProductName(value(payload, "leadtype"));
        lead.setPhone(value(payload, "mobile"));
        lead.setMessage(value(payload, "category"));
        lead.setCityId(value(payload, "city"));
        lead.setAddress(value(payload, "area"));
        lead.setPincode(value(payload, "pincode"));
        lead.setSourceId(2L);
        lead.setStatus("1");
        lead.setIsDeleted(false);
        lead.setCreatedAt(LocalDateTime.now(INDIA_ZONE));
        lead.setUpdatedAt(lead.getCreatedAt());
        Lead savedLead = leadRepository.save(lead);
        sendWelcomeEmail(savedLead);
        return true;
    }

    private void storeIndiaMartLead(Map<?, ?> providerLead, LocalDate today) {
        String uniqueQueryId = value(providerLead, "UNIQUE_QUERY_ID");
        if (uniqueQueryId == null || uniqueQueryId.isBlank()
                || leadRepository.existsByUniqueQueryId(uniqueQueryId)) return;

        Lead lead = new Lead();
        lead.setUniqueQueryId(uniqueQueryId);
        lead.setIsName(value(providerLead, "SENDER_NAME"));
        lead.setPhone(value(providerLead, "SENDER_MOBILE"));
        lead.setEmail(value(providerLead, "SENDER_EMAIL"));
        lead.setCompanyName(value(providerLead, "SENDER_COMPANY"));
        lead.setAddress(value(providerLead, "SENDER_ADDRESS"));
        lead.setCountryId(value(providerLead, "SENDER_COUNTRY_ISO"));
        lead.setStateId(value(providerLead, "SENDER_STATE"));
        lead.setCityId(value(providerLead, "SENDER_CITY"));
        lead.setPincode(value(providerLead, "SENDER_PINCODE"));
        lead.setProductName(value(providerLead, "QUERY_PRODUCT_NAME"));
        lead.setRequirements(value(providerLead, "SUBJECT"));
        lead.setMessage(value(providerLead, "QUERY_MESSAGE"));
        lead.setMcatName(value(providerLead, "QUERY_MCAT_NAME"));
        lead.setLeadDate(today);
        lead.setSourceId(1L);
        lead.setStatus("1");
        lead.setIsDeleted(false);
        lead.setCreatedAt(LocalDateTime.now(INDIA_ZONE));
        lead.setUpdatedAt(lead.getCreatedAt());
        sendWelcomeEmail(leadRepository.save(lead));
    }

    private void sendWelcomeEmail(Lead lead) {
        if (lead.getEmail() == null || !EMAIL_PATTERN.matcher(lead.getEmail()).matches()) return;

        try {
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("leadName", lead.getIsName());
            String html = templateEngine.process("mail/leads/welcome", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(lead.getEmail());
            helper.setFrom(mailFromAddress, mailFromName);
            helper.setSubject("Welcome!");
            helper.setReplyTo("contact@evtlindia.com", "Varun Singh");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception exception) {
            logger.warn("Lead {} was stored, but its welcome email could not be sent", lead.getId());
            logger.debug("Lead welcome email exception", exception);
        }
    }

    private static String value(Map<?, ?> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}