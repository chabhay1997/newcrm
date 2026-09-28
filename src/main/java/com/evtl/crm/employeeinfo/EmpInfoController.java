package com.evtl.crm.employeeinfo;

import jakarta.servlet.http.HttpSession;
import model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class EmpInfoController {

    private final EmpInfoService empInfoService;
    private final UserRepository userRepository;

    public EmpInfoController(EmpInfoService empInfoService, UserRepository userRepository) {
        this.empInfoService = empInfoService;
        this.userRepository = userRepository;
    }

    @ModelAttribute("employeeTypeOptions")
    public Map<Integer, String> employeeTypeOptions() {
        Map<Integer, String> options = new LinkedHashMap<>();
        options.put(1, "Current Employee");
        options.put(2, "No Longer Employed");
        return options;
    }

    @ModelAttribute("departmentOptions")
    public Map<Integer, String> departmentOptions() {
        Map<Integer, String> options = new LinkedHashMap<>();
        options.put(1, "Operation");
        options.put(2, "Operation-ISI");
        options.put(3, "Marketing");
        options.put(4, "Accounts");
        options.put(5, "Website");
        options.put(6, "Quality Control Engineer");
        return options;
    }

    @ModelAttribute("designationOptions")
    public Map<Integer, String> designationOptions() {
        Map<Integer, String> options = new LinkedHashMap<>();
        options.put(1, "Operation Executive");
        options.put(2, "Operation Manager");
        options.put(3, "Business Development Executive");
        options.put(4, "Business Development Manager");
        options.put(5, "Accountant");
        options.put(6, "Quality Control Engineer");
        options.put(7, "PHP Laravel Developer");
        options.put(8, "Front-End Developer");
        options.put(9, "SEO Expert");
        options.put(10, "Reception");
        return options;
    }

    @ModelAttribute("simOptions")
    public Map<Integer, String> simOptions() {
        Map<Integer, String> options = new LinkedHashMap<>();
        options.put(1, "Airtel");
        options.put(2, "Vodafone");
        options.put(3, "BSNL");
        options.put(4, "JIO");
        options.put(5, "Personal");
        options.put(6, "NO");
        return options;
    }

    @ModelAttribute("userNameMap")
    public Map<String, String> userNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            if (user.getId() != null) {
                names.put(String.valueOf(user.getId()), user.getName() == null ? "" : user.getName());
            }
        }
        return names;
    }

    @GetMapping({"/emp-info", "/emp_info", "/employee/emp-info", "/employee/emp_info"})
    public String index(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "25") String size,
                        Model model) {
        List<EmpInfo> employees = empInfoService.findAll();
        String normalizedSearch = search == null ? "" : search.trim();

        if (!normalizedSearch.isEmpty()) {
            String query = normalizedSearch.toLowerCase();
            employees = employees.stream()
                    .filter(emp ->
                            (emp.getName() != null && emp.getName().toLowerCase().contains(query)) ||
                            (emp.getEmail() != null && emp.getEmail().toLowerCase().contains(query)) ||
                            (emp.getMobileNo() != null && emp.getMobileNo().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        String normalizedSize = size == null || size.trim().isBlank() ? "25" : size.trim();
        int pageSize = "all".equalsIgnoreCase(normalizedSize) ? Math.max(employees.size(), 1) : switch (normalizedSize) {
            case "50" -> 50;
            case "100" -> 100;
            default -> 25;
        };

        int zeroBasedPage = page > 0 ? page - 1 : 0;
        int totalPages = employees.isEmpty() ? 0 : (int) Math.ceil((double) employees.size() / pageSize);
        int currentPage = totalPages == 0 ? 0 : Math.min(Math.max(zeroBasedPage, 0), totalPages - 1);
        int showingFrom = employees.isEmpty() ? 0 : currentPage * pageSize + 1;
        int showingTo = employees.isEmpty() ? 0 : Math.min(showingFrom + pageSize - 1, employees.size());

        List<EmpInfo> pageEmployees = employees.isEmpty() ? List.of() : employees.subList(showingFrom - 1, showingTo);

        model.addAttribute("activePage", "emp_info");
        model.addAttribute("employees", pageEmployees);
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("size", normalizedSize);
        model.addAttribute("page", currentPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("showingFrom", showingFrom);
        model.addAttribute("showingTo", showingTo);
        model.addAttribute("totalEmployees", employees.size());
        return "employee/emp_info/index";
    }

    @GetMapping({"/emp-info/create", "/emp_info/create", "/employee/emp-info/create", "/employee/emp_info/create"})
    public String create(Model model) {
        model.addAttribute("empInfo", new EmpInfo());
        model.addAttribute("isEdit", false);
        return "employee/emp_info/create";
    }

    @GetMapping({"/emp-info/{id}/edit", "/emp_info/{id}/edit", "/employee/emp-info/{id}/edit", "/employee/emp_info/{id}/edit"})
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("empInfo", empInfoService.findById(id));
        model.addAttribute("isEdit", true);
        return "employee/emp_info/create";
    }

    @PostMapping({"/emp-info", "/emp_info", "/employee/emp-info", "/employee/emp_info"})
    public String save(@ModelAttribute("empInfo") EmpInfo empInfo,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes,
                       HttpSession session) {
        if (empInfo.getName() == null || empInfo.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Employee name is required.");
            return "redirect:/emp-info/create";
        }
        if (empInfo.getJoinDate() == null) {
            redirectAttributes.addFlashAttribute("error", "Join date is required.");
            return "redirect:/emp-info/create";
        }

        if (empInfo.getId() == null && authentication != null && authentication.getName() != null) {
            empInfo.setCreatedBy("0");
        }

        empInfoService.save(empInfo);
        redirectAttributes.addFlashAttribute("success", empInfo.getId() == null ? "Employee information added successfully." : "Employee information updated successfully.");
        return "redirect:/emp-info";
    }

    @PostMapping({"/emp-info/delete", "/emp_info/delete", "/employee/emp-info/delete", "/employee/emp_info/delete"})
    public String delete(Long id, RedirectAttributes redirectAttributes) {
        if (id != null) {
            empInfoService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Employee information deleted successfully.");
        }
        return "redirect:/emp-info";
    }
}
