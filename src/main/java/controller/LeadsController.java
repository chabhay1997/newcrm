package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LeadsController {

    @GetMapping("/leads")
    public String showLeads(Model model) {
        model.addAttribute("activePage", "leads");
        return "leads/leads";
    }

}