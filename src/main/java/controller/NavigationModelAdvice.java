package controller;

import model.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import repository.UserRepository;

@ControllerAdvice
public class NavigationModelAdvice {
    private final UserRepository users;

    public NavigationModelAdvice(UserRepository users) { this.users = users; }

    @ModelAttribute("canAccessOperation")
    public boolean canAccessOperation(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        User user = users.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return false;
        if (user.getRoleId() != null && user.getRoleId() == 1) return true;
        return user.getPermissions() != null && user.getPermissions().toLowerCase().contains("operation");
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "User";
        return users.findByEmail(authentication.getName())
                .map(User::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(authentication.getName());
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() ? "" : authentication.getName();
    }
}
