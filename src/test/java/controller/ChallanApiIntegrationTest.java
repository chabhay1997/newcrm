package controller;

import com.evtl.crm.EvtlCrmApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import repository.ChallanRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EvtlCrmApplication.class)
@AutoConfigureMockMvc
class ChallanApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ChallanRepository challanRepository;

    @Test
    void rejectsUnauthenticatedRequestsWithoutRedirectingToHtml() throws Exception {
        mockMvc.perform(get("/api/challans").accept("application/json"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @WithMockUser(username = "api-test@evtl.in")
    void returnsAuthenticatedJsonWithSecureCachingAndExpectedContract() throws Exception {
        long databaseCount = challanRepository.count();
        assertThat(databaseCount).isGreaterThan(25);
        mockMvc.perform(get("/api/challans").param("page", "1").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.records.length()").value(25))
                .andExpect(jsonPath("$.records[0].challanNo").value("Evtl/DC/2024-25/7"))
                .andExpect(jsonPath("$.records[0].amount").value("1473.00"))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(25))
                .andExpect(jsonPath("$.totalRecords").value(databaseCount))
                .andExpect(jsonPath("$.hasPrevious").isBoolean())
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    @WithMockUser
    void rejectsInvalidAndResourceIntensiveParameters() throws Exception {
        mockMvc.perform(get("/api/challans").param("page", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/challans").param("page", "10001"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/challans").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/challans").param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void treatsInjectionLikeInputAsDataAndDoesNotExposeRecords() throws Exception {
        mockMvc.perform(get("/api/challans").param("q", "' OR 1=1 --").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(0))
                .andExpect(jsonPath("$.records").isEmpty());
    }

    @Test
    @WithMockUser
    void editApiRejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(put("/api/challans/1").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(username = "edit-test@evtl.in")
    @Transactional
    void rendersAndUpdatesAnExistingChallanWithoutChangingItsIdentity() throws Exception {
        var before = challanRepository.findById(7L).orElseThrow();
        String originalNumber = before.getChallanNo();
        Long originalCreator = before.getCreatedBy();

        mockMvc.perform(get("/challan/7/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("challan/create"))
                .andExpect(model().attribute("editMode", true))
                .andExpect(model().attribute("challanId", 7L))
                .andExpect(model().attributeExists("challanForm", "states"));

        var update = multipart("/api/challans/7").with(csrf())
                .param("challanNo", "SPOOFED-NUMBER")
                .param("clientName", "Edited Client")
                .param("itemName", "Edited Item")
                .param("qty", "3")
                .param("amount", "500")
                .param("gst", "18")
                .param("date", "2026-09-11")
                .accept("application/json");
        update.with(request -> { request.setMethod("PUT"); return request; });
        mockMvc.perform(update)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.challan.id").value(7))
                .andExpect(jsonPath("$.challan.challanNo").value(originalNumber))
                .andExpect(jsonPath("$.challan.clientName").value("Edited Client"))
                .andExpect(jsonPath("$.message").value("Challan updated successfully"));

        var updated = challanRepository.findById(7L).orElseThrow();
        assertThat(updated.getChallanNo()).isEqualTo(originalNumber);
        assertThat(updated.getCreatedBy()).isEqualTo(originalCreator);
        assertThat(updated.getClientName()).isEqualTo("Edited Client");
        assertThat(updated.getTotalAmount()).isEqualTo("590.00");
    }

    @Test
    @WithMockUser
    void editApiRequiresCsrfAndRejectsMissingRecords() throws Exception {
        var withoutCsrf = multipart("/api/challans/7").param("clientName", "Client");
        withoutCsrf.with(request -> { request.setMethod("PUT"); return request; });
        mockMvc.perform(withoutCsrf).andExpect(status().isForbidden());

        var missing = multipart("/api/challans/999999999").with(csrf())
                .param("clientName", "Client").param("itemName", "Item").param("date", "2026-09-11");
        missing.with(request -> { request.setMethod("PUT"); return request; });
        mockMvc.perform(missing).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "api-create-test@evtl.in")
    @Transactional
    void createsARealDatabaseRecordAndReturnsItsDashboardLocation() throws Exception {
        long countBefore = challanRepository.count();
        mockMvc.perform(multipart("/api/challans").with(csrf())
                        .param("clientName", "Production API Test Client")
                        .param("itemName", "Production API Test Item")
                        .param("qty", "2")
                        .param("amount", "1000.00")
                        .param("gst", "18")
                        .param("date", "2026-09-11")
                        .accept("application/json"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/challans/[0-9]+")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.challan.id").isNumber())
                .andExpect(jsonPath("$.challan.challanNo", org.hamcrest.Matchers.matchesPattern("Evtl/DC/2026-27/[0-9]+")))
                .andExpect(jsonPath("$.challan.clientName").value("Production API Test Client"))
                .andExpect(jsonPath("$.challan.amount").value("1000.00"))
                .andExpect(jsonPath("$.dashboardPage").isNumber())
                .andExpect(jsonPath("$.message").value("Challan created successfully"));
        assertThat(challanRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @WithMockUser
    void postApiRequiresCsrfAndRejectsInvalidOrSpoofedInput() throws Exception {
        long countBefore = challanRepository.count();
        mockMvc.perform(multipart("/api/challans")
                        .param("clientName", "Client").param("itemName", "Item").param("date", "2026-09-11"))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/challans").with(csrf())
                        .param("clientName", "").param("itemName", "").param("date", "2026-09-11"))
                .andExpect(status().isBadRequest());
        MockMultipartFile spoofedPng = new MockMultipartFile("uploads", "malicious.png", "image/png", "not-a-png".getBytes());
        mockMvc.perform(multipart("/api/challans").file(spoofedPng).with(csrf())
                        .param("clientName", "Client").param("itemName", "Item").param("date", "2026-09-11"))
                .andExpect(status().isBadRequest());
        assertThat(challanRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser
    void deleteRequiresCsrfAndReturnsNotFoundWithoutChangingData() throws Exception {
        long countBefore = challanRepository.count();
        mockMvc.perform(delete("/api/challans/999999999"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/challans/999999999").with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(challanRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser
    void downloadsAnExistingChallanAsAnAttachmentPdf() throws Exception {
        var result = mockMvc.perform(get("/api/challans/7/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Evtl_DC_2024-25_7.pdf")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn();
        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("DELIVERY CHALLAN", "EMPHATIC VANS & TESTING LABS", "Evtl/DC/2024-25/7",
                    "ACE TEST LAB", "CHIMNEY BIS SEALED SAMPLE", "Address:-", "PinCode:-", "Total Amount (In Words)",
                    "Terms and Conditions", "Authorized Signatory For");
            assertThat(text).contains("1473.00");
        }
    }

    @Test
    @WithMockUser
    void exportsEveryChallanAsAValidExcelWorkbook() throws Exception {
        long databaseCount = challanRepository.count();
        var result = mockMvc.perform(get("/api/challans/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.matchesPattern(
                        "attachment; filename=\\\"challans-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.xlsx\\\"")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheet("Challans");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(databaseCount);
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Challan No.");
            assertThat(sheet.getRow(0).getCell(18).getStringCellValue()).isEqualTo("Created At");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Evtl/DC/2024-25/7");
        }
    }

    @Test
    @WithMockUser(username = "form-test@evtl.in")
    void rendersCreatePageAndRejectsInvalidSubmissionWithoutChangingData() throws Exception {
        long countBefore = challanRepository.count();
        mockMvc.perform(get("/challan/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("challan/create"))
                .andExpect(model().attributeExists("challanForm", "states"));
        mockMvc.perform(multipart("/challan/create").with(csrf())
                        .param("clientName", "")
                        .param("itemName", "")
                        .param("date", "2026-09-11"))
                .andExpect(status().isOk())
                .andExpect(view().name("challan/create"))
                .andExpect(model().attributeExists("errorMessage"));
        assertThat(challanRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @WithMockUser
    void createSubmissionRequiresCsrf() throws Exception {
        mockMvc.perform(multipart("/challan/create")
                        .param("clientName", "Client")
                        .param("itemName", "Item")
                        .param("date", "2026-09-11"))
                .andExpect(status().isForbidden());
    }

    @Test
    void handlesConcurrentAuthenticatedReadRequests() throws Exception {
        int requestCount = 100;
        try (var executor = Executors.newFixedThreadPool(10)) {
            List<Callable<Integer>> requests = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                int page = (index % 2) + 1;
                requests.add(() -> mockMvc.perform(get("/api/challans")
                                .with(user("load-test@evtl.in"))
                                .param("page", String.valueOf(page))
                                .accept("application/json"))
                        .andReturn().getResponse().getStatus());
            }

            var futures = executor.invokeAll(requests, 15, TimeUnit.SECONDS);
            assertThat(futures).allSatisfy(future -> {
                assertThat(future.isCancelled()).isFalse();
                assertThat(future.get()).isEqualTo(200);
            });
        }
    }
}
