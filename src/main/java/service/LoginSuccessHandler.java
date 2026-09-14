package service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public LoginSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String latitude = request.getParameter("latitude");
        String longitude = request.getParameter("longitude");
        if (latitude == null || latitude.isBlank() || longitude == null || longitude.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/users/location");
            return;
        }
        userService.updateLocation(authentication.getName(), latitude, longitude);
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}