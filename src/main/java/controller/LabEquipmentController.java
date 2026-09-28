package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class LabEquipmentController {

    @GetMapping("/lab-equipment/quotation")
    public String showQuotations(Model model) {
        model.addAttribute("activePage", "lab-equipment-quotation");
        return "lab-equipment/quotation";
    }

    @GetMapping("/lab-equipment/quotation/create")
    public String showCreateQuotation(Model model) {
        model.addAttribute("activePage", "lab-equipment-quotation");
        model.addAttribute("quotationDate", LocalDate.now());
        return "lab-equipment/create-quotation";
    }
}
