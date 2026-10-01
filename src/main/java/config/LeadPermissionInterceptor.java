package config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.LeadPermissionService;

@Component
public class LeadPermissionInterceptor implements HandlerInterceptor {

    private final LeadPermissionService permissions;

    public LeadPermissionInterceptor(LeadPermissionService permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals("/leads") && !path.startsWith("/leads/")) return true;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String method = request.getMethod();

        if ("GET".equals(method) && "/leads/momentum-data".equals(path)) {
            permissions.require(authentication, "leads_dashboard", "read");
            return true;
        }
        if ("GET".equals(method) && "/leads".equals(path)) return true;
        if ("GET".equals(method)) {
            permissions.require(authentication, "all_lead", "read");
            return true;
        }
        if (!"POST".equals(method)) return true;

        if ("/leads/add".equals(path)) {
            permissions.require(authentication, "all_lead", "write");
        } else if (path.matches("/leads/\\d+/delete-ajax")) {
            if (!permissions.has(authentication, "leads_delete", "delete")) {
                permissions.require(authentication, "all_lead", "delete");
            }
        } else if (path.matches("/leads/delete/\\d+")) {
            permissions.require(authentication, "all_lead", "delete");
        } else if ("/leads/bulk-assign".equals(path)
                || path.matches("/leads/update/\\d+")
                || path.matches("/leads/\\d+/quick-update")
                || path.matches("/leads/meeting-reminders/\\d+/dismiss")) {
            permissions.require(authentication, "all_lead", "edit");
        } else if (path.matches("/leads/\\d+/meeting-reminders")) {
            permissions.require(authentication, "all_lead", "write");
        } else if (path.matches("/leads/\\d+/.+-quotation")) {
            permissions.require(authentication, "all_lead", "write");
        }
        return true;
    }
}
