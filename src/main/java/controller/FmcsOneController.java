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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.FmcsOneExcelService;
import service.OperationAccessService;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Collectors;

/** FMCS-1 operation workspace. Records will be wired here when its data source is available. */
@Controller
public class FmcsOneController {
    public record SearchSuggestion(String value, String detail) {}
    public record CountryCount(String country, int count) {}
    public record AnalyticsResponse(int year, List<CountryCount> countries, int maximum, String xAxisTitle) {}
    private static final List<String> ANALYTICS_COUNTRIES = List.of("Indonesia", "Thailand", "Vietnam", "China", "South Korea", "Turkey", "Italy", "Nepal", "Zambia", "Czech Republic", "Germany", "Malaysia", "Bangladesh", "Denmark", "Egypt", "France", "Japan", "Saudi Arabia", "United Kingdom", "Morocco", "Taiwan");
    /* The FMCS table stores the legacy country catalogue IDs rather than country names. */
    private static final Map<Integer, String> FMCS_COUNTRY_NAMES = Map.ofEntries(
            Map.entry(102, "Indonesia"), Map.entry(217, "Thailand"), Map.entry(238, "Vietnam"),
            Map.entry(44, "China"), Map.entry(116, "South Korea"), Map.entry(223, "Turkey"),
            Map.entry(107, "Italy"), Map.entry(153, "Nepal"), Map.entry(245, "Zambia"),
            Map.entry(57, "Czech Republic"), Map.entry(82, "Germany"), Map.entry(132, "Malaysia"),
            Map.entry(18, "Bangladesh"), Map.entry(58, "Denmark"), Map.entry(64, "Egypt"),
            Map.entry(75, "France"), Map.entry(109, "Japan"), Map.entry(191, "Saudi Arabia"),
            Map.entry(230, "United Kingdom"), Map.entry(148, "Morocco"), Map.entry(214, "Taiwan"));
    private final OperationAccessService access;
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final FmcsOneExcelService excel;

    public FmcsOneController(OperationAccessService access, JdbcTemplate jdbc, UserRepository users, FmcsOneExcelService excel) {
        this.access = access;
        this.jdbc = jdbc;
        this.users = users;
        this.excel = excel;
    }

    @PostMapping("/operation/fmcs-1/import")
    public String importExcel(Authentication authentication, @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            FmcsOneExcelService.ImportResult result = excel.importFile(access.require(authentication), file);
            String message = result.imported() + " FMCS record" + (result.imported() == 1 ? "" : "s") + " imported successfully.";
            if (result.duplicates() > 0) message += " " + result.duplicates() + " duplicate row" + (result.duplicates() == 1 ? " was" : "s were") + " ignored.";
            redirectAttributes.addFlashAttribute("success", message);
            if (result.invalid() > 0) redirectAttributes.addFlashAttribute("error", result.invalid() + " invalid row" + (result.invalid() == 1 ? " was" : "s were") + " skipped. " + String.join(" | ", result.errors()));
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/operation/fmcs-1";
    }

    @GetMapping("/operation/fmcs-1/suggestions")
    @ResponseBody
    public List<SearchSuggestion> suggestions(Authentication authentication, @RequestParam String query) {
        access.require(authentication);
        String term = query == null ? "" : query.trim();
        if (term.isBlank()) return List.of();
        List<Object> arguments = new ArrayList<>();
        String where = filterWhere(null, null, "", term, "", "", arguments);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select cml_no, company_name, client_name, client_email, indian_standard, product_name, app_no
                from f_m_c_s_operations """ + " " + where + " limit 20", arguments.toArray());
        Map<String, SearchSuggestion> matches = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            addSuggestion(matches, row.get("company_name"), "Company");
            addSuggestion(matches, row.get("client_name"), "Client");
            addSuggestion(matches, row.get("app_no"), "Application No.");
            addSuggestion(matches, row.get("cml_no"), "CML No.");
            addSuggestion(matches, row.get("product_name"), "Product");
            addSuggestion(matches, row.get("indian_standard"), "Indian Standard");
        }
        return matches.values().stream().limit(8).toList();
    }

    @GetMapping(value = "/operation/fmcs-1/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(required = false) String client,
                                               @RequestParam(required = false) String payment) {
        access.require(authentication);
        List<Object> arguments = new ArrayList<>();
        String where = filterWhere(parseDate(startDate), parseDate(endDate), normalizedStatus(status), search, normalizedClient(client), normalizedPayment(payment), arguments);
        List<Map<String, Object>> records = jdbc.queryForList("""
                select cml_no, company_name, client_name, client_email, indian_standard,
                       product_name, app_no, bis_pay, license_status
                from f_m_c_s_operations """ + " " + where + " order by id desc", arguments.toArray());
        byte[] spreadsheet = excel.export(records);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fmcs-list-" + java.time.LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(spreadsheet.length).body(spreadsheet);
    }

    @GetMapping("/operation/fmcs-1")
    public String index(Authentication authentication, Model model,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String client,
                        @RequestParam(required = false) String payment,
                        @RequestParam(defaultValue = "2026") int analyticsYear) {
        access.require(authentication);
        int selectedSize = List.of(10, 25, 50, 100).contains(size) ? size : 25;
        int selectedAnalyticsYear = List.of(2024, 2025, 2026).contains(analyticsYear) ? analyticsYear : 2026;
        LocalDate start = parseDate(startDate), end = parseDate(endDate);
        String selectedStatus = normalizedStatus(status);
        String selectedSearch = search == null ? "" : search.trim();
        String selectedClient = normalizedClient(client);
        String selectedPayment = normalizedPayment(payment);
        List<Object> filterArguments = new ArrayList<>();
        String where = filterWhere(start, end, selectedStatus, selectedSearch, selectedClient, selectedPayment, filterArguments);
        int total = jdbc.queryForObject("select count(*) from f_m_c_s_operations" + where, Integer.class, filterArguments.toArray());
        int pages = Math.max(1, (int) Math.ceil(total / (double) selectedSize));
        int currentPage = Math.min(Math.max(0, page), pages - 1);
        Map<String, Object> counts = jdbc.queryForMap("""
                select
                  sum(lower(trim(coalesce(status, ''))) in ('fresh project', 'fresh')) fresh_project,
                  sum(lower(trim(coalesce(status, ''))) like '%docs review%') docs_review,
                  sum(lower(trim(coalesce(status, ''))) like '%document submit%bis%') document_submit,
                  sum(trim(coalesce(app_no, '')) not in ('', 'NA', 'N/A')) application_no,
                  sum(lower(trim(coalesce(status, ''))) like '%nomination%pending%') nomination_pending,
                  sum(lower(trim(coalesce(status, ''))) like '%nomination%done%') nomination_done,
                  sum(lower(trim(coalesce(status, ''))) like '%inspection%pending%') inspection_pending,
                  sum(lower(trim(coalesce(status, ''))) like '%inspection%done%') inspection_done,
                  sum(lower(trim(coalesce(license_status, ''))) like '%grant%' or lower(trim(coalesce(status, ''))) like '%lic%grant%') license_grant,
                  sum(lower(trim(coalesce(status, ''))) like '%hold%') hold_count,
                  sum(trim(coalesce(pbg_upload, '')) not in ('', 'NA', 'N/A') or lower(trim(coalesce(status, ''))) like '%pbg%done%') pbg_done,
                  sum(trim(coalesce(bis_pay, '')) not in ('', 'NA', 'N/A')) payment
                from f_m_c_s_operations
                """);
        Map<Long, String> creatorNames = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName, (first, ignored) -> first));
        List<Map<String, Object>> records = jdbc.queryForList("""
                select id, created_by, indian_standard, app_no, cml_no, product_name,
                       company_name, client_name, country_id, license_status, service_fee,
                       date_format(nullif(`date`, '0000-00-00'), '%d-%m-%Y') entry_date,
                       date_format(created_at, '%d-%m-%Y') created_date
                from f_m_c_s_operations """ + " " + where + """
                order by id desc
                limit ? offset ?
                """, pageArguments(filterArguments, selectedSize, currentPage * selectedSize).toArray());
        records.forEach(record -> {
            record.put("creatorName", creatorNames.getOrDefault(asLong(record.get("created_by")), "—"));
            Object entryDate = record.get("entry_date");
            record.put("entryDate", entryDate == null || entryDate.toString().isBlank() ? record.get("created_date") : entryDate);
            record.put("isStandard", displayValue(record.get("indian_standard")));
            record.put("applicationNo", displayValue(record.get("app_no")));
            record.put("cmlNo", displayValue(record.get("cml_no")));
            record.put("companyName", displayValue(record.get("company_name")));
            record.put("productName", displayValue(record.get("product_name")));
            Long countryId = asLong(record.get("country_id"));
            record.put("countryName", countryId == null ? "N/A" : FMCS_COUNTRY_NAMES.getOrDefault(countryId.intValue(), "N/A"));
            record.put("licenseStatus", displayValue(record.get("license_status")));
            record.put("clientName", displayValue(record.get("client_name")));
            record.put("serviceFee", displayValue(record.get("service_fee")));
        });
        model.addAttribute("activePage", "operation-fmcs-1");
        model.addAttribute("records", records);
        model.addAttribute("page", currentPage);
        model.addAttribute("pages", pages);
        model.addAttribute("size", selectedSize);
        model.addAttribute("total", total);
        model.addAttribute("startDate", start == null ? "" : start.toString());
        model.addAttribute("endDate", end == null ? "" : end.toString());
        model.addAttribute("status", selectedStatus);
        model.addAttribute("search", selectedSearch);
        model.addAttribute("client", selectedClient);
        model.addAttribute("payment", selectedPayment);
        Map<String, Integer> countryCounts = new LinkedHashMap<>();
        jdbc.queryForList("""
                select country_id, count(*) total
                from f_m_c_s_operations
                where coalesce(nullif(year(nullif(`date`, '0000-00-00')), 0), year(created_at)) = ?
                  and country_id is not null
                group by country_id
                """, selectedAnalyticsYear).forEach(row -> {
            Object countryId = row.get("country_id");
            if (countryId instanceof Number number) {
                String country = FMCS_COUNTRY_NAMES.get(number.intValue());
                if (country != null) countryCounts.merge(country, ((Number) row.get("total")).intValue(), Integer::sum);
            }
        });
        List<CountryCount> yearlyPerformance = ANALYTICS_COUNTRIES.stream().map(country -> new CountryCount(country, countryCounts.getOrDefault(country, 0))).toList();
        model.addAttribute("analyticsYear", selectedAnalyticsYear);
        model.addAttribute("yearlyPerformance", yearlyPerformance);
        model.addAttribute("analyticsMax", yearlyPerformance.stream().mapToInt(CountryCount::count).max().orElse(0));
        model.addAttribute("statusCards", List.of(
                card("Fresh Project", "F", counts.get("fresh_project")), card("Docs Review", "D", counts.get("docs_review")),
                card("Document Submit To BIS", "B", counts.get("document_submit")), card("Application No.", "A", counts.get("application_no")),
                card("Nomination Pending", "N", counts.get("nomination_pending")), card("Nomination Done", "✓", counts.get("nomination_done")),
                card("Inspection Pending", "I", counts.get("inspection_pending")), card("Inspection Done", "✓", counts.get("inspection_done")),
                card("Lic. Grant", "L", counts.get("license_grant")), card("Hold", "H", counts.get("hold_count")),
                card("PBG Done", "P", counts.get("pbg_done")), card("Payment", "₹", counts.get("payment"))));
        return "operation/fmcs-1/index";
    }

    @GetMapping("/operation/fmcs-1/analytics")
    @ResponseBody
    public AnalyticsResponse analytics(Authentication authentication, @RequestParam(defaultValue = "2026") int year,
                                       @RequestParam(name = "country", required = false) String countryFilter,
                                       @RequestParam(required = false) String type) {
        access.require(authentication);
        int selectedYear = List.of(2024, 2025, 2026).contains(year) ? year : 2026;
        String selectedCountry = ANALYTICS_COUNTRIES.contains(countryFilter) ? countryFilter : "";
        String selectedType = List.of("Quarterly", "Half-Yearly").contains(type) ? type : "";
        if (!selectedType.isBlank() || !selectedCountry.isBlank()) {
            Integer countryId = selectedCountry.isBlank() ? null : FMCS_COUNTRY_NAMES.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(selectedCountry)).map(Map.Entry::getKey).findFirst().orElse(null);
            String countryClause = countryId == null ? "" : " and country_id = ?";
            String effectiveDate = "coalesce(nullif(`date`, '0000-00-00'), date(created_at))";
            String periodExpression = selectedType.equals("Quarterly") ? "quarter(" + effectiveDate + ")"
                    : "if(month(" + effectiveDate + ") <= 6, 1, 2)";
            List<Object> periodArguments = new ArrayList<>();
            periodArguments.add(selectedYear);
            if (countryId != null) periodArguments.add(countryId);
            Integer latestPeriod = selectedType.isBlank() ? null : jdbc.queryForObject(
                    "select max(" + periodExpression + ") from f_m_c_s_operations where coalesce(nullif(year(nullif(`date`, '0000-00-00')), 0), year(created_at)) = ?" + countryClause,
                    Integer.class, periodArguments.toArray());
            if (!selectedType.isBlank() && (latestPeriod == null || latestPeriod == 0))
                return new AnalyticsResponse(selectedYear, List.of(), 0, "Companies");
            List<Object> arguments = new ArrayList<>();
            arguments.add(selectedYear);
            if (!selectedType.isBlank()) arguments.add(latestPeriod);
            if (countryId != null) arguments.add(countryId);
            List<CountryCount> performance = jdbc.queryForList("select coalesce(nullif(trim(company_name), ''), 'Unknown Company') company, count(*) total from f_m_c_s_operations "
                    + "where coalesce(nullif(year(nullif(`date`, '0000-00-00')), 0), year(created_at)) = ?"
                    + (selectedType.isBlank() ? "" : " and " + periodExpression + " = ?")
                    + countryClause + " group by company order by total desc, company asc limit 24", arguments.toArray()).stream()
                    .map(row -> new CountryCount(String.valueOf(row.get("company")), ((Number) row.get("total")).intValue())).toList();
            return new AnalyticsResponse(selectedYear, performance, performance.stream().mapToInt(CountryCount::count).max().orElse(0), "Companies");
        }
        Map<String, Integer> countryCounts = new LinkedHashMap<>();
        jdbc.queryForList("""
                select country_id, count(*) total
                from f_m_c_s_operations
                where coalesce(nullif(year(nullif(`date`, '0000-00-00')), 0), year(created_at)) = ?
                  and country_id is not null
                group by country_id
                """, selectedYear).forEach(row -> {
            Object countryId = row.get("country_id");
            if (countryId instanceof Number number) {
                String country = FMCS_COUNTRY_NAMES.get(number.intValue());
                if (country != null) countryCounts.merge(country, ((Number) row.get("total")).intValue(), Integer::sum);
            }
        });
        List<CountryCount> performance = ANALYTICS_COUNTRIES.stream()
                .filter(item -> selectedCountry.isBlank() || item.equals(selectedCountry))
                .map(country -> new CountryCount(country, countryCounts.getOrDefault(country, 0))).toList();
        return new AnalyticsResponse(selectedYear, performance, performance.stream().mapToInt(CountryCount::count).max().orElse(0), "Countries");
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(value.toString()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String displayValue(Object value) {
        String text = value == null ? "" : value.toString().trim();
        return text.isBlank() || text.equalsIgnoreCase("NA") || text.equalsIgnoreCase("N/A") ? "N/A" : text;
    }

    private Map<String, Object> card(String label, String icon, Object count) {
        return Map.of("label", label, "icon", icon, "count", count == null ? 0 : count);
    }

    private void addSuggestion(Map<String, SearchSuggestion> matches, Object value, String detail) {
        String text = value == null ? "" : value.toString().trim();
        if (!text.isBlank() && !text.equalsIgnoreCase("NA") && !text.equalsIgnoreCase("N/A"))
            matches.putIfAbsent(text.toLowerCase(Locale.ROOT), new SearchSuggestion(text, detail));
    }

    private LocalDate parseDate(String value) {
        try { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException ignored) { return null; }
    }

    private String filterWhere(LocalDate startDate, LocalDate endDate, String status, String search, String client, String payment, List<Object> arguments) {
        List<String> clauses = new ArrayList<>();
        if (startDate != null) { clauses.add("`date` >= ?"); arguments.add(java.sql.Date.valueOf(startDate)); }
        if (endDate != null) { clauses.add("`date` <= ?"); arguments.add(java.sql.Date.valueOf(endDate)); }
        switch (status) {
            case "Inclusion" -> clauses.add("trim(coalesce(inclusion, '')) not in ('', 'NA', 'N/A')");
            case "Renewal" -> clauses.add("trim(coalesce(renewal, '')) not in ('', 'NA', 'N/A')");
            case "AIR Updates" -> clauses.add("trim(coalesce(air_name, '')) not in ('', 'NA', 'N/A')");
            case "SIT" -> clauses.add("trim(coalesce(sit, '')) not in ('', 'NA', 'N/A')");
            default -> { }
        }
        if (search != null && !search.isBlank()) {
            clauses.add("(lower(coalesce(cml_no, '')) like ? or lower(coalesce(company_name, '')) like ? or lower(coalesce(client_name, '')) like ? or lower(coalesce(client_email, '')) like ? or lower(coalesce(indian_standard, '')) like ? or lower(coalesce(product_name, '')) like ? or lower(coalesce(app_no, '')) like ?)");
            String term = "%" + search.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            for (int index = 0; index < 7; index++) arguments.add(term);
        }
        if (!client.isBlank()) { clauses.add("lower(trim(coalesce(client_name, ''))) = ?"); arguments.add(client.toLowerCase(Locale.ROOT)); }
        if (!payment.isBlank()) { clauses.add("lower(trim(coalesce(bis_pay, ''))) = ?"); arguments.add(payment.toLowerCase(Locale.ROOT)); }
        return clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses);
    }

    private String normalizedStatus(String status) {
        return status != null && List.of("Inclusion", "Renewal", "AIR Updates", "SIT").contains(status) ? status : "";
    }

    private String normalizedClient(String client) {
        List<String> clients = List.of("cocbang", "ERIC(ZHONGBANG)", "GHANSHYAM PATEL", "GRACE", "SATYA - TAPARRIA", "Satya Ganendra", "SHON RAJAN MANAVATH", "Suresh Panchal");
        return client != null && clients.contains(client) ? client : "";
    }

    private String normalizedPayment(String payment) {
        List<String> payments = List.of("Advance", "After Application", "After Nomination", "After License");
        return payment != null && payments.contains(payment) ? payment : "";
    }

    private List<Object> pageArguments(List<Object> filters, int size, int offset) {
        List<Object> arguments = new ArrayList<>(filters);
        arguments.add(size); arguments.add(offset);
        return arguments;
    }
}
