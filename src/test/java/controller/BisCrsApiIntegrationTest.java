package controller;

import com.evtl.crm.EvtlCrmApplication;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import repository.BisCrsRenewalRepository;
import repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EvtlCrmApplication.class)
@AutoConfigureMockMvc
@Transactional
class BisCrsApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BisCrsRenewalRepository renewals;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setRoleId(1);
        admin.setTypeId(1L);
        admin.setName("CRS API Admin");
        admin.setEmail("crs.api.admin@evtl.test");
        admin.setPassword("test");
        admin.setStatus(1);
        admin = users.save(admin);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(post("/api/operation/bis-crs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(validJson("Unauthenticated Manufacturer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "crs.api.admin@evtl.test")
    void requiresCsrfProtection() throws Exception {
        mvc.perform(post("/api/operation/bis-crs")
                        .contentType(MediaType.APPLICATION_JSON).content(validJson("CSRF Manufacturer")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "crs.api.admin@evtl.test")
    void createsAndPersistsAValidatedRecord() throws Exception {
        long before = renewals.count();
        mvc.perform(post("/api/operation/bis-crs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(validJson("Production CRS Manufacturer")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/operation/bis-crs/[0-9]+")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.manufacturerName").value("Production CRS Manufacturer"))
                .andExpect(jsonPath("$.licenceNumber").value("R-API-2026"))
                .andExpect(jsonPath("$.createdBy").value(admin.getId()))
                .andExpect(jsonPath("$.message").value("BIS CRS record created successfully"));

        assertThat(renewals.count()).isEqualTo(before + 1);
        var saved = renewals.findAll().stream()
                .filter(record -> "Production CRS Manufacturer".equals(record.getManufacturerName()))
                .findFirst().orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo(admin.getId());
        assertThat(saved.getNotifyDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 1));
        assertThat(saved.getAirEmailId()).isEqualTo("compliance@example.com");
    }

    @Test
    @WithMockUser(username = "crs.api.admin@evtl.test")
    void rejectsDuplicatesAndInvalidPayloads() throws Exception {
        String request = validJson("Duplicate CRS Manufacturer");
        mvc.perform(post("/api/operation/bis-crs").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/operation/bis-crs").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate BIS CRS record"));

        mvc.perform(post("/api/operation/bis-crs").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"manufacturerName\":\"\",\"notifyDate\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid BIS CRS request"));
        mvc.perform(post("/api/operation/bis-crs").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"manufacturerName\":\"Bad Date\",\"notifyDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed JSON request"));
    }

    @Test
    @WithMockUser(username = "crs.no.permission@evtl.test")
    void rejectsAuthenticatedUsersWithoutOperationPermission() throws Exception {
        User user = new User();
        user.setRoleId(2);
        user.setTypeId(1L);
        user.setName("No Operation Permission");
        user.setEmail("crs.no.permission@evtl.test");
        user.setPassword("test");
        user.setStatus(1);
        users.save(user);
        mvc.perform(post("/api/operation/bis-crs").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(validJson("Forbidden Manufacturer")))
                .andExpect(status().isForbidden());
    }

    private String validJson(String manufacturer) {
        return """
                {
                  "manufacturerName":"%s",
                  "productName":"Wireless Device",
                  "licenceNumber":"R-API-2026",
                  "isStandard":"IS 13252",
                  "licenceDate":"2026-09-24",
                  "expiryDate":"2028-09-23",
                  "notifyDate":"2026-10-01",
                  "brandName":"EVTL Test",
                  "airName":"Authorized Representative",
                  "airAddress":"New Delhi, India",
                  "airEmailId":"compliance@example.com",
                  "airContactNumber":"9876543210",
                  "currentStatus":"Active"
                }
                """.formatted(manufacturer);
    }
}
