package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.PathVariable;
=======
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4

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
<<<<<<< HEAD

    @GetMapping("/lab-equipment/quotation/{id}/edit")
    public String showEditQuotation(@PathVariable long id, Model model) {
        model.addAttribute("activePage", "lab-equipment-quotation");
        model.addAttribute("quotationDate", LocalDate.now());
        model.addAttribute("editQuotationId", id);
        return "lab-equipment/create-quotation";
    }
=======
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
}
