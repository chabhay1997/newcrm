package controller;

import com.evtl.crm.EvtlCrmApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import repository.TestingEquipmentRepository;
import repository.QuotationTestingEquipmentRepository;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EvtlCrmApplication.class)
@AutoConfigureMockMvc
class QuotationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestingEquipmentRepository testingEquipmentRepository;

    @Autowired
    private QuotationTestingEquipmentRepository quotationTestingEquipmentRepository;

    @Test
    void rejectsUnauthenticatedApiRequests() throws Exception {
        mockMvc.perform(get("/api/lab-equipment/quotations").accept("application/json"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @WithMockUser
    void rendersTheQuotationDashboardForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/lab-equipment/quotation"))
                .andExpect(status().isOk())
                .andExpect(view().name("lab-equipment/quotation"))
                .andExpect(model().attribute("activePage", "lab-equipment-quotation"));
    }

    @Test
    @WithMockUser
    void rendersTheAddEquipmentQuotationDashboardForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/lab-equipment/quotation/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("lab-equipment/create-quotation"))
                .andExpect(model().attribute("activePage", "lab-equipment-quotation"))
                .andExpect(model().attributeExists("quotationDate"));
    }

    @Test
    @WithMockUser(username = "quotation-api-test@evtl.in")
    void returnsTheTestingEquipmentRecordsUsingThePublishedContract() throws Exception {
        long databaseCount = testingEquipmentRepository.count();
        mockMvc.perform(get("/api/lab-equipment/quotations").param("page", "1").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(25)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(25))
                .andExpect(jsonPath("$.totalRecords").value(databaseCount))
                .andExpect(jsonPath("$.hasPrevious").isBoolean())
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    @WithMockUser
    void rejectsInvalidInputAndTreatsInjectionLikeSearchAsData() throws Exception {
        mockMvc.perform(get("/api/lab-equipment/quotations").param("page", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lab-equipment/quotations").param("page", "10001"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lab-equipment/quotations").param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lab-equipment/quotations").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lab-equipment/quotations").param("q", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isEmpty())
                .andExpect(jsonPath("$.totalRecords").value(0));
    }

    @Test
    @WithMockUser(username = "quotation-create-test@evtl.in")
    @Transactional
    void securelyCreatesLinkedQuotationAndEquipmentRowsAndRollsThemBackAfterTheTest() throws Exception {
        long equipmentCountBefore = testingEquipmentRepository.count();
        long quotationCountBefore = quotationTestingEquipmentRepository.count();
        String request = """
                {
                  "invoiceNo":"EVTL/TEST-EQ",
                  "date":"2026-09-11",
                  "attention":"QA Contact",
                  "clientName":"Production Test Client",
                  "companyName":"Production Test Company",
                  "isCode":"IS 302",
                  "items":[{
                    "description":"Production Test Equipment",
                    "hsnCode":"9027",
                    "quantity":2,
                    "actualPrice":100.00,
                    "price":200.00
                  }]
                }
                """;

        mockMvc.perform(post("/api/lab-equipment/quotations").with(csrf())
                        .contentType("application/json").accept("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.invoiceNo", org.hamcrest.Matchers.matchesPattern("EVTL/TEST-EQ/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.message").value("Equipment quotation created successfully"));

        org.assertj.core.api.Assertions.assertThat(testingEquipmentRepository.count()).isEqualTo(equipmentCountBefore + 1);
        org.assertj.core.api.Assertions.assertThat(quotationTestingEquipmentRepository.count()).isEqualTo(quotationCountBefore + 1);
    }

    @Test
    @WithMockUser
    void postRequiresCsrfAndRejectsInvalidQuotationPayloads() throws Exception {
        String invalidRequest = "{\"date\":\"2026-09-11\",\"items\":[]}";
        mockMvc.perform(post("/api/lab-equipment/quotations")
                        .contentType("application/json").content(invalidRequest))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/lab-equipment/quotations").with(csrf())
                        .contentType("application/json").content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
