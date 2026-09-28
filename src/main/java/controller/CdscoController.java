package controller;

import model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import service.CdscoExcelService;
import service.OperationAccessService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;

@Controller
public class CdscoController {
    private final OperationAccessService access;
    private final JdbcTemplate jdbc;
    private final CdscoExcelService excel;

    public CdscoController(OperationAccessService access, JdbcTemplate jdbc, CdscoExcelService excel) {
        this.access = access;
        this.jdbc = jdbc;
        this.excel = excel;
    }

    @PostMapping("/operation/cdsco/import")
    public String importExcel(Authentication authentication, @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            CdscoExcelService.ImportResult result = excel.importFile(access.require(authentication), file);
            String message = result.imported() + " CDSCO record" + (result.imported() == 1 ? "" : "s") + " imported successfully.";
            if (result.duplicates() > 0) message += " " + result.duplicates() + " duplicate row" + (result.duplicates() == 1 ? " was" : "s were") + " ignored.";
            redirectAttributes.addFlashAttribute("success", message);
            if (result.invalid() > 0) redirectAttributes.addFlashAttribute("error", result.invalid() + " invalid row" + (result.invalid() == 1 ? " was" : "s were") + " skipped. " + String.join(" | ", result.errors()));
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/operation/cdsco";
    }

    @GetMapping(value = "/operation/cdsco/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate) {
        access.require(authentication);
        String term = clean(search).toLowerCase(Locale.ROOT);
        List<Map<String,Object>> records = jdbc.queryForList("select * from cdscos").stream()
                .map(this::normalise).filter(row -> matches(row, term, status, startDate, endDate)).toList();
        byte[] spreadsheet = excel.export(records);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cdsco-" + java.time.LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(spreadsheet.length).body(spreadsheet);
    }

    @GetMapping("/operation/cdsco")
    public String index(Authentication authentication, Model model,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "0") int page) {
        User user = access.require(authentication);
        int selectedSize = List.of(10, 25, 50, 100).contains(size) ? size : 25;
        String term = clean(search).toLowerCase(Locale.ROOT);
        List<Map<String,Object>> allRecords = jdbc.queryForList("select * from cdscos").stream()
                .map(this::normalise)
                .sorted(Comparator.comparing((Map<String,Object> row) -> text(row.get("date"))).reversed())
                .toList();
        Map<String,Long> statusCounts = allRecords.stream().collect(Collectors.groupingBy(
                row -> text(row.get("status")), LinkedHashMap::new, Collectors.counting()));
        List<Map<String,Object>> filtered = allRecords.stream()
                .filter(row -> matches(row, term, status, startDate, endDate)).toList();
        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) selectedSize));
        int currentPage = Math.min(Math.max(0, page), pages - 1);
        int from = Math.min(currentPage * selectedSize, total);
        int to = Math.min(from + selectedSize, total);

        model.addAttribute("activePage", "operation-cdsco");
        model.addAttribute("operationAdmin", access.isAdmin(user));
        model.addAttribute("records", filtered.subList(from, to));
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("totalRecords", allRecords.size());
        model.addAttribute("total", total);
        model.addAttribute("page", currentPage);
        model.addAttribute("pages", pages);
        model.addAttribute("size", selectedSize);
        model.addAttribute("search", clean(search));
        model.addAttribute("status", clean(status));
        model.addAttribute("startDate", clean(startDate));
        model.addAttribute("endDate", clean(endDate));
        return "operation/cdsco/index";
    }

    @GetMapping("/operation/cdsco/create")
    public String createPage(Authentication authentication, Model model) {
        access.require(authentication);
        model.addAttribute("activePage", "operation-cdsco");
        model.addAttribute("editing", false);
        return "operation/cdsco/form";
    }

    @GetMapping("/operation/cdsco/{id}/edit")
    public String editPage(Authentication authentication, @PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        access.require(authentication);
        List<Map<String,Object>> matches = jdbc.queryForList("select * from cdscos where id = ?", id);
        if (matches.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "CDSCO record was not found.");
            return "redirect:/operation/cdsco";
        }
        model.addAttribute("activePage", "operation-cdsco");
        model.addAttribute("editing", true);
        model.addAttribute("recordId", id);
        model.addAttribute("record", formRecord(matches.get(0)));
        return "operation/cdsco/form";
    }

    @PostMapping("/operation/cdsco")
    public String create(Authentication authentication,
                         @RequestParam String companyName,
                         @RequestParam(required = false) String clientName,
                         @RequestParam(required = false) String mailId,
                         @RequestParam(required = false) String contactNumber,
                         @RequestParam(required = false) String service,
                         @RequestParam(required = false) String userId,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String projectStatus,
                         @RequestParam(required = false) String paymentStatus,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        if (clean(companyName).isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Company Name is required.");
            return "redirect:/operation/cdsco/create";
        }
        try {
            var columns = tableColumns();
            Map<String,Object> values = new LinkedHashMap<>();
            putColumn(values, columns, clean(companyName), "company_name", "name", "applicant_name");
            putColumn(values, columns, clean(clientName), "client_name", "client", "contact_person");
            putColumn(values, columns, clean(mailId), "mail_id", "email", "email_id", "mail");
            putColumn(values, columns, clean(contactNumber), "contact_number", "contact_no", "contact", "phone", "mobile");
            putColumn(values, columns, clean(service), "service", "service_name");
            putColumn(values, columns, clean(userId), "user_id", "userid");
            putColumn(values, columns, clean(password), "password", "portal_password");
            putColumn(values, columns, clean(projectStatus), "project_status", "current_status", "status");
            putColumn(values, columns, clean(paymentStatus), "payment_status", "paymentstatus");
            putColumn(values, columns, java.time.LocalDate.now(), "date", "application_date", "created_date");
            if (columns.contains("created_at")) values.put("created_at", new Timestamp(System.currentTimeMillis()));
            if (columns.contains("updated_at")) values.put("updated_at", new Timestamp(System.currentTimeMillis()));
            if (values.isEmpty()) throw new IllegalStateException("No supported CDSCO columns were found.");
            String names = String.join(",", values.keySet());
            String placeholders = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
            jdbc.update("insert into cdscos (" + names + ") values (" + placeholders + ")", values.values().toArray());
            redirectAttributes.addFlashAttribute("success", "CDSCO record added successfully.");
            return "redirect:/operation/cdsco";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Unable to add CDSCO record: " + exception.getMessage());
            return "redirect:/operation/cdsco/create";
        }
    }

    @PostMapping("/operation/cdsco/{id}")
    public String update(Authentication authentication, @PathVariable Long id,
                         @RequestParam String companyName,
                         @RequestParam(required = false) String clientName,
                         @RequestParam(required = false) String mailId,
                         @RequestParam(required = false) String contactNumber,
                         @RequestParam(required = false) String service,
                         @RequestParam(required = false) String userId,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String projectStatus,
                         @RequestParam(required = false) String paymentStatus,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        if (clean(companyName).isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Company Name is required.");
            return "redirect:/operation/cdsco/" + id + "/edit";
        }
        try {
            var columns = tableColumns();
            Map<String,Object> values = new LinkedHashMap<>();
            putColumn(values, columns, clean(companyName), "company_name", "name", "applicant_name");
            putColumnAllowBlank(values, columns, clean(clientName), "client_name", "client", "contact_person");
            putColumnAllowBlank(values, columns, clean(mailId), "mail_id", "email", "email_id", "mail");
            putColumnAllowBlank(values, columns, clean(contactNumber), "contact_number", "contact_no", "contact", "phone", "mobile");
            putColumnAllowBlank(values, columns, clean(service), "service", "service_name");
            putColumnAllowBlank(values, columns, clean(userId), "user_id", "userid");
            if (!clean(password).isBlank()) putColumn(values, columns, clean(password), "password", "portal_password");
            putColumnAllowBlank(values, columns, clean(projectStatus), "project_status", "current_status", "status");
            putColumnAllowBlank(values, columns, clean(paymentStatus), "payment_status", "paymentstatus");
            if (columns.contains("updated_at")) values.put("updated_at", new Timestamp(System.currentTimeMillis()));
            String idColumn = columns.contains("id") ? "id" : "cdsco_id";
            String assignments = values.keySet().stream().map(column -> column + " = ?").collect(Collectors.joining(", "));
            List<Object> arguments = new java.util.ArrayList<>(values.values()); arguments.add(id);
            int updated = jdbc.update("update cdscos set " + assignments + " where " + idColumn + " = ?", arguments.toArray());
            if (updated == 1) redirectAttributes.addFlashAttribute("success", "CDSCO record updated successfully.");
            else redirectAttributes.addFlashAttribute("error", "CDSCO record was not found.");
            return "redirect:/operation/cdsco";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Unable to update CDSCO record: " + exception.getMessage());
            return "redirect:/operation/cdsco/" + id + "/edit";
        }
    }

    @PostMapping("/operation/cdsco/{id}/delete")
    public String delete(Authentication authentication, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        int deleted = jdbc.update("delete from cdscos where id = ?", id);
        if (deleted == 1) redirectAttributes.addFlashAttribute("success", "CDSCO record deleted successfully.");
        else redirectAttributes.addFlashAttribute("error", "CDSCO record was not found.");
        return "redirect:/operation/cdsco";
    }

    private boolean matches(Map<String,Object> row, String term, String status, String startDate, String endDate) {
        String searchable = String.join(" ", row.values().stream().map(this::text).toList()).toLowerCase(Locale.ROOT);
        if (!term.isBlank() && !searchable.contains(term)) return false;
        if (!clean(status).isBlank() && !clean(status).equalsIgnoreCase(text(row.get("status")))) return false;
        String date = text(row.get("date"));
        if (!clean(startDate).isBlank() && date.compareTo(startDate) < 0) return false;
        return clean(endDate).isBlank() || date.compareTo(endDate) <= 0;
    }

    private Map<String,Object> normalise(Map<String,Object> source) {
        Map<String,Object> raw = new LinkedHashMap<>();
        source.forEach((key, value) -> raw.put(key.toLowerCase(Locale.ROOT), value));
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("id", first(raw, "id", "cdsco_id"));
        row.put("name", display(first(raw, "name", "company_name", "applicant_name", "client_name")));
        Object email = first(raw, "email", "email_id", "mail_id", "mail", "emailid", "mailid",
                "e_mail", "client_mail_id", "client_email", "company_email", "email_address");
        if (email == null) email = firstMatching(raw, "email", "mail");
        row.put("email", display(email));
        row.put("service", display(first(raw, "service", "service_name")));
        row.put("contact", display(first(raw, "contact", "contact_no", "contact_number", "phone", "mobile", "phone_no")));
        Object userId = first(raw, "user_id", "userid", "created_by", "createdby");
        String displayedUserId = display(userId);
        row.put("userId", displayedUserId.matches("\\d+") ? "N/A" : displayedUserId);
        row.put("password", display(first(raw, "password", "portal_password", "passcode")));
        row.put("status", paymentStatusLabel(first(raw, "payment_status", "paymentstatus")));
        Object date = first(raw, "date", "application_date", "created_at", "created_date", "updated_at");
        String dateText = date == null ? "" : date.toString();
        row.put("date", dateText.isBlank() ? "N/A" : dateText.substring(0, Math.min(10, dateText.length())));
        return row;
    }

    private Map<String,Object> formRecord(Map<String,Object> source) {
        Map<String,Object> raw = new LinkedHashMap<>();
        source.forEach((key,value) -> raw.put(key.toLowerCase(Locale.ROOT), value));
        Map<String,Object> record = new LinkedHashMap<>();
        record.put("companyName", formValue(first(raw, "company_name", "name", "applicant_name")));
        record.put("clientName", formValue(first(raw, "client_name", "client", "contact_person")));
        Object email = first(raw, "mail_id", "email", "email_id", "mail", "emailid", "mailid",
                "e_mail", "client_mail_id", "client_email", "company_email", "email_address");
        if (email == null) email = firstMatching(raw, "email", "mail");
        record.put("mailId", formValue(email));
        record.put("contactNumber", formValue(first(raw, "contact_number", "contact_no", "contact", "phone", "mobile")));
        record.put("service", formValue(first(raw, "service", "service_name")));
        record.put("userId", formValue(first(raw, "user_id", "userid")));
        record.put("projectStatus", formValue(first(raw, "project_status", "current_status", "status")));
        record.put("paymentStatus", paymentStatusLabel(first(raw, "payment_status", "paymentstatus")));
        return record;
    }

    private Object first(Map<String,Object> row, String... keys) {
        for (String key : keys) if (row.get(key) != null) return row.get(key);
        return null;
    }

    private Object firstMatching(Map<String,Object> row, String... fragments) {
        for (Map.Entry<String,Object> entry : row.entrySet()) {
            if (entry.getValue() == null || entry.getKey().contains("password") || entry.getKey().contains("passcode")) continue;
            for (String fragment : fragments)
                if (entry.getKey().contains(fragment)) return entry.getValue();
        }
        return null;
    }

    private java.util.Set<String> tableColumns() {
        return jdbc.query("select * from cdscos limit 0", resultSet -> {
            java.util.Set<String> columns = new java.util.LinkedHashSet<>();
            ResultSetMetaData metadata = resultSet.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++)
                columns.add(metadata.getColumnLabel(index).toLowerCase(Locale.ROOT));
            return columns;
        });
    }

    private void putColumn(Map<String,Object> values, java.util.Set<String> columns, Object value, String... candidates) {
        if (value == null || value.toString().isBlank()) return;
        for (String candidate : candidates) if (columns.contains(candidate)) { values.put(candidate, value); return; }
    }

    private void putColumnAllowBlank(Map<String,Object> values, java.util.Set<String> columns, String value, String... candidates) {
        for (String candidate : candidates) if (columns.contains(candidate)) { values.put(candidate, value.isBlank() ? null : value); return; }
    }

    private String formValue(Object value) {
        if (value == null) return "";
        String result = value.toString().trim();
        return result.equalsIgnoreCase("null") ? "" : result;
    }

    private String paymentStatusLabel(Object value) {
        String status = value == null ? "" : value.toString().trim();
        if (status.isBlank()) return "N/A";
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "1", "yes", "paid" -> "Yes";
            case "0", "no", "unpaid" -> "No";
            case "2", "half", "50", "50%", "partial", "partially paid",
                    "50% received", "50% recieved", "50% payment received", "50% payment recieved" -> "Half";
            case "100%", "100% received", "100% recieved", "100% payment received",
                    "100% payment recieved" -> "100% Received";
            case "3", "pending" -> "Pending";
            default -> status;
        };
    }

    private String display(Object value) {
        String result = value == null ? "" : value.toString().trim();
        return result.isBlank() || result.equalsIgnoreCase("null") ? "N/A" : result;
    }

    private String text(Object value) { return value == null ? "" : value.toString(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
