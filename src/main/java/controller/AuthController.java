package controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import repository.RoleRepository;

@Controller
public class AuthController {

    private final RoleRepository roleRepository;

    public AuthController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("roles", roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        return "auth/login";
    }
}
