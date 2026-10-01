package service;

import java.util.Set;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadPermissionService {

    private final UserService userService;

    public LeadPermissionService(UserService userService) {
        this.userService = userService;
    }

    public boolean has(Authentication authentication, String module, String action) {
        User user = authenticatedUser(authentication);
        if (user == null) return false;
        Set<String> permissions = userService.selectedPermissions(user);
        return permissions.contains(module + "." + action) || permissions.contains(module);
    }

    public void require(Authentication authentication, String module, String action) {
        if (!has(authentication, module, action)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing permission: " + module + "." + action);
        }
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userService.getUserByEmail(authentication.getName());
    }
}
