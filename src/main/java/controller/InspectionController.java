package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InspectionController {

    @GetMapping("/inspection")
    public String showInspection(Model model) {
        model.addAttribute("activePage", "inspection");
        return "inspection/inspection";
    }

}