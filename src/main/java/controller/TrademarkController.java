package controller;

import model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.UserRepository;
import service.OperationAccessService;
import service.TrademarkExcelService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Trademark list.  The legacy trademarks table is intentionally read through JDBC so this UI can
 * work with the column names already present in crmevtl rather than attempting to migrate them. */
@Controller
public class TrademarkController {
    private final OperationAccessService access;
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final TrademarkExcelService excel;

    public TrademarkController(OperationAccessService access, JdbcTemplate jdbc, UserRepository users,
                               TrademarkExcelService excel) {
        this.access = access; this.jdbc = jdbc; this.users = users; this.excel = excel;
    }

    @GetMapping("/operation/trademark")
    public String index(Authentication authentication, Model model,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String paymentStatus,
                        @RequestParam(required = false, name = "status") String statusFilter,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "0") int page) {
        User user = access.require(authentication);
        int selectedSize = List.of(10, 25, 50, 100).contains(size) ? size : 25;
        String term = safe(search).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = jdbc.queryForList("select * from trademarks");
        List<Map<String, Object>> records = rows.stream().map(this::normalise)
                .filter(r -> matches(r, term, paymentStatus, statusFilter, startDate, endDate))
                .sorted(Comparator.comparing((Map<String, Object> r) -> safe((String) r.get("date"))).reversed())
                .toList();
        Map<Long, String> creatorNames = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        records.forEach(r -> r.put("creatorName", creatorNames.getOrDefault(r.get("createdBy"), string(r.get("createdBy")))));
        int total = records.size(), pages = Math.max(1, (int) Math.ceil(total / (double) selectedSize));
        int current = Math.min(Math.max(0, page), pages - 1), from = Math.min(current * selectedSize, total), to = Math.min(from + selectedSize, total);
        model.addAttribute("activePage", "operation-trademark");
        model.addAttribute("operationAdmin", access.isAdmin(user));
        model.addAttribute("records", records.subList(from, to));
        model.addAttribute("total", total); model.addAttribute("totalTrademarks", rows.size());
        model.addAttribute("submitted", count(rows, "Submitted") + count(rows, "1"));
        model.addAttribute("pending", count(rows, "Pending")); model.addAttribute("query", count(rows, "Query"));
        model.addAttribute("queryReply", count(rows, "Query Reply") + count(rows, "Query Replied")); model.addAttribute("granted", count(rows, "Granted"));
        model.addAttribute("page", current); model.addAttribute("pages", pages); model.addAttribute("size", selectedSize);
        model.addAttribute("search", safe(search)); model.addAttribute("paymentStatus", safe(paymentStatus));
        model.addAttribute("statusFilter", safe(statusFilter));
        model.addAttribute("startDate", safe(startDate)); model.addAttribute("endDate", safe(endDate));
        return "operation/trademark/index";
    }

    @GetMapping("/operation/trademark/create")
    public String createPage(Authentication authentication, Model model) {
        access.require(authentication);
        model.addAttribute("activePage", "operation-trademark");
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editing", false);
        return "operation/trademark/form";
    }

    @GetMapping("/operation/trademark/{id}/edit")
    public String editPage(Authentication authentication, @PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        access.require(authentication);
        List<Map<String,Object>> matches = jdbc.queryForList("select * from trademarks where id = ?", id);
        if (matches.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Trademark record was not found.");
            return "redirect:/operation/trademark";
        }
        Map<String,Object> trademark = normalise(matches.get(0));
        trademark.put("paymentCode", paymentCode(matches.get(0).entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("payment_status"))
                .map(Map.Entry::getValue).findFirst().orElse(null)));
        model.addAttribute("activePage", "operation-trademark");
        model.addAttribute("editing", true);
        model.addAttribute("trademark", trademark);
        model.addAttribute("today", LocalDate.now());
        return "operation/trademark/form";
    }

    @GetMapping("/operation/trademark/{id}/data")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> editData(Authentication authentication, @PathVariable Long id) {
        access.require(authentication);
        List<Map<String,Object>> matches = jdbc.queryForList("select * from trademarks where id = ?", id);
        if (matches.isEmpty()) return ResponseEntity.notFound().build();
        Map<String,Object> trademark = normalise(matches.get(0));
        trademark.put("paymentCode", paymentCode(matches.get(0).entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("payment_status"))
                .map(Map.Entry::getValue).findFirst().orElse(null)));
        return ResponseEntity.ok(trademark);
    }

    @PostMapping("/operation/trademark")
    public String create(Authentication authentication,
                         @RequestParam String companyName,
                         @RequestParam LocalDate date,
                         @RequestParam(required = false) String contactNo,
                         @RequestParam(required = false) String mailId,
                         @RequestParam(required = false) String tradeMarkClass,
                         @RequestParam(required = false) String tradeMarkName,
                         @RequestParam(required = false) String appNo,
                         @RequestParam(required = false) Integer paymentStatus,
                         @RequestParam(required = false) String markStatus,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "5 YEAR") String validity,
                         @RequestParam(required = false) String address,
                         @RequestParam(required = false) String remark,
                         RedirectAttributes redirectAttributes) {
        User user = access.require(authentication);
        if (companyName == null || companyName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Company Name is required.");
            return "redirect:/operation/trademark/create";
        }
        jdbc.update("""
                insert into trademarks
                (created_by, company_name, contact_no, mail_id, trade_mark_class, trade_mark_name,
                 app_no, payment_status, mark_status, status, validity, address, remark, date, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, user.getId(), clean(companyName), clean(contactNo), clean(mailId), clean(tradeMarkClass),
                clean(tradeMarkName), clean(appNo), paymentStatus, clean(markStatus), clean(status),
                clean(validity), clean(address), clean(remark), date);
        redirectAttributes.addFlashAttribute("success", "Trademark added successfully.");
        return "redirect:/operation/trademark";
    }

    @PostMapping("/operation/trademark/{id}")
    public String update(Authentication authentication, @PathVariable Long id,
                         @RequestParam String companyName,
                         @RequestParam LocalDate date,
                         @RequestParam(required = false) String contactNo,
                         @RequestParam(required = false) String mailId,
                         @RequestParam(required = false) String tradeMarkClass,
                         @RequestParam(required = false) String tradeMarkName,
                         @RequestParam(required = false) String appNo,
                         @RequestParam(required = false) Integer paymentStatus,
                         @RequestParam(required = false) String markStatus,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "5 YEAR") String validity,
                         @RequestParam(required = false) String address,
                         @RequestParam(required = false) String remark,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        if (companyName == null || companyName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Company Name is required.");
            return "redirect:/operation/trademark/" + id + "/edit";
        }
        int updated = jdbc.update("""
                update trademarks set company_name = ?, contact_no = ?, mail_id = ?, trade_mark_class = ?,
                trade_mark_name = ?, app_no = ?, payment_status = ?, mark_status = ?, status = ?, validity = ?,
                address = ?, remark = ?, date = ?, updated_at = current_timestamp where id = ?
                """, clean(companyName), clean(contactNo), clean(mailId), clean(tradeMarkClass),
                clean(tradeMarkName), clean(appNo), paymentStatus, clean(markStatus), clean(status),
                clean(validity), clean(address), clean(remark), date, id);
        if (updated == 1) redirectAttributes.addFlashAttribute("success", "Trademark updated successfully.");
        else redirectAttributes.addFlashAttribute("error", "Trademark record was not found.");
        return "redirect:/operation/trademark";
    }

    @PostMapping("/operation/trademark/import")
    public String importExcel(Authentication authentication, @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            TrademarkExcelService.ImportResult result = excel.importFile(access.require(authentication), file);
            String summary = result.imported() == 0 ? "No new trademark records were imported."
                    : result.imported() + " trademark record" + (result.imported() == 1 ? "" : "s") + " imported successfully.";
            if (result.duplicates() > 0) summary += " " + result.duplicates() + " duplicate row"
                    + (result.duplicates() == 1 ? " was" : "s were") + " ignored.";
            redirectAttributes.addFlashAttribute("success", summary);
            if (result.invalid() > 0) {
                String detail = "Import validation warning: " + result.invalid() + " row"
                        + (result.invalid() == 1 ? " was" : "s were") + " rejected because required or valid data was missing.";
                if (!result.errors().isEmpty()) detail += System.lineSeparator() + String.join(System.lineSeparator(), result.errors());
                if (result.invalid() > result.errors().size()) detail += System.lineSeparator() + "Additional invalid rows were omitted from this notification.";
                redirectAttributes.addFlashAttribute("error", detail);
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/operation/trademark";
    }

    @PostMapping("/operation/trademark/{id}/delete")
    public String delete(Authentication authentication, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        int deleted = jdbc.update("delete from trademarks where id = ?", id);
        if (deleted == 1) redirectAttributes.addFlashAttribute("success", "Trademark record deleted permanently.");
        else redirectAttributes.addFlashAttribute("error", "Trademark record was not found or had already been deleted.");
        return "redirect:/operation/trademark";
    }

    @GetMapping(value = "/operation/trademark/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(required = false) String paymentStatus,
                                               @RequestParam(required = false, name = "status") String statusFilter,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate) {
        access.require(authentication);
        String term = safe(search).toLowerCase(Locale.ROOT);
        List<Map<String,Object>> filtered = jdbc.queryForList("select * from trademarks").stream()
                .map(this::normalise)
                .filter(row -> matches(row, term, paymentStatus, statusFilter, startDate, endDate))
                .map(row -> {
                    Map<String,Object> original = new LinkedHashMap<>();
                    row.forEach((key,value) -> original.put(key.toLowerCase(Locale.ROOT), value));
                    return original;
                }).toList();
        byte[] spreadsheet = excel.export(filtered);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trademarks-" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(spreadsheet.length).body(spreadsheet);
    }

    @GetMapping("/operation/trademark/analytics")
    @ResponseBody
    public Map<String,Object> analytics(Authentication authentication,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
        access.require(authentication);
        List<Map<String,Object>> source = jdbc.queryForList("select status, date from trademarks where date is not null");
        SortedSet<Integer> availableYears = new TreeSet<>(Comparator.reverseOrder());
        for (Map<String,Object> row : source) { LocalDate date = toLocalDate(row.get("date")); if (date != null) availableYears.add(date.getYear()); }
        int selectedYear = year != null ? year : (availableYears.isEmpty() ? LocalDate.now().getYear() : availableYears.first());
        int selectedMonth = month != null && month >= 1 && month <= 12 ? month : 0;
        int[][] monthly = new int[12][4];
        for (Map<String,Object> row : source) {
            LocalDate date = toLocalDate(row.get("date")); if (date == null || date.getYear() != selectedYear) continue;
            int category = analyticsCategory(row.get("status")); if (category >= 0) monthly[date.getMonthValue() - 1][category]++;
        }
        int[] totals = new int[4];
        for (int m = 0; m < 12; m++) if (selectedMonth == 0 || selectedMonth == m + 1)
            for (int category = 0; category < 4; category++) totals[category] += monthly[m][category];
        List<Map<String,Integer>> monthlyData = new ArrayList<>();
        for (int m = 0; m < 12; m++) monthlyData.add(Map.of("submit", monthly[m][0], "approve", monthly[m][1], "query", monthly[m][2], "grant", monthly[m][3]));
        return Map.of("years", availableYears, "year", selectedYear, "month", selectedMonth,
                "submit", totals[0], "approve", totals[1], "query", totals[2], "grant", totals[3],
                "total", Arrays.stream(totals).sum(), "monthly", monthlyData);
    }

    private int analyticsCategory(Object status) {
        String value = status == null ? "" : status.toString().trim().toLowerCase(Locale.ROOT);
        if (value.equals("1") || value.equals("submitted") || value.equals("submit")) return 0;
        if (value.equals("approved") || value.equals("approve") || value.equals("accepted & advertised")) return 1;
        if (value.equals("query") || value.equals("query reply") || value.equals("query replied") || value.equals("objected")) return 2;
        if (value.equals("granted") || value.equals("grant") || value.equals("registered")) return 3;
        return -1;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof LocalDate date) return date;
        if (value == null) return null;
        try { return LocalDate.parse(value.toString().substring(0, 10)); } catch (Exception ignored) { return null; }
    }

    private int count(List<Map<String,Object>> rows, String state) { return (int) rows.stream().map(this::normalise).filter(r -> state.equalsIgnoreCase(string(r.get("status")))).count(); }
    private boolean matches(Map<String,Object> r, String term, String payment, String statusFilter, String start, String end) {
        String text = String.join(" ", r.values().stream().map(this::string).toList()).toLowerCase(Locale.ROOT);
        if (!term.isBlank() && !text.contains(term)) return false;
        if (!safe(payment).isBlank()) {
            if (!payment.equalsIgnoreCase(string(r.get("paymentStatus")))) return false;
        }
        if (!safe(statusFilter).isBlank()) {
            String actual = string(r.get("status"));
            boolean matchesStatus = statusFilter.equalsIgnoreCase(actual)
                    || (statusFilter.equalsIgnoreCase("Submitted") && actual.equals("1"))
                    || (statusFilter.equalsIgnoreCase("Query Reply") && actual.equalsIgnoreCase("Query Replied"));
            if (!matchesStatus) return false;
        }
        String date = string(r.get("date"));
        return (safe(start).isBlank() || date.compareTo(start) >= 0) && (safe(end).isBlank() || date.compareTo(end) <= 0);
    }
    private Map<String,Object> normalise(Map<String,Object> source) {
        Map<String,Object> r = new LinkedHashMap<>(); source.forEach((k,v) -> r.put(k.toLowerCase(Locale.ROOT), v));
        put(r,"createdBy", "created_by","createdby","user_id"); put(r,"applicationNo", "application_no","app_no","application_number","appno");
        put(r,"tradeClass", "class","trade_class","tm_class"); put(r,"validity", "validity","validity_year");
        put(r,"company", "company_name","company","client_name","name");
        put(r,"email", "email","mail","email_id","email_address","company_email","client_email","mail_id","emailid","e_mail");
        put(r,"phone", "phone","mobile","contact_no","contact_number","phone_no","mobile_no");
        if (r.get("email") == null) r.put("email", firstMatching(r, "email", "mail"));
        put(r,"address", "address","company_address"); put(r,"paymentStatus", "payment_status","paymentstatus"); put(r,"status", "project_status","status","application_status");
        put(r,"markStatus", "mark_status","trademark_status","mark_type"); put(r,"remarks", "remarks","remark","comment"); put(r,"id", "id");
        r.put("paymentStatus", paymentLabel(r.get("paymentStatus")));
        r.put("status", statusLabel(r.get("status")));
        r.put("markStatus", markStatusLabel(r.get("markStatus")));
        Object date = first(r,"date","application_date","created_at","created_date","submission_date"); r.put("date", date == null ? "" : date.toString().substring(0, Math.min(10,date.toString().length())));
        List.of("applicationNo", "tradeClass", "validity", "company", "email", "phone", "address",
                "paymentStatus", "status", "markStatus", "date", "remarks")
                .forEach(key -> r.put(key, displayValue(r.get(key))));
        return r;
    }
    private void put(Map<String,Object> r,String target,String... keys){ r.put(target, first(r,keys)); }
    private Object first(Map<String,Object> r,String... keys){ for(String key:keys) if(r.get(key)!=null) return r.get(key); return null; }
    private Object firstMatching(Map<String,Object> r, String... fragments) {
        for (Map.Entry<String,Object> entry : r.entrySet()) {
            if (entry.getValue() == null) continue;
            for (String fragment : fragments)
                if (entry.getKey().contains(fragment)) return entry.getValue();
        }
        return null;
    }
    private String displayValue(Object value) {
        if (value == null) return "N/A";
        String text = value.toString().trim();
        return text.isEmpty() || text.equalsIgnoreCase("null") ? "N/A" : text;
    }
    private String paymentLabel(Object value) {
        String code = value == null ? "" : value.toString().trim();
        return switch (code) { case "0" -> "No"; case "1" -> "Yes"; case "2" -> "Half"; case "3" -> "Pending"; default -> value == null ? null : value.toString(); };
    }
    private String paymentCode(Object value) {
        String current = value == null ? "" : value.toString().trim();
        return switch (current.toLowerCase(Locale.ROOT)) {
            case "yes", "1" -> "1";
            case "half", "2" -> "2";
            case "pending", "3" -> "3";
            case "no", "0" -> "0";
            default -> "";
        };
    }
    private String statusLabel(Object value) {
        String code = value == null ? "" : value.toString().trim();
        return "1".equals(code) ? "Submitted" : value == null ? null : value.toString();
    }
    private String markStatusLabel(Object value) {
        String code = value == null ? "" : value.toString().trim();
        return switch (code) { case "1" -> "Word Mark"; case "2" -> "Logo Mark"; default -> value == null ? null : value.toString(); };
    }
    private String string(Object value){ return displayValue(value); }
    private String safe(String value){ return value == null ? "" : value.trim(); }
    private String clean(String value){ String text = safe(value); return text.isEmpty() ? null : text; }
}
