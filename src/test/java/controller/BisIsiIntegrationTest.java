package controller;

import com.evtl.crm.EvtlCrmApplication;
import model.BisIsiOperation;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import repository.BisPreInspectionRepository;
import repository.IsiChecklistRepository;
import repository.UserRepository;
import repository.BisIsiOperationRepository;
import repository.BisIsiAmcQuotationRepository;
import repository.ProjectStatusRepository;
import service.BisIsiService;
import model.ProjectStatus;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EvtlCrmApplication.class)
@AutoConfigureMockMvc
@Transactional
class BisIsiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BisIsiService service;
    @Autowired BisPreInspectionRepository preInspections;
    @Autowired IsiChecklistRepository checklists;
    @Autowired BisIsiOperationRepository operations;
    @Autowired BisIsiAmcQuotationRepository amcQuotations;
    @Autowired ProjectStatusRepository projectStatuses;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    private User admin;

    @BeforeEach
    void createAdmin() {
        admin = new User();
        admin.setRoleId(1); admin.setTypeId(1L); admin.setName("BIS Admin");
        admin.setEmail("bis.admin@evtl.test"); admin.setPassword("test"); admin.setStatus(1);
        admin = users.save(admin);
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void rendersTheBisIsiList() throws Exception {
        mvc.perform(get("/operation/bis-isi"))
                .andExpect(status().isOk())
                .andExpect(view().name("operation/bis-isi/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BIS-ISI Operations")));
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void creatorFilterDrivesBothMonthlyChartAndBreakdown() throws Exception {
        User divyanshu = new User();
        divyanshu.setRoleId(2); divyanshu.setTypeId(1L); divyanshu.setName("Divyanshu");
        divyanshu.setEmail("divyanshu.analytics@evtl.test"); divyanshu.setPassword("test");
        divyanshu.setStatus(1); divyanshu.setPermissions("operation");
        divyanshu = users.save(divyanshu);

        User vartika = new User();
        vartika.setRoleId(2); vartika.setTypeId(1L); vartika.setName("Vartika");
        vartika.setEmail("vartika.analytics@evtl.test"); vartika.setPassword("test");
        vartika.setStatus(1); vartika.setPermissions("operation");
        vartika = users.save(vartika);

        BisIsiOperation divyanshuDrafting = operation(
                "Divyanshu Drafting Industries",
                "IS DIV 1",
                "Drafting"
        );
        divyanshuDrafting.setOperationDate(java.time.LocalDate.of(2026, 4, 5));
        service.save(divyanshu, divyanshuDrafting, null);

        BisIsiOperation divyanshuLicence = operation(
                "Divyanshu Licence Industries",
                "IS DIV 2",
                "License Granted"
        );
        divyanshuLicence.setOperationDate(java.time.LocalDate.of(2026, 4, 18));
        service.save(divyanshu, divyanshuLicence, null);

        BisIsiOperation vartikaOperation = operation(
                "Vartika Industries",
                "IS VAR 1",
                "On Hold"
        );
        vartikaOperation.setOperationDate(java.time.LocalDate.of(2026, 4, 20));
        service.save(vartika, vartikaOperation, null);

        var response = mvc.perform(get("/operation/bis-isi")
                        .param("creator", divyanshu.getId().toString())
                        .param("analyticsYear", "2026")
                        .param("breakdownMonth", "4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("creatorFilter", divyanshu.getId()))
                .andReturn();

        BisIsiService.MonthlyAnalytics analytics =
                (BisIsiService.MonthlyAnalytics) response
                        .getModelAndView()
                        .getModel()
                        .get("monthlyAnalytics");
        BisIsiService.MonthlyAnalyticsPoint april = analytics.months().get(3);

        assertThat(april.total()).isEqualTo(2);
        assertThat(april.licenceGranted()).isEqualTo(1);
        assertThat(april.breakdown())
                .extracting(BisIsiService.ProcessBreakdown::label)
                .containsExactlyInAnyOrder("Drafting", "License Granted")
                .doesNotContain("On Hold");
        assertThat(april.breakdown())
                .extracting(BisIsiService.ProcessBreakdown::count)
                .containsOnly(1L);
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void amcPageShowsOnlyLicensedOperationsAndSearchesThem() throws Exception {
        service.save(admin, operation("AMC Licensed Industries", "IS AMC 1", "License Granted"), null);
        service.save(admin, operation("Ordinary Industries", "IS ORD 1", "Registration"), null);

        mvc.perform(get("/operation/bis-isi-amc"))
                .andExpect(status().isOk())
                .andExpect(view().name("operation/bis-isi-amc/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AMC Licensed Industries")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Ordinary Industries"))));

        mvc.perform(get("/operation/bis-isi-amc").param("search", "no match"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No licensed operations found")));
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void amcPaginationPreservesSearchAndLimitsSuggestionsToLicensedOperations() throws Exception {
        for (int number = 1; number <= 12; number++)
            service.save(admin, operation("Paged AMC " + number, "IS PAGE " + number, "License Granted"), null);
        service.save(admin, operation("Hidden AMC Search", "IS HIDDEN SEARCH", "Registration"), null);

        mvc.perform(get("/operation/bis-isi-amc").param("size", "10").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("operations"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Showing 11 to 12 of 12 entries")));
        mvc.perform(get("/operation/bis-isi-amc").param("size", "10").param("page", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Showing 11 to 12 of 12 entries")));
        mvc.perform(get("/operation/bis-isi-amc/suggestions").param("query", "Paged AMC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
        mvc.perform(get("/operation/bis-isi-amc/suggestions").param("query", "Hidden AMC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void exportsOnlyLicensedAmcOperations() throws Exception {
        service.save(admin, operation("Exported AMC Industries", "IS AMC EX", "License Granted"), null);
        service.save(admin, operation("Excluded Ordinary Industries", "IS ORD EX", "Registration"), null);

        byte[] bytes = mvc.perform(get("/operation/bis-isi-amc/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("Exported AMC Industries");
        }
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void createsAndEditsAnAmcQuotationForLicensedOperation() throws Exception {
        BisIsiOperation operation = service.save(admin, operation("Quoted AMC Industries", "IS AMC Q", "License Granted"), null);

        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation", operation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editing").value(false))
                .andExpect(jsonPath("$.isStandard").value("IS AMC Q"));

        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", operation.getId()).with(csrf())
                        .param("proposalDate", "2026-09-19")
                        .param("kindAttention", "Purchasing Team")
                        .param("isStandard", "IS AMC Q")
                        .param("product", "Electrical Equipment")
                        .param("cmlNumber", "CML 12345")
                        .param("licenceValidityDate", "2027-09-19"))
                .andExpect(status().is3xxRedirection());

        var created = amcQuotations.findByOperationId(operation.getId()).orElseThrow();
        assertThat(created.getReferenceNumber()).matches("EVTL/2026-27/SIT/AMC/\\d{3,}/R1");
        assertThat(created.getProduct()).isEqualTo("Electrical Equipment");

        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", operation.getId()).with(csrf())
                        .param("proposalDate", "2026-09-20")
                        .param("product", "Updated Equipment"))
                .andExpect(status().is3xxRedirection());

        assertThat(amcQuotations.count()).isEqualTo(1);
        assertThat(amcQuotations.findByOperationId(operation.getId()).orElseThrow().getProduct()).isEqualTo("Updated Equipment");
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void downloadsSavedAmcQuotationAsTwoPagePdf() throws Exception {
        BisIsiOperation operation = service.save(admin, operation("PDF AMC Industries", "IS PDF 123", "License Granted"), null);
        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation/download", operation.getId()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", operation.getId()).with(csrf())
                        .param("proposalDate", "2026-09-19")
                        .param("kindAttention", "Procurement Department")
                        .param("isStandard", "IS PDF 123")
                        .param("product", "Industrial Fan")
                        .param("cmlNumber", "CML 9876")
                        .param("licenceValidityDate", "2027-09-19")
                        .param("consultancyServiceFee", "25000")
                        .param("consultancyOneYear", "75000"))
                .andExpect(status().is3xxRedirection());

        var quote = amcQuotations.findByOperationId(operation.getId()).orElseThrow();
        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation/download", operation.getId())
                        .param("quotation_id", String.valueOf(quote.getId() + 1)))
                .andExpect(status().isNotFound());

        byte[] bytes = mvc.perform(get("/operation/bis-isi-amc/{id}/quotation/download", operation.getId())
                        .param("quotation_id", String.valueOf(quote.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("AMC-SIT-Quotation-" + operation.getId())))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        try (var document = org.apache.pdfbox.pdmodel.PDDocument.load(bytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertThat(text).contains(quote.getReferenceNumber(), "Industrial Fan", "CML 9876", "25000 INR/-", "75000 INR/-");
        }
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void preservesAndSelectsAmcQuotationRevisionsForDownload() throws Exception {
        BisIsiOperation operation = service.save(admin, operation("Revised AMC Industries", "IS REV 1", "License Granted"), null);
        long id = operation.getId();
        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", id).with(csrf())
                        .param("proposalDate", "2026-09-19").param("product", "R1 Product"))
                .andExpect(status().is3xxRedirection());
        String r1Reference = amcQuotations.findByOperationId(id).orElseThrow().getReferenceNumber();
        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation", id).param("revision", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editing").value(false))
                .andExpect(jsonPath("$.revisionNumber").value(2))
                .andExpect(jsonPath("$.product").value(""));

        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", id).with(csrf())
                        .param("revisionNumber", "2").param("newRevision", "true")
                        .param("proposalDate", "2026-09-20").param("product", "R2 Product"))
                .andExpect(status().is3xxRedirection());
        assertThat(amcQuotations.findByOperationId(id).orElseThrow().getReferenceNumber()).endsWith("/R2");
        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", id).with(csrf())
                        .param("revisionNumber", "2").param("newRevision", "true")
                        .param("proposalDate", "2026-09-21").param("product", "Stale Draft"))
                .andExpect(status().is3xxRedirection());
        assertThat(amcQuotations.findByOperationId(id).orElseThrow().getProduct()).isEqualTo("R2 Product");
        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation", id).param("revision", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisions.length()").value(2))
                .andExpect(jsonPath("$.product").value("R1 Product"))
                .andExpect(jsonPath("$.referenceNumber").value(r1Reference));
        byte[] r2Pdf = mvc.perform(get("/operation/bis-isi-amc/{id}/quotation/download", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var document = org.apache.pdfbox.pdmodel.PDDocument.load(r2Pdf)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document)).contains("R2 Product", "/R2").doesNotContain("R1 Product");
        }

        mvc.perform(post("/operation/bis-isi-amc/{id}/quotation", id).with(csrf())
                        .param("revisionNumber", "1").param("proposalDate", "2026-09-19")
                        .param("product", "R1 Product"))
                .andExpect(status().is3xxRedirection());
        assertThat(amcQuotations.count()).isEqualTo(1);
        assertThat(amcQuotations.findByOperationId(id).orElseThrow().getReferenceNumber()).isEqualTo(r1Reference);
        byte[] r1Pdf = mvc.perform(get("/operation/bis-isi-amc/{id}/quotation/download", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var document = org.apache.pdfbox.pdmodel.PDDocument.load(r1Pdf)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document)).contains("R1 Product", "/R1").doesNotContain("R2 Product");
        }
        mvc.perform(get("/operation/bis-isi-amc/{id}/quotation", id).param("revision", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.product").value("R2 Product"));
    }

    @Test
    void legacyLicensedOperationAppearsOnlyInAmc() {
        ProjectStatus legacyStatus = new ProjectStatus();
        legacyStatus.setId(916L); legacyStatus.setProjectStatus("License Granted");
        projectStatuses.save(legacyStatus);
        BisIsiOperation legacy = service.save(admin, operation("Legacy AMC Industries", "IS AMC 916", "Registration"), null);
        entityManager.flush();
        jdbc.update("UPDATE operations SET project_status = NULL, project_fk_id = ? WHERE id = ?", "916", legacy.getId());
        entityManager.clear();

        assertThat(service.listAmc(admin, null, 25, 0).getContent()).extracting(BisIsiOperation::getId).contains(legacy.getId());
        assertThat(service.list(admin, null, null, null, null, null, null, null, null, 0).getContent())
                .extracting(BisIsiOperation::getId).doesNotContain(legacy.getId());
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void updatesOnlyTheSelectedOperationsProjectStatus() throws Exception {
        BisIsiOperation target = service.save(admin, operation("Status Target Industries", "IS ST 1", "Registration"), null);
        BisIsiOperation other = service.save(admin, operation("Status Other Industries", "IS ST 2", "Registration"), null);

        mvc.perform(post("/operation/bis-isi/{id}/status", target.getId()).with(csrf())
                        .param("projectStatus", "Closure Notice Issued"))
                .andExpect(status().isNoContent());

        assertThat(operations.findById(target.getId()).orElseThrow().getProjectStatus()).isEqualTo("Closure Notice Issued");
        assertThat(operations.findById(other.getId()).orElseThrow().getProjectStatus()).isEqualTo("Registration");
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void rejectsInvalidProjectStatusWithoutChangingTheOperation() throws Exception {
        BisIsiOperation target = service.save(admin, operation("Status Validation Industries", "IS ST 3", "Registration"), null);

        mvc.perform(post("/operation/bis-isi/{id}/status", target.getId()).with(csrf())
                        .param("projectStatus", "Not a valid status"))
                .andExpect(status().isBadRequest());

        assertThat(operations.findById(target.getId()).orElseThrow().getProjectStatus()).isEqualTo("Registration");
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void downloadsTheSimplifiedFormat() throws Exception {
        mvc.perform(get("/operation/bis-isi/simplified/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("simplified.docx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void rendersTheRequestedBisIsiCreateFields() throws Exception {
        mvc.perform(get("/operation/bis-isi/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("operation/bis-isi/form"))
                .andExpect(model().attribute("operation", org.hamcrest.Matchers.hasProperty("operationDate", org.hamcrest.Matchers.is(java.time.LocalDate.now()))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"" + java.time.LocalDate.now() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alternate Contact Number")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Extra Mail ID by EVTL")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Target Date of Final Exception")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Operating Person")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Extra Service")));
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void suggestsMatchingVisibleOperationsWhileTyping() throws Exception {
        service.save(admin, operation("Suggestion Industries", "IS SUG 123", "Registration"), null);

        mvc.perform(get("/operation/bis-isi/suggestions").param("query", "Sugg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Suggestion Industries"));
    }

    @Test
    void statusCardsCountLegacyProjectReferencesAndCurrentTextStatuses() {
        ProjectStatus legacyStatus = new ProjectStatus();
        legacyStatus.setId(16L); legacyStatus.setProjectStatus("License Granted");
        projectStatuses.save(legacyStatus);
        BisIsiOperation legacy = service.save(admin, operation("Legacy Licence Industries", "IS LEG 16", "Registration"), null);
        service.save(admin, operation("Current Registration Industries", "IS CUR 1", "Registration"), null);
        entityManager.flush();
        jdbc.update("UPDATE operations SET project_status = NULL, project_fk_id = ? WHERE id = ?", "16", legacy.getId());
        entityManager.clear();

        var cards = service.statusCards(admin);
        assertThat(cards.stream().filter(card -> card.label().equals("License Granted")).findFirst().orElseThrow().count()).isEqualTo(1);
        assertThat(cards.stream().filter(card -> card.label().equals("Registration")).findFirst().orElseThrow().count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "bis.admin@evtl.test")
    void bulkDeleteRemovesOnlySelectedOperations() throws Exception {
        BisIsiOperation first = service.save(admin, operation("Delete First Industries", "IS DEL 1", "Registration"), null);
        BisIsiOperation second = service.save(admin, operation("Delete Second Industries", "IS DEL 2", "Registration"), null);
        BisIsiOperation untouched = service.save(admin, operation("Keep Industries", "IS KEEP 1", "Registration"), null);

        mvc.perform(post("/operation/bis-isi/bulk-delete").with(csrf())
                        .param("selectedIds", first.getId().toString(), second.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/operation/bis-isi"));

        assertThat(operations.findById(first.getId())).isEmpty();
        assertThat(operations.findById(second.getId())).isEmpty();
        assertThat(operations.findById(untouched.getId())).isPresent();
    }

    @Test
    void bulkDeleteRejectsUnknownIdsWithoutDeletingValidSelection() {
        BisIsiOperation saved = service.save(admin, operation("Safe Industries", "IS SAFE 1", "Registration"), null);

        assertThatThrownBy(() -> service.bulkDelete(admin, java.util.List.of(saved.getId(), Long.MAX_VALUE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(operations.findById(saved.getId())).isPresent();
    }

    @Test
    void bulkDeleteIsAdminOnly() {
        BisIsiOperation saved = service.save(admin, operation("Protected Industries", "IS PROT 1", "Registration"), null);
        User employee = new User();
        employee.setRoleId(2); employee.setTypeId(1L); employee.setName("Protected Employee");
        employee.setEmail("protected.employee@evtl.test"); employee.setPassword("test"); employee.setStatus(1);
        employee.setPermissions("operation"); employee = users.save(employee);
        User nonAdmin = employee;

        assertThatThrownBy(() -> service.bulkDelete(nonAdmin, java.util.List.of(saved.getId())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(operations.findById(saved.getId())).isPresent();
    }

    @Test
    void createsOperationInOperationsTableWithoutWritingSecondaryTables() {
        BisIsiOperation operation = new BisIsiOperation();
        operation.setCompanyName("Precision Industries"); operation.setClientName("Test Client");
        operation.setIndianStandard("IS 1234"); operation.setContactNumber("9999999999");
        operation.setOperationDate(java.time.LocalDate.now()); operation.setEmail("client@example.com");
        operation.setTestingStatus("Pending"); operation.setTestingPerson("Test Engineer");
        operation.setStateId(1L); operation.setProjectStatus("Registration"); operation.setProcedure("Simplified");
        operation.setPaymentStatus("1st Installment"); operation.setAdvancePaymentStatus("Pending");
        operation.setAddress("A deliberately long operation address that belongs in the operations table and must not be constrained by a secondary checklist location column.");

        BisIsiOperation saved = service.save(admin, operation, null);

        assertThat(saved.getTargetDate()).isNotNull();
        assertThat(saved.getFinalDate()).isEqualTo(saved.getTargetDate().plusDays(45));
        assertThat(saved.getAssignedEngineerId()).isNull();
        assertThat(operations.findById(saved.getId())).isPresent();
        assertThat(preInspections.findByOperationId(saved.getId())).isEmpty();
        assertThat(checklists.findByOperationId(saved.getId())).isEmpty();
    }

    @Test
    void bulkAssignmentStoresTheSelectedUsersId() {
        User assignee = new User();
        assignee.setRoleId(2); assignee.setTypeId(1L); assignee.setName("Prerna Pandey");
        assignee.setEmail("prerna.assignment@evtl.test"); assignee.setPassword("test"); assignee.setStatus(1);
        assignee = users.save(assignee);

        BisIsiOperation operation = new BisIsiOperation();
        operation.setCompanyName("Assignment Industries"); operation.setClientName("Assignment Client");
        operation.setIndianStandard("IS 9876"); operation.setContactNumber("8888888888");
        operation.setOperationDate(java.time.LocalDate.now()); operation.setEmail("assignment@example.com");
        operation.setTestingStatus("Pending"); operation.setTestingPerson("Test Engineer");
        operation.setStateId(1L); operation.setProjectStatus("Registration"); operation.setProcedure("Simplified");
        operation.setPaymentStatus("1st Installment"); operation.setAdvancePaymentStatus("Pending");
        BisIsiOperation saved = service.save(admin, operation, null);

        service.bulkAssign(admin, java.util.List.of(saved.getId()), assignee.getId());

        assertThat(operations.findById(saved.getId()).orElseThrow().getAssignedEngineerId()).isEqualTo(assignee.getId());
    }

    @Test
    void operationsTeamOnlySeesOwnRecordsOutsideRestrictedStatuses() {
        User dev = new User();
        dev.setRoleId(2); dev.setTypeId(1L); dev.setName("Dev"); dev.setEmail("dev.visibility@evtl.test");
        dev.setPassword("test"); dev.setStatus(1); dev.setPermissions("operation"); dev = users.save(dev);
        final User devUser = dev;

        BisIsiOperation ownVisible = operation("Dev Visible Industries", "IS DEV 1", "Registration");
        ownVisible = service.save(devUser, ownVisible, null);
        BisIsiOperation ownHidden = service.save(devUser, operation("Dev Hidden Industries", "IS DEV 2", "Application Under Process"), null);
        BisIsiOperation assignedFromAdmin = service.save(admin, operation("Admin Assigned Industries", "IS ADMIN 1", "Registration"), null);
        service.bulkAssign(admin, java.util.List.of(assignedFromAdmin.getId()), dev.getId());

        var visible = service.list(devUser, null, null, null, null, null, null, null, null, 0);

        assertThat(visible.getContent()).extracting(BisIsiOperation::getId).containsExactly(ownVisible.getId());
        assertThat(service.dashboard(devUser).total()).isEqualTo(1);
        assertThatThrownBy(() -> service.get(devUser, ownHidden.getId())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.get(devUser, assignedFromAdmin.getId())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void divyanshuOnlySeesApplicationWorkAndLicenceGrantedRoutesToAmc() {
        User divyanshu = new User();
        divyanshu.setRoleId(2); divyanshu.setTypeId(1L); divyanshu.setName("Divyanshu");
        divyanshu.setEmail("divyanshu.visibility@evtl.test"); divyanshu.setPassword("test");
        divyanshu.setStatus(1); divyanshu.setPermissions("operation"); divyanshu = users.save(divyanshu);
        final User applicationUser = divyanshu;

        BisIsiOperation application = service.save(admin, operation("Application Queue Industries", "IS APP 1", "Application Under Process"), null);
        BisIsiOperation registration = service.save(admin, operation("Registration Queue Industries", "IS REG 1", "Registration"), null);
        BisIsiOperation licence = service.save(admin, operation("AMC Queue Industries", "IS AMC 1", "License Granted"), null);

        var divyanshuView = service.list(applicationUser, null, null, null, null, null, null, null, null, 0);
        var mainAdminView = service.list(admin, null, null, null, null, null, null, null, null, 0);
        var amcAdminView = service.listAmc(admin, 0);

        assertThat(divyanshuView.getContent()).extracting(BisIsiOperation::getId).containsExactly(application.getId());
        assertThatThrownBy(() -> service.get(applicationUser, registration.getId())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(mainAdminView.getContent()).extracting(BisIsiOperation::getId).doesNotContain(licence.getId());
        assertThat(amcAdminView.getContent()).extracting(BisIsiOperation::getId).contains(licence.getId());
        assertThat(service.statusCards(admin).stream().filter(card -> card.label().equals("License Granted")).findFirst().orElseThrow().count()).isEqualTo(1);
        assertThat(service.statusCards(applicationUser).stream().filter(card -> card.label().equals("License Granted")).findFirst().orElseThrow().count()).isZero();
    }

    private BisIsiOperation operation(String company,String standard,String status) {
        BisIsiOperation operation = new BisIsiOperation();
        operation.setCompanyName(company); operation.setClientName("Test Client"); operation.setIndianStandard(standard);
        operation.setContactNumber("9999999999"); operation.setOperationDate(java.time.LocalDate.now());
        operation.setEmail("client@example.com"); operation.setTestingStatus("Pending"); operation.setTestingPerson("Test Engineer");
        operation.setStateId(1L); operation.setProjectStatus(status); operation.setProcedure("Simplified");
        operation.setPaymentStatus("1st Installment"); operation.setAdvancePaymentStatus("Pending");
        return operation;
    }
}
