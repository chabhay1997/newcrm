package controller;

import model.User;
import model.BisCrsRenewal;
import dto.BisCrsRenewalForm;
import dto.BisCrsCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.BisCrsRenewalRepository;
import repository.UserRepository;
import service.OperationAccessService;
import service.BisCrsExcelService;
import service.BisCrsRenewalCreationService;
import service.DuplicateBisCrsRenewalException;

import java.util.Map;
import java.util.stream.Collectors;

/** Entry point for the BIS CRS renewal operation workspace. */
@Controller
public class BisCrsController {
    private final OperationAccessService access;
    private final BisCrsRenewalRepository renewals;
    private final UserRepository users;
    private final BisCrsExcelService excel;
    private final BisCrsRenewalCreationService creation;

    public BisCrsController(OperationAccessService access, BisCrsRenewalRepository renewals, UserRepository users,
                            BisCrsExcelService excel, BisCrsRenewalCreationService creation) {
        this.access = access;
        this.renewals = renewals;
        this.users = users;
        this.excel = excel;
        this.creation = creation;
    }

    @GetMapping("/operation/bis-crs")
    public String index(Authentication authentication, Model model,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "0") int page) {
        User user = access.require(authentication);
        int selectedSize = size == 10 || size == 25 || size == 50 || size == 100 ? size : 25;
        Pageable pageable = PageRequest.of(Math.max(0, page), selectedSize);
        String query = search == null ? "" : search.trim();
        Page<BisCrsRenewal> records = renewals.search(query, pageable);
        if (records.getTotalPages() > 0 && records.getNumber() >= records.getTotalPages())
            records = renewals.search(query, PageRequest.of(records.getTotalPages() - 1, selectedSize));
        Map<Long, String> creatorNames = users.findAllById(records.getContent().stream()
                        .map(BisCrsRenewal::getCreatedBy).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getName));

        model.addAttribute("activePage", "operation-bis-crs");
        model.addAttribute("operationAdmin", access.isAdmin(user));
        model.addAttribute("records", records);
        model.addAttribute("creatorNames", creatorNames);
        model.addAttribute("search", query);
        model.addAttribute("size", selectedSize);
        return "operation/bis-crs/index";
    }

    @PostMapping("/operation/bis-crs/{id}/delete")
    public String delete(Authentication authentication, @PathVariable Long id,
                         @RequestParam(defaultValue = "") String search,
                         @RequestParam(defaultValue = "25") int size,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirectAttributes) {
        access.requireAdmin(authentication);
        BisCrsRenewal record = renewals.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "BIS CRS record not found"));
        renewals.delete(record);
        redirectAttributes.addFlashAttribute("success", "BIS CRS record deleted successfully.");
        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("page", Math.max(0, page));
        return "redirect:/operation/bis-crs";
    }

    @PostMapping("/operation/bis-crs")
    public String create(Authentication authentication, BisCrsRenewalForm form, RedirectAttributes redirectAttributes) {
        try {
            creation.create(access.require(authentication), createRequest(form));
            redirectAttributes.addFlashAttribute("success", "BIS CRS record added successfully.");
            return "redirect:/operation/bis-crs";
        } catch (IllegalArgumentException | DuplicateBisCrsRenewalException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/operation/bis-crs/create";
        }
    }

    @GetMapping("/operation/bis-crs/create")
    public String createPage(Authentication authentication, Model model) {
        access.require(authentication);
        model.addAttribute("activePage", "operation-bis-crs");
        model.addAttribute("today", java.time.LocalDate.now());
        BisCrsRenewalForm form = new BisCrsRenewalForm();
        form.setLicenceDate(java.time.LocalDate.now());
        form.setExpiryDate(java.time.LocalDate.now());
        model.addAttribute("form", form);
        model.addAttribute("editing", false);
        return "operation/bis-crs/form";
    }

    @GetMapping("/operation/bis-crs/{id}/edit")
    public String editPage(Authentication authentication, @PathVariable Long id, Model model) {
        access.require(authentication);
        BisCrsRenewal record = renewals.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "BIS CRS record not found"));
        BisCrsRenewalForm form = toForm(record);
        model.addAttribute("activePage", "operation-bis-crs");
        model.addAttribute("today", java.time.LocalDate.now());
        model.addAttribute("form", form);
        model.addAttribute("editing", true);
        model.addAttribute("recordId", id);
        return "operation/bis-crs/form";
    }

    @PostMapping("/operation/bis-crs/{id}")
    public String update(Authentication authentication, @PathVariable Long id, BisCrsRenewalForm form,
                         RedirectAttributes redirectAttributes) {
        access.require(authentication);
        BisCrsRenewal record = renewals.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "BIS CRS record not found"));
        String licenceNumber = clean(form.getLicenceNumber());
        String manufacturerName = clean(form.getManufacturerName());
        String productName = clean(form.getProductName());
        String airName = clean(form.getAirName());
        String brandName = clean(form.getBrandName());
        String currentStatus = clean(form.getCurrentStatus());
        if (manufacturerName.isEmpty() || form.getNotifyDate() == null) {
            redirectAttributes.addFlashAttribute("error", "Manufacturer Name and Notify Date are required.");
            return "redirect:/operation/bis-crs/" + id + "/edit";
        }
        if (!currentStatus.equals("Active") && !currentStatus.equals("Differed")
                && !currentStatus.equals("Registered") && !currentStatus.equals("Expired")) {
            redirectAttributes.addFlashAttribute("error",
                    "Current Status must be Active, Differed, Registered or Expired.");
            return "redirect:/operation/bis-crs/" + id + "/edit";
        }
        if (renewals.existsExactRecordExcludingId(id, licenceNumber, manufacturerName, productName, airName,
                brandName, form.getLicenceDate(), currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "This BIS CRS record already exists.");
            return "redirect:/operation/bis-crs/" + id + "/edit";
        }
        applyForm(record, form);
        renewals.save(record);
        redirectAttributes.addFlashAttribute("success", "BIS CRS record updated successfully.");
        return "redirect:/operation/bis-crs";
    }

    @PostMapping("/operation/bis-crs/import")
    public String importExcel(Authentication authentication, @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            BisCrsExcelService.ImportResult result = excel.importFile(access.require(authentication), file);
            redirectAttributes.addFlashAttribute("success", result.imported() + " BIS CRS record"
                    + (result.imported() == 1 ? "" : "s") + " imported successfully."
                    + (result.skipped() == 0 ? "" : " " + result.skipped() + " row(s) skipped."));
            if (!result.errors().isEmpty()) redirectAttributes.addFlashAttribute("error", String.join(" | ", result.errors()));
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/operation/bis-crs";
    }

    @GetMapping(value = "/operation/bis-crs/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication, @RequestParam(required = false) String search) {
        access.require(authentication);
        String query = search == null ? "" : search.trim();
        byte[] spreadsheet = excel.export(renewals.search(query, Pageable.unpaged()).getContent());
        String filename = "bis-crs-" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(spreadsheet.length).body(spreadsheet);
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private BisCrsCreateRequest createRequest(BisCrsRenewalForm form) {
        return new BisCrsCreateRequest(form.getManufacturerName(), form.getProductName(), form.getLicenceNumber(),
                form.getIsStandard(), form.getLicenceDate(), form.getExpiryDate(), form.getNotifyDate(),
                form.getBrandName(), form.getAirName(), form.getAirAddress(), form.getAirEmailId(),
                form.getAirContactNumber(), form.getCurrentStatus());
    }

    private void applyForm(BisCrsRenewal record, BisCrsRenewalForm form) {
        record.setLicenceNumber(clean(form.getLicenceNumber()));
        record.setManufacturerName(clean(form.getManufacturerName()));
        record.setProductName(clean(form.getProductName()));
        record.setIsStandard(clean(form.getIsStandard()));
        record.setAirName(clean(form.getAirName()));
        record.setBrandName(clean(form.getBrandName()));
        record.setLicenceDate(form.getLicenceDate());
        record.setExpiryDate(form.getExpiryDate());
        record.setNotifyDate(form.getNotifyDate());
        record.setAirAddress(clean(form.getAirAddress()));
        record.setAirEmailId(clean(form.getAirEmailId()));
        record.setAirContactNumber(clean(form.getAirContactNumber()));
        record.setCurrentStatus(clean(form.getCurrentStatus()));
    }

    private BisCrsRenewalForm toForm(BisCrsRenewal record) {
        BisCrsRenewalForm form = new BisCrsRenewalForm();
        form.setLicenceNumber(record.getLicenceNumber());
        form.setManufacturerName(record.getManufacturerName());
        form.setProductName(record.getProductName());
        form.setIsStandard(record.getIsStandard());
        form.setAirName(record.getAirName());
        form.setBrandName(record.getBrandName());
        form.setLicenceDate(record.getLicenceDate());
        form.setExpiryDate(record.getExpiryDate());
        form.setNotifyDate(record.getNotifyDate());
        form.setAirAddress(record.getAirAddress());
        form.setAirEmailId(record.getAirEmailId());
        form.setAirContactNumber(record.getAirContactNumber());
        form.setCurrentStatus(record.getCurrentStatus());
        return form;
    }
}
