package controller;

import model.EmployeeApplicant;
import model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.servlet.http.HttpSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import service.EmployeeApplicantService;
import service.EmployeeApplicantPdfService;
import service.UserService;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;

@Controller
public class EmployeeApplicantController {

    private static final String DRAFT_APPLICANT_ID = "employeeApplicationDraftId";
    private final EmployeeApplicantService applicantService;
    private final EmployeeApplicantPdfService applicantPdfService;
    private final UserService userService;

    public EmployeeApplicantController(EmployeeApplicantService applicantService, EmployeeApplicantPdfService applicantPdfService, UserService userService) {
        this.applicantService = applicantService;
        this.applicantPdfService = applicantPdfService;
        this.userService = userService;
    }

    /** Same option IDs and labels as Laravel's qualifications() and education_boards() helpers. */
    @ModelAttribute("qualificationOptions")
    public Map<String, String> qualificationOptions() {
        return optionMap("""
                1|High School (10th)
                2|Intermediate (12th)
                3|Diploma (Engineering)
                4|Diploma (Other Fields)
                5|B.A
                6|B.Com
                7|B.Sc (General)
                8|B.Sc (Computer Science)
                9|B.Sc (IT)
                10|BBA
                11|BCA
                12|B.Tech (Computer Science)
                13|B.Tech (Information Technology)
                14|B.Tech (Electronics & Communication)
                15|B.Tech (Mechanical)
                16|B.Tech (Civil)
                17|B.Tech (Electrical)
                18|B.Tech (Other Branch)
                19|LLB (Bachelor of Law)
                20|MBBS
                21|BDS (Dental Surgery)
                22|B.Pharm
                23|B.Ed
                24|M.A
                25|M.Com
                26|M.Sc (General)
                27|M.Sc (Computer Science)
                28|M.Sc (IT)
                29|MBA
                30|MCA
                31|M.Tech (Computer Science)
                32|M.Tech (Information Technology)
                33|M.Tech (Electronics & Communication)
                34|M.Tech (Mechanical)
                35|M.Tech (Civil)
                36|M.Tech (Electrical)
                37|LLM (Master of Law)
                38|MD (Doctor of Medicine)
                39|MDS (Dental Surgery)
                40|M.Pharm
                41|M.Ed
                42|Ph.D
                43|D.Litt / D.Sc
                44|CA (Chartered Accountant)
                45|CS (Company Secretary)
                46|CFA
                47|ICWA
                48|Other Certification""");
    }

    @ModelAttribute("boardOptions")
    public Map<String, String> boardOptions() {
        return optionMap("""
                1|CBSE (Central Board of Secondary Education)
                2|ICSE (Indian Certificate of Secondary Education)
                3|NIOS (National Institute of Open Schooling)
                4|UP Board (Uttar Pradesh)
                5|Bihar Board
                6|Madhya Pradesh Board
                7|Rajasthan Board
                8|Maharashtra State Board
                9|Tamil Nadu State Board
                10|Kerala State Board
                11|Karnataka State Board
                12|Andhra Pradesh Board
                13|Telangana Board
                14|Gujarat Board
                15|Punjab School Education Board
                16|Haryana Board (HBSE)
                17|West Bengal Board (WBBSE / WBCHSE)
                18|Odisha Board
                19|Assam Board (SEBA / AHSEC)
                20|Jharkhand Academic Council
                21|Chhattisgarh Board (CGBSE)
                22|Goa Board
                23|Meghalaya Board (MBOSE)
                24|Mizoram Board
                25|Nagaland Board
                26|Manipur Board
                27|Tripura Board
                28|Sikkim Board
                29|Jammu & Kashmir Board (JKBOSE)
                30|Himachal Pradesh Board (HPBOSE)
                31|Uttarakhand Board (UBSE)
                32|Delhi (State Open Schools)
                33|Other International Board (IB, IGCSE, etc)""");
    }

    private static Map<String, String> optionMap(String entries) {
        Map<String, String> options = new LinkedHashMap<>();
        for (String entry : entries.strip().split("\\n")) {
            String[] parts = entry.trim().split("\\|", 2);
            options.put(parts[0], parts[1]);
        }
        return options;
    }

    @GetMapping({"/emp-app-form/index", "/emp-application-form/index"})
    public String index(Model model, Authentication authentication,
                        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "") String search,
                        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "25") String size,
                        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        requirePermission(authentication);

        // Fetch the complete, explicit database result first.  Pagination is
        // then applied in memory, avoiding a derived-query/page mismatch.
        List<EmployeeApplicant> matchingApplicants = applicantService.findAll();
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!normalizedSearch.isBlank()) {
            matchingApplicants = matchingApplicants.stream()
                    .filter(applicant -> contains(applicant.getName(), normalizedSearch)
                            || contains(applicant.getEmail(), normalizedSearch)
                            || contains(applicant.getPhone(), normalizedSearch))
                    .toList();
        }

        String normalizedSize = normalizePageSize(size);
        int totalApplicants = matchingApplicants.size();
        int rowsPerPage = "all".equals(normalizedSize) ? Math.max(totalApplicants, 1) : applicantService.pageSize(normalizedSize);
        int totalPages = totalApplicants == 0 ? 0 : Math.max(1, (int) Math.ceil((double) totalApplicants / rowsPerPage));
        int currentPage = totalPages == 0 ? 0 : Math.min(Math.max(page, 0), totalPages - 1);
        int showingFrom = totalApplicants == 0 ? 0 : currentPage * rowsPerPage + 1;
        int showingTo = totalApplicants == 0 ? 0 : Math.min(showingFrom + rowsPerPage - 1, totalApplicants);
        List<EmployeeApplicant> applicants = totalApplicants == 0
                ? List.of()
                : matchingApplicants.subList(showingFrom - 1, showingTo);

        model.addAttribute("activePage", "hr");
        model.addAttribute("applicants", applicants);
        model.addAttribute("totalApplicants", totalApplicants);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("showingFrom", showingFrom);
        model.addAttribute("showingTo", showingTo);
        return "employee/applicants/index";
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String normalizePageSize(String size) {
        return "50".equals(size) || "100".equals(size) || "all".equalsIgnoreCase(size) ? size : "25";
    }

    @GetMapping({"/emp-app-form/export", "/emp-application-form/export"})
    public ResponseEntity<byte[]> export(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "") String search,
                                         Authentication authentication) {
        requirePermission(authentication);
        List<EmployeeApplicant> applicants = applicantService.findForExport(search);
        StringBuilder csv = new StringBuilder("S. No,Name,Position,Email,Phone,Preferred Start Date,Application Date\n");
        for (int index = 0; index < applicants.size(); index++) {
            EmployeeApplicant applicant = applicants.get(index);
            csv.append(csvValue(index + 1)).append(',')
                    .append(csvValue(applicant.getName())).append(',')
                    .append(csvValue(applicant.getPosition())).append(',')
                    .append(csvValue(applicant.getEmail())).append(',')
                    .append(csvValue(applicant.getPhone())).append(',')
                    .append(csvValue(applicant.getStartDate())).append(',')
                    .append(csvValue(applicant.getApplicationDate())).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee-applications.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping({"/emp-app-form/create", "/emp-application-form/create"})
    public String create(Model model, Authentication authentication, HttpSession session) {
        model.addAttribute("publicApplication", true);
        Object draftId = session.getAttribute(DRAFT_APPLICANT_ID);

        EmployeeApplicant applicant = new EmployeeApplicant();
        if (draftId instanceof Long draftApplicantId) {
            try {
                applicant = applicantService.findById(draftApplicantId);
            } catch (IllegalArgumentException ex) {
                session.removeAttribute(DRAFT_APPLICANT_ID);
                applicant = new EmployeeApplicant();
            }
        }

        model.addAttribute("applicant", applicant);
        return "employee/applicants/form";
    }

    @GetMapping({"/emp-app-form/qr-code", "/emp-application-form/qr-code"})
    public String qrCode(Model model, Authentication authentication) {
        requirePermission(authentication);
        model.addAttribute("qrImage", qrImage(publicApplicationUrl()));
        model.addAttribute("applicationUrl", publicApplicationUrl());
        return "employee/applicants/qr-code";
    }

    @PostMapping({"/emp-app-form", "/emp-application-form"})
    public String store(@ModelAttribute("applicant") EmployeeApplicant applicant, Authentication authentication,
                        HttpSession session) {
        validateDraftOwnership(applicant, authentication, session);
        applicantService.save(applicant);
        session.removeAttribute(DRAFT_APPLICANT_ID);
        return "redirect:/emp-app-form/success";
    }

    @PostMapping({"/emp-app-form/autosave", "/emp-application-form/autosave"})
    @ResponseBody
    public Map<String, Object> autosave(@ModelAttribute("applicant") EmployeeApplicant applicant,
                                        Authentication authentication, HttpSession session) {
        Object sessionDraftId = session.getAttribute(DRAFT_APPLICANT_ID);
        if (applicant.getId() == null && sessionDraftId instanceof Long) {
            applicant.setId((Long) sessionDraftId);
        }
        validateDraftOwnership(applicant, authentication, session);
        EmployeeApplicant saved = applicantService.save(applicant);
        session.setAttribute(DRAFT_APPLICANT_ID, saved.getId());
        return Map.of("success", true, "id", saved.getId());
    }

    @GetMapping({"/emp-app-form/success", "/emp-application-form/success"})
    public String success() {
        return "employee/applicants/success";
    }

    @GetMapping({"/emp-app-form/{id}/edit", "/emp-application-form/{id}/edit"})
    public String edit(@PathVariable Long id, Model model, Authentication authentication) {
        requirePermission(authentication);
        model.addAttribute("activePage", "hr");
        model.addAttribute("applicant", applicantService.findById(id));
        return "employee/applicants/form";
    }

    @PostMapping({"/emp-app-form/{id}", "/emp-application-form/{id}"})
    public String update(@PathVariable Long id, @ModelAttribute("applicant") EmployeeApplicant applicant,
                         Authentication authentication) {
        requirePermission(authentication);
        applicant.setId(id);
        applicantService.save(applicant);
        return "redirect:/emp-app-form/index";
    }

    @GetMapping({"/emp-app-form/{id}/details", "/emp-application-form/{id}/details"})
    @ResponseBody
    public ResponseEntity<EmployeeApplicant> details(@PathVariable Long id, Authentication authentication) {
        requirePermission(authentication);
        return ResponseEntity.ok(applicantService.findById(id));
    }

    @GetMapping({"/emp-app-form/{id}/view", "/emp-application-form/{id}/view"})
    public String view(@PathVariable Long id, Model model, Authentication authentication) {
        requirePermission(authentication);
        model.addAttribute("activePage", "hr");
        model.addAttribute("applicant", applicantService.findById(id));
        return "employee/applicants/view";
    }

    @GetMapping({"/emp-app-form/{id}/pdf", "/emp-application-form/{id}/pdf"})
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Authentication authentication) {
        requirePermission(authentication);
        try {
            EmployeeApplicant applicant = applicantService.findById(id);
            String name = (applicant.getName() == null ? "Applicant" : applicant.getName()).replaceAll("[^a-zA-Z0-9._-]+", "-");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Employee-Application-" + name + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(applicantPdfService.generate(applicant));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping({"/emp-app-form/{id}/delete", "/emp-application-form/{id}/delete"})
    public String delete(@PathVariable Long id, Authentication authentication) {
        requirePermission(authentication);
        applicantService.delete(id);
        return "redirect:/emp-app-form/index";
    }

    @DeleteMapping({"/emp-app-form/{id}", "/emp-application-form/{id}"})
    @ResponseBody
    public Map<String, Object> deleteAjax(@PathVariable Long id, Authentication authentication) {
        requirePermission(authentication);
        applicantService.delete(id);
        return Map.of("success", true, "message", "Employee application deleted successfully");
    }

    @PostMapping({"/emp-app-form/delete-old-data", "/emp-application-form/delete-old-data"})
    public String deleteOldData(Authentication authentication) {
        requirePermission(authentication);
        applicantService.deleteOlderThanDays(45);
        return "redirect:/emp-app-form/index";
    }

    @PostMapping({"/emp-app-form/delete-selected", "/emp-application-form/delete-selected"})
    public String deleteSelected(@org.springframework.web.bind.annotation.RequestParam List<Long> ids,
                                 Authentication authentication) {
        requirePermission(authentication);
        applicantService.deleteAll(ids);
        return "redirect:/emp-app-form/index";
    }

    private void requirePermission(Authentication authentication) {
        if (authentication == null || !hasPermission(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access employee applications");
        }
    }

    private void validateDraftOwnership(EmployeeApplicant applicant, Authentication authentication, HttpSession session) {
        if (applicant.getId() == null) {
            return;
        }
        Object sessionDraftId = session.getAttribute(DRAFT_APPLICANT_ID);
        boolean ownsDraft = sessionDraftId instanceof Long && applicant.getId().equals(sessionDraftId);
        if (!ownsDraft && (authentication == null || !hasPermission(authentication))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this application");
        }
    }

    private boolean hasPermission(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return true;
        }
        User user = userService.getUserByEmail(authentication.getName());
        if (user == null || user.getPermissions() == null) {
            return false;
        }
        return user.getPermissions().contains("emp_applicant");
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String publicApplicationUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/emp-app-form/create")
            .toUriString();
    }

    private String qrImage(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320);
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return java.util.Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | java.io.IOException exception) {
            throw new IllegalStateException("Unable to generate QR code", exception);
        }
    }
}
