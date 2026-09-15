package controller;

import dto.ChallanCreateRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.StateRepository;
import service.ChallanService;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ChallanController {
    private final ChallanService challanService;
    private final StateRepository stateRepository;

    public ChallanController(ChallanService challanService, StateRepository stateRepository) {
        this.challanService = challanService;
        this.stateRepository = stateRepository;
    }

    @GetMapping("/challan")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "challan");
        return "challan/index";
    }

    @GetMapping("/challan/create")
    public String createForm(Model model) {
        ChallanCreateRequest form = new ChallanCreateRequest();
        form.setChallanNo(challanService.nextChallanNumberPreview());
        form.setDate(LocalDate.now());
        form.setSampleReturnDate(LocalDate.now().plusDays(90));
        populateCreatePage(model, form, false, null);
        return "challan/create";
    }

    @GetMapping("/challan/analytics")
    public String analytics(Model model) {
        model.addAttribute("activePage", "challan");
        return "challan/analytics";
    }

    @GetMapping("/challan/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        ChallanCreateRequest form = challanService.findForEdit(id);
        populateCreatePage(model, form, true, id);
        return "challan/create";
    }

    @PostMapping(value = "/challan/create", consumes = "multipart/form-data")
    public String create(@ModelAttribute("challanForm") ChallanCreateRequest form,
                         @RequestParam(name = "uploads", required = false) List<MultipartFile> uploads,
                         Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            var saved = challanService.createChallan(form, uploads, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Challan " + saved.getChallanNo() + " created successfully.");
            return "redirect:/challan";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            form.setChallanNo(challanService.nextChallanNumberPreview());
            model.addAttribute("errorMessage", exception.getMessage());
            populateCreatePage(model, form, false, null);
            return "challan/create";
        }
    }

    @PostMapping(value = "/challan/{id}/edit", consumes = "multipart/form-data")
    public String update(@PathVariable long id, @ModelAttribute("challanForm") ChallanCreateRequest form,
                         @RequestParam(name = "uploads", required = false) List<MultipartFile> uploads,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            var saved = challanService.updateChallan(id, form, uploads);
            redirectAttributes.addFlashAttribute("successMessage", "Challan " + saved.getChallanNo() + " updated successfully.");
            return "redirect:/challan";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            populateCreatePage(model, form, true, id);
            return "challan/create";
        }
    }

    private void populateCreatePage(Model model, ChallanCreateRequest form, boolean editMode, Long challanId) {
        model.addAttribute("challanForm", form);
        model.addAttribute("states", stateRepository.findByCountryIdOrderByNameAsc(101));
        model.addAttribute("activePage", "challan");
        model.addAttribute("editMode", editMode);
        model.addAttribute("challanId", challanId);
    }
}
