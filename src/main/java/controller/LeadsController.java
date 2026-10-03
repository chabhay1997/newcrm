package controller;

import dto.LeadMeetingReminderRequest;
import model.Lead;
import model.LeadMeetingReminder;
import model.User;
import repository.LeadRepository;
import repository.LeadFollowUpRepository;
import repository.LeadMeetingReminderRepository;
import repository.UserRepository;
import util.LeadStatus;
import util.LeadSource;
import util.LeadService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import model.quotations.BisIsiQuotation;
import repository.quotations.BisIsiQuotationRepository;
import model.quotations.BisFmcsQuotation;
import repository.quotations.BisFmcsQuotationRepository;
import model.quotations.CdscoQuotation;
import repository.quotations.CdscoQuotationRepository;
import model.quotations.BisCrsQuotation;
import repository.quotations.BisCrsQuotationRepository;
import model.quotations.CosmeticsQuotation;
import repository.quotations.CosmeticsQuotationRepository;
import model.quotations.BisWpcQuotation;
import model.quotations.BisSitQuotation;
import repository.quotations.BisWpcQuotationRepository;
import repository.quotations.BisSitQuotationRepository;
import model.quotations.BisLmpcQuotation;
import repository.quotations.BisLmpcQuotationRepository;
import model.quotations.BisDpiitQuotation;
import repository.quotations.BisDpiitQuotationRepository;
import model.quotations.DrugQuotation;
import repository.quotations.DrugQuotationRepository;
import model.quotations.CbIsiQuotation;
import repository.quotations.CbIsiQuotationRepository;
import model.quotations.CbFmcsQuotation;
import repository.quotations.CbFmcsQuotationRepository;
import model.quotations.CbRdsoQuotation;
import model.quotations.CbRdsoDetail;
import repository.quotations.CbRdsoQuotationRepository;
import repository.quotations.CbRdsoDetailRepository;
import model.quotations.RdsoQuotation;
import model.quotations.RdsoDetail;
import repository.quotations.RdsoQuotationRepository;
import repository.quotations.RdsoDetailRepository;
import model.quotations.ElectronicEprQuotation;
import repository.quotations.ElectronicEprQuotationRepository;
import model.quotations.PlasticEprQuotation;
import repository.quotations.PlasticEprQuotationRepository;
import model.quotations.BatteryEprQuotation;
import repository.quotations.BatteryEprQuotationRepository;
import model.quotations.BeeQuotation;
import model.quotations.BeeQuotationFee;
import repository.quotations.BeeQuotationRepository;
import repository.quotations.BeeQuotationFeeRepository;
import repository.quotations.SchemeXForeignQuotationRepository;
import service.quotation_pdfs.LaravelQuotationPdfClient;
import service.LeadPermissionService;

@Controller
public class LeadsController {

    private static final long INDIA_COUNTRY_ID = 101L;

    private final LeadRepository leadRepository;
    private final LeadFollowUpRepository leadFollowUpRepository;
    private final LeadMeetingReminderRepository leadMeetingReminderRepository;
    private final UserRepository userRepository;
    private final BisIsiQuotationRepository bisIsiQuotationRepository;
    private final BisFmcsQuotationRepository bisFmcsQuotationRepository;
    private final CdscoQuotationRepository cdscoQuotationRepository;
    private final BisCrsQuotationRepository bisCrsQuotationRepository;
    private final CosmeticsQuotationRepository cosmeticsQuotationRepository;
    private final BisWpcQuotationRepository bisWpcQuotationRepository;
    private final BisSitQuotationRepository bisSitQuotationRepository;
    private final BisLmpcQuotationRepository bisLmpcQuotationRepository;
    private final BisDpiitQuotationRepository bisDpiitQuotationRepository;
    private final DrugQuotationRepository drugQuotationRepository;
    private final CbIsiQuotationRepository cbIsiQuotationRepository;
    private final CbFmcsQuotationRepository cbFmcsQuotationRepository;
    private final CbRdsoQuotationRepository cbRdsoQuotationRepository;
    private final CbRdsoDetailRepository cbRdsoDetailRepository;
    private final RdsoQuotationRepository rdsoQuotationRepository;
    private final RdsoDetailRepository rdsoDetailRepository;
    private final ElectronicEprQuotationRepository electronicEprQuotationRepository;
    private final PlasticEprQuotationRepository plasticEprQuotationRepository;
    private final BatteryEprQuotationRepository batteryEprQuotationRepository;
    private final BeeQuotationRepository beeQuotationRepository;
    private final BeeQuotationFeeRepository beeQuotationFeeRepository;
    private final SchemeXForeignQuotationRepository schemeXForeignQuotationRepository;
    private final LaravelQuotationPdfClient laravelQuotationPdfClient;
    private final LeadPermissionService leadPermissionService;
    private final JdbcTemplate jdbcTemplate;

        public LeadsController(LeadRepository leadRepository, LeadFollowUpRepository leadFollowUpRepository,
            LeadMeetingReminderRepository leadMeetingReminderRepository, UserRepository userRepository, BisIsiQuotationRepository bisIsiQuotationRepository,
            BisFmcsQuotationRepository bisFmcsQuotationRepository, CdscoQuotationRepository cdscoQuotationRepository,
            BisCrsQuotationRepository bisCrsQuotationRepository, CosmeticsQuotationRepository cosmeticsQuotationRepository,
            BisWpcQuotationRepository bisWpcQuotationRepository, BisSitQuotationRepository bisSitQuotationRepository, BisLmpcQuotationRepository bisLmpcQuotationRepository,
            BisDpiitQuotationRepository bisDpiitQuotationRepository, DrugQuotationRepository drugQuotationRepository,
            CbIsiQuotationRepository cbIsiQuotationRepository, CbFmcsQuotationRepository cbFmcsQuotationRepository,
            CbRdsoQuotationRepository cbRdsoQuotationRepository, CbRdsoDetailRepository cbRdsoDetailRepository,
            RdsoQuotationRepository rdsoQuotationRepository, RdsoDetailRepository rdsoDetailRepository,
            ElectronicEprQuotationRepository electronicEprQuotationRepository,
            PlasticEprQuotationRepository plasticEprQuotationRepository,
            BatteryEprQuotationRepository batteryEprQuotationRepository,
            BeeQuotationRepository beeQuotationRepository, BeeQuotationFeeRepository beeQuotationFeeRepository,
            SchemeXForeignQuotationRepository schemeXForeignQuotationRepository,
            LaravelQuotationPdfClient laravelQuotationPdfClient,
            LeadPermissionService leadPermissionService,
            JdbcTemplate jdbcTemplate) {
        this.leadRepository = leadRepository;
        this.leadFollowUpRepository = leadFollowUpRepository;
        this.leadMeetingReminderRepository = leadMeetingReminderRepository;
        this.userRepository = userRepository;
        this.bisIsiQuotationRepository = bisIsiQuotationRepository;
        this.bisFmcsQuotationRepository = bisFmcsQuotationRepository;
        this.cdscoQuotationRepository = cdscoQuotationRepository;
        this.bisCrsQuotationRepository = bisCrsQuotationRepository;
        this.cosmeticsQuotationRepository = cosmeticsQuotationRepository;
        this.bisWpcQuotationRepository = bisWpcQuotationRepository;
        this.bisSitQuotationRepository = bisSitQuotationRepository;
        this.bisLmpcQuotationRepository = bisLmpcQuotationRepository;
        this.bisDpiitQuotationRepository = bisDpiitQuotationRepository;
        this.drugQuotationRepository = drugQuotationRepository;
        this.cbIsiQuotationRepository = cbIsiQuotationRepository;
        this.cbFmcsQuotationRepository = cbFmcsQuotationRepository;
        this.cbRdsoQuotationRepository = cbRdsoQuotationRepository;
        this.cbRdsoDetailRepository = cbRdsoDetailRepository;
        this.rdsoQuotationRepository = rdsoQuotationRepository;
        this.rdsoDetailRepository = rdsoDetailRepository;
        this.electronicEprQuotationRepository = electronicEprQuotationRepository;
        this.plasticEprQuotationRepository = plasticEprQuotationRepository;
        this.batteryEprQuotationRepository = batteryEprQuotationRepository;
        this.beeQuotationRepository = beeQuotationRepository;
        this.beeQuotationFeeRepository = beeQuotationFeeRepository;
        this.schemeXForeignQuotationRepository = schemeXForeignQuotationRepository;
        this.laravelQuotationPdfClient = laravelQuotationPdfClient;
        this.leadPermissionService = leadPermissionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static boolean matchesLeadLocationFilter(
            Lead lead,
            String selectedStateId,
            String selectedCityId,
            String selectedStateName,
            String selectedCityName,
            Map<String, String> cityStateMap) {
        if (lead == null) {
            return false;
        }

        boolean stateMatches = true;
        if (selectedStateId != null && !selectedStateId.isBlank()) {
            String normalizedStateId = selectedStateId.trim();
            stateMatches = valueMatches(lead.getStateId(), normalizedStateId)
                    || cityBelongsToState(lead.getCityId(), normalizedStateId, cityStateMap)
                || textEquals(lead.getStateId(), selectedStateName)
                || (!hasNumericLocationId(lead.getStateId())
                    && !hasNumericLocationId(lead.getCityId())
                    && textContainsAny(selectedStateName, lead.getAddress(), lead.getMessage(), lead.getRequirements(), lead.getRemarks()));
        }

        boolean cityMatches = true;
        if (selectedCityId != null && !selectedCityId.isBlank()) {
            String normalizedCityId = selectedCityId.trim();
            cityMatches = valueMatches(lead.getCityId(), normalizedCityId)
                    || textEquals(lead.getCityId(), selectedCityName)
                    || (!hasNumericLocationId(lead.getCityId())
                        && textContainsAny(selectedCityName, lead.getAddress(), lead.getMessage(), lead.getRequirements(), lead.getRemarks()));
        }

        return stateMatches && cityMatches;
    }

    private static boolean valueMatches(String leadValue, String selectedValue) {
        return selectedValue != null && !selectedValue.isBlank()
                && leadValue != null
                && leadValue.trim().equals(selectedValue.trim());
    }

    private static boolean cityBelongsToState(String leadCityId, String selectedStateId, Map<String, String> cityStateMap) {
        if (leadCityId == null || leadCityId.isBlank() || selectedStateId == null || selectedStateId.isBlank()) {
            return false;
        }
        String mappedStateId = cityStateMap == null ? null : cityStateMap.get(leadCityId.trim());
        if (mappedStateId == null && cityStateMap != null) {
            mappedStateId = cityStateMap.get("name:" + normalizeText(leadCityId));
        }
        return mappedStateId != null && mappedStateId.trim().equals(selectedStateId.trim());
    }

    private static boolean textEquals(String value, String query) {
        return value != null && !value.isBlank() && query != null && !query.isBlank()
                && normalizeText(value).equals(normalizeText(query));
    }

    private static boolean hasNumericLocationId(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(value.trim()) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean textContainsAny(String query, String... values) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalizedQuery = normalizeText(query);
        for (String value : values) {
            if (value != null && !value.isBlank() && normalizeText(value).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static final Map<Long, String> CERTIFICATE_TYPE_LABELS = Map.ofEntries(
            Map.entry(1L, "Electronic EPR"),
            Map.entry(2L, "Plastic EPR"),
            Map.entry(3L, "Battery EPR"),
            Map.entry(4L, "BIS CRS"),
            Map.entry(5L, "LMPC"),
            Map.entry(6L, "BIS-ISI Certification"),
            Map.entry(7L, "BIS FMCS"),
            Map.entry(8L, "WPC"),
            Map.entry(9L, "BEE Certification"),
            Map.entry(10L, "Scheme-X Domestic"),
            Map.entry(11L, "Scheme-X Foreign"),
            Map.entry(12L, "RDSO Quotation"),
            Map.entry(13L, "SIT Quotation"),
            Map.entry(14L, "Drug Quotation"),
            Map.entry(15L, "CDSCO Registration"),
            Map.entry(16L, "Cosmetics"),
            Map.entry(17L, "DPIIT"),
            Map.entry(18L, "CB-ISI Certification"),
            Map.entry(19L, "CB-FMCS"),
            Map.entry(20L, "CB-RDSO"),
            Map.entry(21L, "General Quotation")
    );
        private static final List<String> ALL_QUOTATION_TYPES = List.of(
            "Electronic EPR", "Plastic EPR", "Battery EPR", "BIS CRS", "LMPC",
            "BIS-ISI Certification", "BEE Certification", "BIS FMCS", "WPC",
            "General Quotation", "Scheme-X Domestic", "Scheme-X Foreign",
            "RDSO Quotation", "SIT Quotation", "Drug Quotation",
            "CDSCO Registration", "Cosmetics", "DPIIT", "CB-ISI Certification", "CB-FMCS", "CB-RDSO"
        );

    private static Long certificateTypeIdForLabel(String label) {
        if (label == null) return null;
        String normalized = label.trim().replaceAll("\\s+", " ").toLowerCase();
        for (Map.Entry<Long, String> entry : CERTIFICATE_TYPE_LABELS.entrySet()) {
            if (entry.getValue().toLowerCase().equals(normalized)) return entry.getKey();
        }
        return switch (normalized) {
            case "bee registration" -> 9L;
            case "cosmetics registration" -> 16L;
            case "cb - isi" -> 18L;
            case "cb - fmcs" -> 19L;
            case "cb - rdso" -> 20L;
            default -> null;
        };
    }

    @GetMapping("/leads")
    public String showLeads(
            Model model,
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) List<String> quotationTypes,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String countryId,
            @RequestParam(required = false) String stateId,
            @RequestParam(required = false) String cityId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<String> quotationTypesFilter = (quotationTypes == null || quotationTypes.isEmpty()) ? null : quotationTypes;
        List<Long> certificateTypeIdsFilter = quotationTypesFilter == null ? null : quotationTypesFilter.stream()
            .map(LeadsController::certificateTypeIdForLabel)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

        java.time.LocalDateTime startDateTime = null;
        java.time.LocalDateTime endDateTime = null;
        try {
            if (startDate != null && !startDate.isEmpty()) {
                startDateTime = java.time.LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isEmpty()) {
                endDateTime = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
            }
        } catch (Exception ignored) { }

        String filterCountryId = (countryId == null || countryId.isBlank()) ? null : countryId.trim();
        String filterStateId = (stateId == null || stateId.isBlank()) ? null : stateId.trim();
        String filterCityId = (cityId == null || cityId.isBlank()) ? null : cityId.trim();

        String selectedStateName = null;
        String selectedCityName = null;
        if (filterStateId != null && !filterStateId.isBlank()) {
            try {
                selectedStateName = jdbcTemplate.queryForObject("SELECT name FROM states WHERE id = ?", String.class, Long.parseLong(filterStateId));
            } catch (Exception ignored) {
                selectedStateName = null;
            }
        }
        if (filterCityId != null && !filterCityId.isBlank()) {
            try {
                selectedCityName = jdbcTemplate.queryForObject("SELECT name FROM cities WHERE id = ?", String.class, Long.parseLong(filterCityId));
            } catch (Exception ignored) {
                selectedCityName = null;
            }
        }

        String effectiveCountryId = (filterCountryId == null || filterCountryId.isBlank()) ? String.valueOf(INDIA_COUNTRY_ID) : filterCountryId;
        List<Map<String, Object>> countries = jdbcTemplate.queryForList("SELECT id, name FROM countries WHERE id = ? ORDER BY name", INDIA_COUNTRY_ID);
        List<Map<String, Object>> states = jdbcTemplate.queryForList("SELECT id, name, country_id FROM states WHERE country_id = ? ORDER BY name", INDIA_COUNTRY_ID);
        List<Map<String, Object>> cities = jdbcTemplate.queryForList(
                "SELECT c.id, c.name, c.state_id FROM cities c INNER JOIN states s ON s.id = c.state_id WHERE s.country_id = ? ORDER BY c.name",
                INDIA_COUNTRY_ID);

        Map<String, String> cityStateMap = new LinkedHashMap<>();
        for (Map<String, Object> city : cities) {
            Object cityIdValue = city.get("id");
            Object stateIdValue = city.get("state_id");
            if (cityIdValue != null && stateIdValue != null) {
                String mappedStateId = String.valueOf(stateIdValue);
                cityStateMap.put(String.valueOf(cityIdValue), mappedStateId);
                Object cityNameValue = city.get("name");
                if (cityNameValue != null) {
                    String cityNameKey = "name:" + normalizeText(String.valueOf(cityNameValue));
                    cityStateMap.merge(cityNameKey, mappedStateId,
                            (existingStateId, newStateId) -> existingStateId.equals(newStateId) ? existingStateId : "");
                }
            }
        }

        String selectedCountryName = null;
        if (effectiveCountryId != null && !effectiveCountryId.isBlank()) {
            try {
                selectedCountryName = jdbcTemplate.queryForObject("SELECT name FROM countries WHERE id = ?", String.class, Long.parseLong(effectiveCountryId));
            } catch (Exception ignored) {
                selectedCountryName = null;
            }
        }

        boolean canLeadRead = leadPermissionService.has(authentication, "all_lead", "read");
        boolean canLeadEdit = leadPermissionService.has(authentication, "all_lead", "edit");
        List<Lead> leads = canLeadRead
            ? leadRepository.findByFilters(status, sourceId, quotationTypesFilter, certificateTypeIdsFilter, userId, startDateTime, endDateTime, filterCountryId, null, null)
            : new java.util.ArrayList<>();
        String currentEmail = authentication == null ? "" : authentication.getName();
        Integer currentRoleId = userRepository.findByEmailIgnoreCase(currentEmail)
            .map(User::getRoleId)
            .orElse(null);
        List<String> keywordPatterns = List.of(
            "cdsco", "cos", "md 14", "md 15", "md-15", "diagnostics", "md-13", "md 41",
            "md-42", "md 42", "md 3", "md 5", "md 12", "md 13", "md 7", "md 9",
            "md 8", "md 10", "md 4", "md 6", "medical", "cosmetic", "drug");

        if ("shivam@evtlindia.com".equalsIgnoreCase(currentEmail)) {
            java.time.LocalDate start = java.time.LocalDate.of(2025, 1, 15);
            java.time.LocalDate end = java.time.LocalDate.of(2025, 2, 15);
            leads.removeIf(lead -> lead.getCreatedAt() == null
                || !lead.getCreatedAt().toLocalDate().isAfter(start)
                || !lead.getCreatedAt().toLocalDate().isBefore(end));
        }

        if (List.of("anshu@evtlindia.com", "sujata@evtlindia.com", "vinod@evtlindia.in")
            .stream().anyMatch(email -> email.equalsIgnoreCase(currentEmail))) {
            leads.removeIf(lead -> !containsAnyLeadKeyword(lead, keywordPatterns));
        } else if (!Integer.valueOf(1).equals(currentRoleId)) {
            leads.removeIf(lead -> !containsAnyLeadKeyword(lead, List.of("medical"))
                && containsAnyLeadKeyword(lead, keywordPatterns));
        }

        for (Lead lead : leads) {
            Long certificateTypeId = lead.getCertificateTypeId();
            String quotationLabel = certificateTypeId == null ? null : CERTIFICATE_TYPE_LABELS.get(certificateTypeId);
            lead.setSelectedQuotationTypeLabel(quotationLabel != null ? quotationLabel : lead.getQuotationType());
        }
        Map<Long, String> latestFollowups = new java.util.HashMap<>();
        if (!leads.isEmpty()) {
            List<Long> leadIds = leads.stream().map(Lead::getId).toList();
            for (LeadFollowUpRepository.LatestFollowUpSummary followup
                    : leadFollowUpRepository.findLatestCommentsByLeadIds(leadIds)) {
                latestFollowups.put(followup.getLeadId(), followup.getReason());
            }
        }
        java.util.Set<Long> leadsWithFollowupComments = canLeadRead
            ? new java.util.HashSet<>(leadFollowUpRepository.findLeadIdsWithComments())
            : new java.util.HashSet<>();

        java.util.Set<Long> creatorIds = new java.util.HashSet<>();
        for (Lead lead : leads) {
            if (lead.getCreatedBy() != null) creatorIds.add(lead.getCreatedBy());
            if (lead.getAssignTo() != null) creatorIds.add(lead.getAssignTo());
        }
        java.util.Map<Long, String> creatorNames = new java.util.HashMap<>();
        if (!creatorIds.isEmpty()) {
            for (User u : userRepository.findAllById(creatorIds)) {
                creatorNames.put(u.getId(), u.getName());
            }
        }
        for (Lead lead : leads) {
            if (lead.getCreatedBy() != null) {
                lead.setCreatedByName(creatorNames.getOrDefault(lead.getCreatedBy(), "Unknown"));
            }
            if (lead.getAssignTo() != null) {
                lead.setAssignToName(creatorNames.getOrDefault(lead.getAssignTo(), "Unknown"));
            }
        }

        String locationStateName = selectedStateName;
        String locationCityName = selectedCityName;
        leads = leads.stream()
                .filter(lead -> matchesLeadLocationFilter(lead, filterStateId, filterCityId, locationStateName, locationCityName, cityStateMap))
                .toList();

        // Status counts for pie chart (computed from the filtered lead list)
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (LeadStatus s : LeadStatus.values()) statusCounts.put(s.getLabel(), 0L);
        for (Lead l : leads) {
            String label = LeadStatus.labelOf(l.getStatus());
            if (statusCounts.containsKey(label)) {
                statusCounts.put(label, statusCounts.get(label) + 1);
            }
        }

        // Source counts for pie chart (computed from the filtered lead list)
        Map<String, Long> sourceCounts = new LinkedHashMap<>();
        for (LeadSource s : LeadSource.values()) sourceCounts.put(s.getLabel(), 0L);
        for (Lead l : leads) {
            String label = LeadSource.labelOf(l.getSourceId());
            if (sourceCounts.containsKey(label)) {
                sourceCounts.put(label, sourceCounts.get(label) + 1);
            }
        }

        // Weekly momentum: New vs Converted leads for the current week (Mon-Sun) and % change vs previous week
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate mondayThisWeek = today.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate mondayLastWeek = mondayThisWeek.minusWeeks(1);

        long[] newLeadsByDay = new long[7];
        long[] convertedLeadsByDay = new long[7];
        long thisWeekTotal = 0;
        long lastWeekTotal = 0;

        for (Lead l : leads) {
            if (l.getCreatedAt() == null) continue;
            java.time.LocalDate createdDate = l.getCreatedAt().toLocalDate();
            boolean isConverted = "4".equals(l.getStatus());

            if (!createdDate.isBefore(mondayThisWeek) && createdDate.isBefore(mondayThisWeek.plusWeeks(1))) {
                int dayIndex = createdDate.getDayOfWeek().getValue() - 1; // Mon=0 ... Sun=6
                newLeadsByDay[dayIndex]++;
                if (isConverted) convertedLeadsByDay[dayIndex]++;
                thisWeekTotal++;
            } else if (!createdDate.isBefore(mondayLastWeek) && createdDate.isBefore(mondayThisWeek)) {
                lastWeekTotal++;
            }
        }

        double weekOverWeekChange = lastWeekTotal == 0
                ? (thisWeekTotal > 0 ? 100.0 : 0.0)
                : ((thisWeekTotal - lastWeekTotal) * 100.0) / lastWeekTotal;

        boolean canLeadDashboard = leadPermissionService.has(authentication, "leads_dashboard", "read");
        model.addAttribute("momentumDayLabels", canLeadDashboard ? List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") : List.of());
        model.addAttribute("momentumNewLeads", canLeadDashboard ? newLeadsByDay : new long[0]);
        model.addAttribute("momentumConvertedLeads", canLeadDashboard ? convertedLeadsByDay : new long[0]);
        model.addAttribute("momentumWeekChange", canLeadDashboard ? Math.round(weekOverWeekChange * 10.0) / 10.0 : 0.0);

        model.addAttribute("activePage", "leads");
        model.addAttribute("totalLeads", (long) leads.size());
        model.addAttribute("leads", leads);
        model.addAttribute("latestFollowups", latestFollowups);
        model.addAttribute("leadsWithFollowupComments", leadsWithFollowupComments);
        model.addAttribute("allStatuses", LeadStatus.values());
        model.addAttribute("allSources", LeadSource.values());
        model.addAttribute("allServices", LeadService.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSourceId", sourceId);
        model.addAttribute("allCountries", countries);
        model.addAttribute("allStates", states);
        model.addAttribute("allCities", cities);
        model.addAttribute("selectedCountryId", effectiveCountryId);
        model.addAttribute("selectedCountryName", selectedCountryName);
        model.addAttribute("selectedStateId", filterStateId);
        model.addAttribute("selectedStateName", selectedStateName);
        model.addAttribute("selectedCityId", filterCityId);
        model.addAttribute("selectedCityName", selectedCityName);

        String activeFilterLabel = "All Leads";
        if (status != null && !status.isEmpty()) {
            activeFilterLabel = LeadStatus.labelOf(status);
        } else if (sourceId != null) {
            activeFilterLabel = LeadSource.labelOf(sourceId);
        } else if (userId != null) {
            activeFilterLabel = "Assigned Leads";
        }
        model.addAttribute("activeFilterLabel", activeFilterLabel);
        model.addAttribute("selectedStatusId", status);
        model.addAttribute("statusCounts", canLeadDashboard ? statusCounts : Map.of());
        model.addAttribute("sourceCounts", canLeadDashboard ? sourceCounts : Map.of());
        model.addAttribute("allQuotationTypes", ALL_QUOTATION_TYPES);
        model.addAttribute("certificateTypeLabels", CERTIFICATE_TYPE_LABELS);
        model.addAttribute("selectedQuotationTypes", quotationTypesFilter);
        model.addAttribute("salesUsers", canLeadEdit ? userRepository.findByRoleNameContainingIgnoreCase("sales") : List.of());
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);

        model.addAttribute("isSuperAdmin", hasSuperAdminAccess());
        model.addAttribute("canLeadWrite", leadPermissionService.has(authentication, "all_lead", "write"));
        model.addAttribute("canLeadEdit", canLeadEdit);
        model.addAttribute("canLeadDelete", leadPermissionService.has(authentication, "all_lead", "delete"));
        model.addAttribute("canLeadDeleteButton",
            leadPermissionService.has(authentication, "leads_delete", "delete")
                || leadPermissionService.has(authentication, "all_lead", "delete"));
        model.addAttribute("canLeadExcel", leadPermissionService.has(authentication, "all_lead", "excel"));
        model.addAttribute("canLeadDashboard", canLeadDashboard);

        return "leads/leads";
    }

    private static boolean containsAnyLeadKeyword(Lead lead, List<String> keywords) {
        String searchableText = String.join(" ",
                lead.getProductName() == null ? "" : lead.getProductName(),
                lead.getRequirements() == null ? "" : lead.getRequirements(),
                lead.getMessage() == null ? "" : lead.getMessage())
                .toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(searchableText::contains);
    }

    @GetMapping("/leads/momentum-data")
    @ResponseBody
    public Map<String, Object> getMomentumData(
            @RequestParam(defaultValue = "all") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "year") String period) {
    List<Lead> allLeads = leadRepository.findAll();

        if ("all".equalsIgnoreCase(range) || "custom".equalsIgnoreCase(range)) {
            java.time.LocalDate start = null;
            java.time.LocalDate end = null;
            if ("custom".equalsIgnoreCase(range)) {
                try {
                    start = java.time.LocalDate.parse(startDate);
                    end = java.time.LocalDate.parse(endDate);
                } catch (Exception ignored) {
                    return Map.of("labels", List.of(), "newLeads", List.of(), "convertedLeads", List.of(), "weekChange", 0);
                }
            }

            java.util.TreeMap<String, long[]> buckets = new java.util.TreeMap<>();
            for (Lead lead : allLeads) {
                if (lead.getCreatedAt() == null) continue;
                java.time.LocalDate date = lead.getCreatedAt().toLocalDate();
                if (start != null && (date.isBefore(start) || date.isAfter(end))) continue;
                String key = "year".equalsIgnoreCase(period)
                        ? String.valueOf(date.getYear())
                        : ("month".equalsIgnoreCase(period) ? date.toString().substring(0, 7) : date.toString());
                long[] counts = buckets.computeIfAbsent(key, ignored -> new long[2]);
                counts[0]++;
                if ("4".equals(lead.getStatus())) counts[1]++;
            }

            List<String> labels = new java.util.ArrayList<>(buckets.keySet());
            List<Long> newCounts = new java.util.ArrayList<>();
            List<Long> convertedCounts = new java.util.ArrayList<>();
            for (long[] counts : buckets.values()) {
                newCounts.add(counts[0]);
                convertedCounts.add(counts[1]);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("labels", labels);
            result.put("newLeads", newCounts);
            result.put("convertedLeads", convertedCounts);
            result.put("weekChange", 0.0);
            return result;
        }

    java.time.LocalDate today = java.time.LocalDate.now();
    List<String> labels;
    long[] newCounts;
    long[] convertedCounts;
    long currentTotal = 0;
    long previousTotal = 0;

    if ("monthly".equals(range)) {
        // Current month split into weeks (Week 1..Week 5), vs previous month total
        java.time.LocalDate monthStart = today.withDayOfMonth(1);
        java.time.LocalDate monthEnd = monthStart.plusMonths(1);
        java.time.LocalDate prevMonthStart = monthStart.minusMonths(1);

        int weeksInMonth = (int) Math.ceil((monthStart.lengthOfMonth() + monthStart.getDayOfWeek().getValue() - 1) / 7.0);
        labels = new java.util.ArrayList<>();
        for (int i = 1; i <= weeksInMonth; i++) labels.add("Week " + i);
        newCounts = new long[weeksInMonth];
        convertedCounts = new long[weeksInMonth];

        for (Lead l : allLeads) {
            if (l.getCreatedAt() == null) continue;
            java.time.LocalDate d = l.getCreatedAt().toLocalDate();
            boolean isConverted = "4".equals(l.getStatus());
            if (!d.isBefore(monthStart) && d.isBefore(monthEnd)) {
                int weekIndex = (d.getDayOfMonth() - 1 + monthStart.getDayOfWeek().getValue() - 1) / 7;
                weekIndex = Math.min(weekIndex, weeksInMonth - 1);
                newCounts[weekIndex]++;
                if (isConverted) convertedCounts[weekIndex]++;
                currentTotal++;
            } else if (!d.isBefore(prevMonthStart) && d.isBefore(monthStart)) {
                previousTotal++;
            }
        }
    } else if ("quarterly".equals(range)) {
        // Current quarter by month, vs previous quarter total
        int currentMonth = today.getMonthValue();
        int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
        java.time.LocalDate quarterStart = java.time.LocalDate.of(today.getYear(), quarterStartMonth, 1);
        java.time.LocalDate quarterEnd = quarterStart.plusMonths(3);
        java.time.LocalDate prevQuarterStart = quarterStart.minusMonths(3);

        labels = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) labels.add(quarterStart.plusMonths(i).getMonth().toString().substring(0, 3));
        newCounts = new long[3];
        convertedCounts = new long[3];

        for (Lead l : allLeads) {
            if (l.getCreatedAt() == null) continue;
            java.time.LocalDate d = l.getCreatedAt().toLocalDate();
            boolean isConverted = "4".equals(l.getStatus());
            if (!d.isBefore(quarterStart) && d.isBefore(quarterEnd)) {
                int monthIndex = (d.getYear() - quarterStart.getYear()) * 12 + d.getMonthValue() - quarterStart.getMonthValue();
                newCounts[monthIndex]++;
                if (isConverted) convertedCounts[monthIndex]++;
                currentTotal++;
            } else if (!d.isBefore(prevQuarterStart) && d.isBefore(quarterStart)) {
                previousTotal++;
            }
        }
    } else if ("yearly".equals(range)) {
        // Current year by month, vs previous year total
        java.time.LocalDate yearStart = java.time.LocalDate.of(today.getYear(), 1, 1);
        java.time.LocalDate yearEnd = yearStart.plusYears(1);
        java.time.LocalDate prevYearStart = yearStart.minusYears(1);

        labels = new java.util.ArrayList<>();
        for (int i = 1; i <= 12; i++) labels.add(java.time.Month.of(i).toString().substring(0, 3));
        newCounts = new long[12];
        convertedCounts = new long[12];

        for (Lead l : allLeads) {
            if (l.getCreatedAt() == null) continue;
            java.time.LocalDate d = l.getCreatedAt().toLocalDate();
            boolean isConverted = "4".equals(l.getStatus());
            if (!d.isBefore(yearStart) && d.isBefore(yearEnd)) {
                int monthIndex = d.getMonthValue() - 1;
                newCounts[monthIndex]++;
                if (isConverted) convertedCounts[monthIndex]++;
                currentTotal++;
            } else if (!d.isBefore(prevYearStart) && d.isBefore(yearStart)) {
                previousTotal++;
            }
        }
    } else {
        // weekly (default): Mon-Sun current week, vs previous week
        java.time.LocalDate mondayThisWeek = today.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate mondayLastWeek = mondayThisWeek.minusWeeks(1);
        labels = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        newCounts = new long[7];
        convertedCounts = new long[7];

        for (Lead l : allLeads) {
            if (l.getCreatedAt() == null) continue;
            java.time.LocalDate d = l.getCreatedAt().toLocalDate();
            boolean isConverted = "4".equals(l.getStatus());
            if (!d.isBefore(mondayThisWeek) && d.isBefore(mondayThisWeek.plusWeeks(1))) {
                int dayIndex = d.getDayOfWeek().getValue() - 1;
                newCounts[dayIndex]++;
                if (isConverted) convertedCounts[dayIndex]++;
                currentTotal++;
            } else if (!d.isBefore(mondayLastWeek) && d.isBefore(mondayThisWeek)) {
                previousTotal++;
            }
        }
    }

    double change = previousTotal == 0
            ? (currentTotal > 0 ? 100.0 : 0.0)
            : ((currentTotal - previousTotal) * 100.0) / previousTotal;

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("labels", labels);
    result.put("newLeads", newCounts);
    result.put("convertedLeads", convertedCounts);
    result.put("weekChange", Math.round(change * 10.0) / 10.0);
    return result;
}

    @PostMapping("/leads/add")
    public String addLead(
            @ModelAttribute Lead lead,
            @RequestParam(name = "isName", required = false) String clientName,
            @RequestParam(name = "companyName", required = false) String companyName) {
        lead.setIsName(clientName);
        lead.setCompanyName(companyName);
        if (lead.getIsDeleted() == null) {
            lead.setIsDeleted(false);
        }
        leadRepository.save(lead);
        return "redirect:/leads";
    }

    @PostMapping("/leads/update/{id}")
    public String updateLead(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @ModelAttribute Lead lead,
            @RequestParam(name = "isName", required = false) String clientName,
            @RequestParam(name = "companyName", required = false) String companyName) {
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Lead not found"));

        existingLead.setIsName(clientName);
        existingLead.setCompanyName(companyName);
        existingLead.setEmail(lead.getEmail());
        existingLead.setPhone(lead.getPhone());
        existingLead.setProductName(lead.getProductName());
        existingLead.setServicesId(lead.getServicesId());
        existingLead.setCountryId(lead.getCountryId());
        existingLead.setStateId(lead.getStateId());
        existingLead.setCityId(lead.getCityId());
        existingLead.setCertificateTypeId(lead.getCertificateTypeId());
        existingLead.setSourceId(lead.getSourceId());
        existingLead.setStatus(lead.getStatus());
        existingLead.setLeadDate(lead.getLeadDate());
        existingLead.setAddress(lead.getAddress());
        existingLead.setRequirements(lead.getRequirements());
        existingLead.setRemarks(lead.getRemarks());

        leadRepository.save(existingLead);
        return "redirect:/leads";
    }

    @PostMapping("/leads/delete/{id}")
    public String deleteLead(@org.springframework.web.bind.annotation.PathVariable Long id) {
        if (!hasSuperAdminAccess()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Super Admin access only");
        }
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Lead not found"));
        lead.setIsDeleted(true);
        leadRepository.save(lead);
        return "redirect:/leads";
    }

    @PostMapping("/leads/{id}/delete-ajax")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> deleteLeadAjax(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (!hasSuperAdminAccess()) {
            response.put("success", false);
            response.put("message", "Super Admin access only");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        }
        try {
            Lead lead = leadRepository.findById(id).orElse(null);
            if (lead == null) {
                response.put("success", false);
                response.put("message", "Lead not found");
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
            }
            lead.setIsDeleted(true);
            leadRepository.save(lead);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    private boolean hasSuperAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @PostMapping("/leads/{id}/meeting-reminders")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> createMeetingReminder(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody LeadMeetingReminderRequest request,
            Authentication authentication) {
        Optional<User> user = findAuthenticatedUser(authentication);
        if (user.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "Sign in to schedule a reminder"));

        Optional<Lead> lead = leadRepository.findById(id);
        if (lead.isEmpty()) return ResponseEntity.notFound().build();
        if (request == null || request.getRemindAt() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Choose a reminder date and time"));
        }
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty() || message.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a reminder message of up to 500 characters"));
        }

        LeadMeetingReminder reminder = new LeadMeetingReminder();
        reminder.setLeadId(id);
        reminder.setUserId(user.get().getId());
        reminder.setRemindAt(request.getRemindAt());
        reminder.setMessage(message);
        reminder = leadMeetingReminderRepository.save(reminder);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", reminder.getId());
        response.put("remindAt", reminder.getRemindAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leads/meeting-reminders/due")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> getDueMeetingReminders(Authentication authentication) {
        Optional<User> user = findAuthenticatedUser(authentication);
        if (user.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "Sign in to check reminders"));

        List<Map<String, Object>> dueReminders = leadMeetingReminderRepository
                .findByUserIdAndDismissedAtIsNullAndRemindAtLessThanEqualOrderByRemindAtAsc(
                        user.get().getId(), LocalDateTime.now())
                .stream()
                .map(reminder -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", reminder.getId());
                    item.put("remindAt", reminder.getRemindAt());
                    item.put("message", reminder.getMessage());
                    leadRepository.findById(reminder.getLeadId()).ifPresent(lead -> {
                        item.put("leadId", lead.getId());
                        item.put("clientName", lead.getIsName());
                        item.put("companyName", lead.getCompanyName());
                        item.put("phone", lead.getPhone() != null ? lead.getPhone() : lead.getCompanyMobile());
                        item.put("email", lead.getEmail() != null ? lead.getEmail() : lead.getOfficialMailId());
                    });
                    return item;
                }).toList();
        return ResponseEntity.ok(dueReminders);
    }

    @PostMapping("/leads/meeting-reminders/{reminderId}/dismiss")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> dismissMeetingReminder(
            @org.springframework.web.bind.annotation.PathVariable Long reminderId,
            Authentication authentication) {
        Optional<User> user = findAuthenticatedUser(authentication);
        if (user.isEmpty()) return ResponseEntity.status(401).body(Map.of("success", false));

        Optional<LeadMeetingReminder> reminder = leadMeetingReminderRepository
                .findByIdAndUserIdAndDismissedAtIsNull(reminderId, user.get().getId());
        if (reminder.isEmpty()) return ResponseEntity.notFound().build();

        reminder.get().setDismissedAt(LocalDateTime.now());
        leadMeetingReminderRepository.save(reminder.get());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Optional<User> findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName());
    }

    @GetMapping("/leads/{id}/json")
    @org.springframework.web.bind.annotation.ResponseBody
    public Lead getLeadJson(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead != null && lead.getCreatedBy() != null) {
            userRepository.findById(lead.getCreatedBy())
                    .ifPresent(user -> lead.setCreatedByName(user.getName()));
        }
        return lead;
    }

    @GetMapping("/leads/{id}/followups")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getLeadFollowups(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> followups = leadFollowUpRepository.findByLeadId(id).stream().map(item -> {
                Map<String, Object> followup = new LinkedHashMap<>();
                followup.put("id", item.getId());
                followup.put("reason", item.getReason());
                followup.put("followupType", item.getFollowupType());
                followup.put("nextFollowupDate", item.getNextFollowupDate());
                followup.put("nextFollowupTime", item.getNextFollowupTime());
                followup.put("createdAt", item.getCreatedAt());
                followup.put("userName", item.getUserName());
                return followup;
            }).toList();
            response.put("success", true);
            response.put("followups", followups);
        } catch (Exception exception) {
            response.put("success", false);
            response.put("message", "Follow-up history is unavailable");
        }
        return response;
    }

    @GetMapping("/leads/{id}/isi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getIsiQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisIsiQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisIsiQuotationRepository.findById(revisionId).filter(q -> id.equals(q.getLeadId())).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/isi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveIsiQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisIsiQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.setLeadId(id);
        if (quotation.getId() != null && bisIsiQuotationRepository.findById(quotation.getId()).filter(q -> id.equals(q.getLeadId())).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.getId() == null) quotation.setReferenceNo("REF-" + id + "-R" + (bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1));
        bisIsiQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{leadId}/isi-quotation/{quotationId}/pdf")
    public ResponseEntity<byte[]> downloadIsiQuotationPdf(
            @PathVariable Long leadId,
            @PathVariable Long quotationId) {
        Lead lead = leadRepository.findById(leadId).orElse(null);
        BisIsiQuotation quotation = bisIsiQuotationRepository.findById(quotationId)
                .filter(item -> leadId.equals(item.getLeadId()))
                .orElse(null);

        if (lead == null || quotation == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] document = laravelQuotationPdfClient.download(leadId, "bis_isi", quotation);
            String filename = safeFilename(
                    "ISI-Quotation-" + (quotation.getReferenceNo() == null ? quotationId : quotation.getReferenceNo()) + ".pdf");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(document);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leads/{leadId}/quotation/{quotationType}/{quotationId}/pdf")
    public ResponseEntity<byte[]> downloadQuotationPdf(
            @PathVariable Long leadId,
            @PathVariable String quotationType,
            @PathVariable Long quotationId) {
        Lead lead = leadRepository.findById(leadId).orElse(null);
        Object quotation = findQuotation(quotationType, leadId, quotationId);
        if (lead == null || quotation == null) return ResponseEntity.notFound().build();
        if ("cb_rdso".equals(quotationType) && quotation instanceof CbRdsoQuotation cbRdso) {
            cbRdso.details = cbRdsoDetailRepository.findByCbRdsoFkIdOrderByIdAsc(cbRdso.id);
        }
        try {
            byte[] document = quotation instanceof CbRdsoQuotation cbRdso
                    ? laravelQuotationPdfClient.downloadCbRdso(leadId, cbRdso)
                    : laravelQuotationPdfClient.download(leadId, quotationType, quotation);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Quotation-" + safeFilename(quotationType) + "-" + quotationId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(document);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Object findQuotation(String type, Long leadId, Long quotationId) {
        return switch (type) {
            case "electronic_epr" -> electronicEprQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "plastic_epr" -> plasticEprQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "battery_epr" -> batteryEprQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "bis_crs" -> bisCrsQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "lmpc" -> bisLmpcQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "bis_isi" -> bisIsiQuotationRepository.findById(quotationId).filter(q -> leadId.equals(q.getLeadId())).orElse(null);
            case "bis_fmcs" -> bisFmcsQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "wpc" -> bisWpcQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "sit" -> bisSitQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "dpiit" -> bisDpiitQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "drug" -> drugQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "cb_isi" -> cbIsiQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "cb_fmcs" -> cbFmcsQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "cb_rdso" -> cbRdsoQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "rdso" -> rdsoQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "cdsco" -> cdscoQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "cosmetics" -> cosmeticsQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "bee" -> beeQuotationRepository.findById(quotationId).filter(q -> belongsToLead(q, leadId)).orElse(null);
            case "scheme_x_foreign" -> schemeXForeignQuotationRepository.findById(quotationId).filter(q -> leadId.equals(q.leadId)).orElse(null);
            default -> null;
        };
    }

    private boolean belongsToLead(Object quotation, Long leadId) {
        try {
            for (String getter : List.of("getLeadId", "getLeadFkId")) {
                try {
                    Object value = quotation.getClass().getMethod(getter).invoke(quotation);
                    return leadId.equals(value);
                } catch (NoSuchMethodException ignored) { }
            }
            for (String fieldName : List.of("leadId", "leadFkId")) {
                try {
                    Object value = quotation.getClass().getField(fieldName).get(quotation);
                    return leadId.equals(value);
                } catch (NoSuchFieldException ignored) { }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private String safeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    @GetMapping("/leads/{id}/fmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getFmcsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisFmcsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisFmcsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/fmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveFmcsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisFmcsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisFmcsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisFmcsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cdsco-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCdscoQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cdscoQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cdscoQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cdsco-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCdscoQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CdscoQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && cdscoQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        cdscoQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cdscoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/crs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCrsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisCrsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisCrsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/crs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCrsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisCrsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisCrsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisCrsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisCrsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cosmetics-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCosmeticsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cosmeticsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cosmeticsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cosmetics-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCosmeticsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CosmeticsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && cosmeticsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        cosmeticsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cosmeticsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/wpc-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getWpcQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisWpcQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisWpcQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisWpcQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/wpc-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveWpcQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisWpcQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisWpcQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisWpcQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisWpcQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisWpcQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/sit-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getSitQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisSitQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisSitQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisSitQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/sit-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveSitQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisSitQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisSitQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisSitQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisSitQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisSitQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

        @GetMapping("/leads/{id}/lmpc-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getLmpcQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisLmpcQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisLmpcQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisLmpcQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/lmpc-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveLmpcQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisLmpcQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisLmpcQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisLmpcQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisLmpcQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisLmpcQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/dpiit-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getDpiitQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", bisDpiitQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? bisDpiitQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : bisDpiitQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/dpiit-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveDpiitQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BisDpiitQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && bisDpiitQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (bisDpiitQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        bisDpiitQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", bisDpiitQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/drug-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getDrugQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", drugQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? drugQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : drugQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/drug-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveDrugQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody DrugQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && drugQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (drugQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        drugQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", drugQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cbisi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCbIsiQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cbIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cbIsiQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cbIsiQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cbisi-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCbIsiQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CbIsiQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        quotation.certificateType = 18;
        if (quotation.id != null && cbIsiQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cbIsiQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        if (quotation.scopeRowsJson == null) quotation.scopeRowsJson = "[]";
        cbIsiQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cbIsiQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/cbfmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCbFmcsQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cbFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? cbFmcsQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : cbFmcsQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/cbfmcs-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveCbFmcsQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CbFmcsQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && cbFmcsQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (cbFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        cbFmcsQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", cbFmcsQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    public static class CbRdsoRequest {
        public CbRdsoQuotation quotation;
        public List<CbRdsoDetail> details;
    }

    @GetMapping("/leads/{id}/cbrdso-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getCbRdsoQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        CbRdsoQuotation quotation = revisionId == null
                ? cbRdsoQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null)
                : cbRdsoQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null);
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", cbRdsoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", quotation);
        response.put("details", quotation == null ? List.of() : cbRdsoDetailRepository.findByCbRdsoFkIdOrderByIdAsc(quotation.id));
        return response;
    }

    @PostMapping("/leads/{id}/cbrdso-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> saveCbRdsoQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody CbRdsoRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        CbRdsoQuotation quotation = request.quotation;
        if (quotation == null) {
            response.put("success", false);
            response.put("message", "Quotation data missing");
            return response;
        }
        quotation.leadId = id;
        quotation.certificateType = 20;
        if (quotation.id != null) {
            CbRdsoQuotation existing = cbRdsoQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).orElse(null);
            if (existing == null) {
                response.put("success", false);
                response.put("message", "Quotation revision not found for this lead");
                return response;
            }
            // keep columns the form does not send
            quotation.payment1 = existing.payment1;
            quotation.payment2 = existing.payment2;
            quotation.payment3 = existing.payment3;
            quotation.createdBy = existing.createdBy;
            quotation.createdAt = existing.createdAt;
        } else {
            quotation.referenceNo = "REF-" + id + "-R" + (cbRdsoQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        }
        CbRdsoQuotation saved = cbRdsoQuotationRepository.save(quotation);

        // replace this quotation's item rows in cb_rdso_details
        cbRdsoDetailRepository.deleteByCbRdsoFkId(saved.id);
        List<CbRdsoDetail> details = new java.util.ArrayList<>();
        if (request.details != null) {
            for (CbRdsoDetail detail : request.details) {
                detail.id = null;
                detail.cbRdsoFkId = saved.id;
                details.add(detail);
            }
        }
        cbRdsoDetailRepository.saveAll(details);

        response.put("success", true);
        response.put("quotation", saved);
        response.put("details", details);
        response.put("quotations", cbRdsoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    public static class RdsoRequest {
        public RdsoQuotation quotation;
        public List<RdsoDetail> details;
    }

    @GetMapping("/leads/{id}/rdso-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getRdsoQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        RdsoQuotation quotation = revisionId == null
                ? rdsoQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null)
                : rdsoQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null);
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", rdsoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", quotation);
        response.put("details", quotation == null ? List.of() : rdsoDetailRepository.findByRdsoFkIdOrderByIdAsc(quotation.id));
        return response;
    }

    @PostMapping("/leads/{id}/rdso-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> saveRdsoQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody RdsoRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        RdsoQuotation quotation = request.quotation;
        if (quotation == null) {
            response.put("success", false);
            response.put("message", "Quotation data missing");
            return response;
        }
        quotation.leadId = id;
        quotation.certificateType = 12;
        if (quotation.id != null) {
            RdsoQuotation existing = rdsoQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).orElse(null);
            if (existing == null) {
                response.put("success", false);
                response.put("message", "Quotation revision not found for this lead");
                return response;
            }
            quotation.payment1 = existing.payment1;
            quotation.payment2 = existing.payment2;
            quotation.payment3 = existing.payment3;
            quotation.createdBy = existing.createdBy;
            quotation.createdAt = existing.createdAt;
        } else {
            quotation.referenceNo = "REF-" + id + "-R" + (rdsoQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        }
        RdsoQuotation saved = rdsoQuotationRepository.save(quotation);

        // replace this quotation's rows in r_d_s_o_details
        rdsoDetailRepository.deleteByRdsoFkId(saved.id);
        List<RdsoDetail> details = new java.util.ArrayList<>();
        if (request.details != null) {
            for (RdsoDetail detail : request.details) {
                detail.id = null;
                detail.rdsoFkId = saved.id;
                details.add(detail);
            }
        }
        rdsoDetailRepository.saveAll(details);

        response.put("success", true);
        response.put("quotation", saved);
        response.put("details", details);
        response.put("quotations", rdsoQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/electronicepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getElectronicEprQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", electronicEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? electronicEprQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : electronicEprQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/electronicepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveElectronicEprQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody ElectronicEprQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && electronicEprQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (electronicEprQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        electronicEprQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", electronicEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/plasticepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getPlasticEprQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", plasticEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? plasticEprQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : plasticEprQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/plasticepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> savePlasticEprQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody PlasticEprQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && plasticEprQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (plasticEprQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        plasticEprQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", plasticEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @GetMapping("/leads/{id}/batteryepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getBatteryEprQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", batteryEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", revisionId == null ? batteryEprQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null) : batteryEprQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null));
        return response;
    }

    @PostMapping("/leads/{id}/batteryepr-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> saveBatteryEprQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BatteryEprQuotation quotation) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        quotation.leadId = id;
        if (quotation.id != null && batteryEprQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).isEmpty()) {
            response.put("success", false); response.put("message", "Quotation revision not found for this lead"); return response;
        }
        if (quotation.id == null) quotation.referenceNo = "REF-" + id + "-R" + (batteryEprQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
        batteryEprQuotationRepository.save(quotation);
        response.put("success", true);
        response.put("quotation", quotation);
        response.put("quotations", batteryEprQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    public static class BeeQuotationRequest {
        public BeeQuotation quotation;
        public List<BeeQuotationFee> fees;
    }

    @GetMapping("/leads/{id}/bee-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getBeeQuotation(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(required = false) Long revisionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        BeeQuotation quotation = revisionId == null
                ? beeQuotationRepository.findFirstByLeadIdOrderByIdDesc(id).orElse(null)
                : beeQuotationRepository.findById(revisionId).filter(q -> id.equals(q.leadId)).orElse(null);
        response.put("success", true);
        response.put("lead", lead);
        response.put("quotations", beeQuotationRepository.findByLeadIdOrderByIdAsc(id));
        response.put("quotation", quotation);
        response.put("fees", quotation == null ? List.of() : beeQuotationFeeRepository.findByBeeQuotationIdOrderBySortOrderAsc(quotation.id));
        return response;
    }

    @PostMapping("/leads/{id}/bee-quotation")
    @org.springframework.web.bind.annotation.ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> saveBeeQuotation(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody BeeQuotationRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }
        BeeQuotation quotation = request.quotation;
        if (quotation == null) {
            response.put("success", false);
            response.put("message", "Quotation data missing");
            return response;
        }
        quotation.leadId = id;
        quotation.certificateType = 9;
        if (quotation.id != null) {
            BeeQuotation existing = beeQuotationRepository.findById(quotation.id).filter(q -> id.equals(q.leadId)).orElse(null);
            if (existing == null) {
                response.put("success", false);
                response.put("message", "Quotation revision not found for this lead");
                return response;
            }
            quotation.createdAt = existing.createdAt;
        } else {
            quotation.referenceNo = "REF-" + id + "-R" + (beeQuotationRepository.findByLeadIdOrderByIdAsc(id).size() + 1);
            quotation.createdAt = java.time.LocalDateTime.now();
        }
        quotation.updatedAt = java.time.LocalDateTime.now();
        BeeQuotation saved = beeQuotationRepository.save(quotation);

        beeQuotationFeeRepository.deleteByBeeQuotationId(saved.id);
        List<BeeQuotationFee> fees = new java.util.ArrayList<>();
        if (request.fees != null) {
            int order = 0;
            for (BeeQuotationFee fee : request.fees) {
                fee.id = null;
                fee.beeQuotationId = saved.id;
                fee.sortOrder = order++;
                fee.createdAt = java.time.LocalDateTime.now();
                fee.updatedAt = java.time.LocalDateTime.now();
                fees.add(fee);
            }
        }
        beeQuotationFeeRepository.saveAll(fees);

        response.put("success", true);
        response.put("quotation", saved);
        response.put("fees", fees);
        response.put("quotations", beeQuotationRepository.findByLeadIdOrderByIdAsc(id));
        return response;
    }

    @PostMapping("/leads/{id}/quick-update")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> quickUpdateLead(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam String field,
            @RequestParam String value) {

        Map<String, Object> response = new LinkedHashMap<>();
        Lead lead = leadRepository.findById(id).orElse(null);

        if (lead == null) {
            response.put("success", false);
            response.put("message", "Lead not found");
            return response;
        }

        try {
            if ("status".equals(field)) {
                lead.setStatus(value);
            } else if ("sourceId".equals(field)) {
                if (value == null || value.trim().isEmpty()) {
                    lead.setSourceId(null);
                } else {
                    lead.setSourceId(Long.parseLong(value));
                }
            } else if ("certificateTypeId".equals(field)) {
                Long certificateTypeId = Long.parseLong(value);
                if (!CERTIFICATE_TYPE_LABELS.containsKey(certificateTypeId)) {
                    response.put("success", false);
                    response.put("message", "Invalid certificate type");
                    return response;
                }
                lead.setCertificateTypeId(certificateTypeId);
            } else if ("followUpNotes".equals(field)) {
                lead.setReminder(value);
            } else {
                response.put("success", false);
                response.put("message", "Invalid field");
                return response;
            }

            leadRepository.save(lead);
            response.put("success", true);
            response.put("field", field);
            response.put("value", value);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/leads/bulk-assign")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> bulkAssignLeads(
            @RequestParam List<Long> leadIds,
            @RequestParam Long userId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            List<Lead> leads = leadRepository.findAllById(leadIds);
            for (Lead lead : leads) {
                lead.setAssignTo(userId);
            }
            leadRepository.saveAll(leads);
            response.put("success", true);
            response.put("updated", leads.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

}
