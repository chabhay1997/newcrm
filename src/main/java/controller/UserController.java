package controller;

import model.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/index")
    public String showIndex(Model model,
                            @RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "active") String status,
                            @RequestParam(defaultValue = "25") String size,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "-1") int pageNumber,
                            @RequestParam(defaultValue = "") String highlightId,
                            Authentication authentication) {
        model.addAttribute("activePage", "users");
        model.addAttribute("authRole", isAdmin(authentication));
        if (pageNumber > 0) {
            page = pageNumber - 1;
        }
        Long highlightedUserId = parseId(highlightId);
        Page<User> users = userService.findUsers(search, status, size, page, highlightedUserId);
        model.addAttribute("users", users.getContent());
        model.addAttribute("totalUsers", users.getTotalElements());
        model.addAttribute("currentPage", users.getNumber());
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("pageSize", userService.pageSize(size));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        model.addAttribute("highlightId", highlightId);
        model.addAttribute("temporaryStatusUpdate", !highlightId.isBlank());
        model.addAttribute("serialOffset", (long) users.getNumber() * users.getSize());
        model.addAttribute("showingFrom", users.getTotalElements() == 0 ? 0 : users.getNumber() * users.getSize() + 1);
        model.addAttribute("showingTo", Math.min((long) (users.getNumber() + 1) * users.getSize(), users.getTotalElements()));
        return "users/index";
    }

    @PostMapping("/users/{id}/name")
    public String updateName(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(defaultValue = "") String search,
                             @RequestParam(defaultValue = "all") String status,
                             @RequestParam(defaultValue = "25") String size,
                             @RequestParam(defaultValue = "0") int page,
                             Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        userService.updateName(id, name);
        return redirectToIndex(search, status, size, page);
    }

    @GetMapping("/users/create")
    public String create(Model model, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        model.addAttribute("activePage", "users");
        model.addAttribute("user", new User());
        model.addAttribute("permissionGroups", userService.permissionGroups());
        model.addAttribute("selectedPermissions", java.util.Set.of());
        model.addAttribute("roles", userService.findRoles());
        return "users/edit";
    }

    @GetMapping("/users/{id}/edit")
    public String edit(@PathVariable Long id, Model model, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        model.addAttribute("activePage", "users");
        User user = userService.getUser(id);
        model.addAttribute("user", user);
        model.addAttribute("permissionGroups", userService.permissionGroups());
        model.addAttribute("selectedPermissions", userService.selectedPermissions(user));
        model.addAttribute("roles", userService.findRoles());
        return "users/edit";
    }

    @GetMapping("/users/location")
    public String location(Model model, Authentication authentication) {
        model.addAttribute("activePage", "users");
        if (authentication != null) {
            User user = userService.getUserByEmail(authentication.getName());
            if (user != null) {
                model.addAttribute("latitude", user.getLatitude());
                model.addAttribute("longitude", user.getLongitude());
            }
        }
        return "users/location";
    }

    @PostMapping("/users/location")
    public String saveLocation(Authentication authentication,
                               @RequestParam String latitude,
                               @RequestParam String longitude) {
        userService.updateLocation(authentication.getName(), latitude, longitude);
        return "redirect:/dashboard";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String location,
                             @RequestParam(required = false) Integer status,
                             @RequestParam(required = false) BigDecimal latitude,
                             @RequestParam(required = false) BigDecimal longitude,
                             @RequestParam(required = false) Integer roleId,
                             @RequestParam(required = false) LocalDate dob,
                             @RequestParam(required = false) String password,
                             @RequestParam(required = false) String[] permissions,
                             Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        userService.createUser(name, email, phone, location, status, latitude, longitude, roleId, dob, password, permissions);
        return "redirect:/users/index";
    }

    @PostMapping("/users/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String location,
                         @RequestParam(required = false) Integer status,
                         @RequestParam(required = false) BigDecimal latitude,
                         @RequestParam(required = false) BigDecimal longitude,
                         @RequestParam(required = false) Integer roleId,
                         @RequestParam(required = false) LocalDate dob,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String[] permissions,
                         Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        userService.updateUser(id, name, email, phone, location, status, latitude, longitude, roleId, dob, password, permissions);
        return "redirect:/users/index";
    }

    @PostMapping("/users/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               @RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "active") String filterStatus,
                               @RequestParam(defaultValue = "25") String size,
                               @RequestParam(defaultValue = "0") int page,
                               Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        userService.updateStatus(id, status);
        if (status != null && status == 0) {
            return "redirect:/users/index?search=" + encode(search) + "&status=" + encode(filterStatus) + "&size=" + encode(size)
                + "&page=" + Math.max(page, 0) + "&highlightId=" + id;
        }
        return redirectToIndex(search, filterStatus, size, page);
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String search,
                         @RequestParam(defaultValue = "all") String status,
                         @RequestParam(defaultValue = "25") String size,
                         @RequestParam(defaultValue = "0") int page,
                         Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/users/index";
        }
        userService.delete(id);
        return redirectToIndex(search, status, size, page);
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "") String search,
                                         @RequestParam(defaultValue = "all") String status) {
        List<User> users = userService.findUsersForExport(search, status);
        StringBuilder csv = new StringBuilder("S. No,Name,Email,Phone,Location,Status,Created On\n");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            csv.append(csvValue(i + 1)).append(',')
                    .append(csvValue(user.getName())).append(',')
                    .append(csvValue(user.getEmail())).append(',')
                    .append(csvValue(user.getPhone())).append(',')
                    .append(csvValue(user.getLocation())).append(',')
                    .append(csvValue(user.getStatus() != null && user.getStatus() == 1 ? "Active" : "Inactive")).append(',')
                    .append(csvValue(user.getCreatedAt())).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String redirectToIndex(String search, String status, String size, int page) {
        return "redirect:/users/index?search=" + encode(search) + "&status=" + encode(status)
                + "&size=" + encode(size) + "&page=" + Math.max(page, 0);
    }

    private String encode(Object value) {
        return java.net.URLEncoder.encode(value == null ? "" : value.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Long parseId(String value) {
        try {
            return value == null || value.isBlank() ? 0L : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && userService.isAdmin(authentication.getName());
    }

}
