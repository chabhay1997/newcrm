package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
<<<<<<< HEAD
        return "dashboard/index";
=======
        return "dashboard/dashboard";
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
    }

}