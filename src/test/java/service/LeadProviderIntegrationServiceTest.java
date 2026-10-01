package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class LeadProviderIntegrationServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private LeadProviderIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new LeadProviderIntegrationService(
                leadRepository, RestClient.builder(), mailSender, templateEngine, "");
    }

    @Test
    void storesJustdialFieldsAndUsesProviderLeadIdForDeduplication() {
        when(leadRepository.existsByUniqueQueryId("JD-123")).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean inserted = service.storeJustdialLead(Map.of(
                "leadid", "JD-123",
                "leadtype", "Certification enquiry",
                "name", "A Client",
                "mobile", "9876543210",
                "email", "",
                "category", "Product compliance",
                "city", "Noida",
                "area", "Sector 63",
                "company", "Example Ltd",
                "pincode", "201301"));

        ArgumentCaptor<Lead> savedLead = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(savedLead.capture());
        Lead lead = savedLead.getValue();
        assertTrue(inserted);
        assertEquals("JD-123", lead.getUniqueQueryId());
        assertEquals("A Client", lead.getIsName());
        assertEquals("Example Ltd", lead.getCompanyName());
        assertEquals("Certification enquiry", lead.getProductName());
        assertEquals("9876543210", lead.getPhone());
        assertEquals("Product compliance", lead.getMessage());
        assertEquals("Noida", lead.getCityId());
        assertEquals("Sector 63", lead.getAddress());
        assertEquals(2L, lead.getSourceId());
        assertEquals("1", lead.getStatus());
    }

    @Test
    void doesNotInsertDuplicateJustdialLead() {
        when(leadRepository.existsByUniqueQueryId("JD-123")).thenReturn(true);

        boolean inserted = service.storeJustdialLead(Map.of("leadid", "JD-123"));

        assertEquals(false, inserted);
        verify(leadRepository, never()).save(any(Lead.class));
    }
}