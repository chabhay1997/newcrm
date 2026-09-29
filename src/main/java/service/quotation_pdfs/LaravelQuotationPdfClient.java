package service.quotation_pdfs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.quotations.BisIsiQuotation;
import model.quotations.CbRdsoQuotation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LaravelQuotationPdfClient {
    private static final Logger log = LoggerFactory.getLogger(LaravelQuotationPdfClient.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;

    public LaravelQuotationPdfClient(
            ObjectMapper objectMapper,
            @Value("${laravel.quotation-pdf-url:https://check.evtlindia.com/api/integration/quotation-pdf}") String endpoint,
            @Value("${laravel.quotation-pdf-token:}") String token) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.token = token;
    }

    public byte[] downloadIsi(Long leadId, BisIsiQuotation quotation) throws IOException, InterruptedException {
        return download(leadId, "bis_isi", quotation);
    }

    public byte[] download(Long leadId, String quotationType, Object quotation) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lead_id", leadId);
        payload.put("quotation_type", quotationType);
        payload.put("quotation", objectMapper.convertValue(quotation, new TypeReference<Map<String, Object>>() {}));
        return send(payload);
        }

        public byte[] downloadCbRdso(Long leadId, CbRdsoQuotation quotation) throws IOException, InterruptedException {
        Map<String, Object> quotationPayload = objectMapper.convertValue(
            quotation, new TypeReference<Map<String, Object>>() {});
        quotationPayload.put("details", objectMapper.convertValue(
            quotation.details, new TypeReference<java.util.List<Map<String, Object>>>() {}));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lead_id", leadId);
        payload.put("quotation_type", "cb_rdso");
        payload.put("quotation", quotationPayload);
        return send(payload);
        }

        private byte[] send(Map<String, Object> payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/pdf")
                .header("X-Integration-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        HttpStatusCode status = HttpStatusCode.valueOf(response.statusCode());
        if (!status.is2xxSuccessful()) {
            String body = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
            log.error("Laravel PDF request failed with HTTP {}: {}", response.statusCode(), body);
            throw new IOException("Laravel PDF request failed with HTTP " + response.statusCode());
        }
        return response.body();
    }
}