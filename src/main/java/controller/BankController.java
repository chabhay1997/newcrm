package controller;

import dto.BankCreateRequest;
import dto.BankRowDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.BankNameResolver;
import service.BankService;

import java.util.List;

@Controller
public class BankController {

    private final BankService bankService;
    private final BankNameResolver bankNameResolver;

    public BankController(BankService bankService, BankNameResolver bankNameResolver) {
        this.bankService = bankService;
        this.bankNameResolver = bankNameResolver;
    }

    @GetMapping("/bank")
    public String index(Model model) {
        List<BankRowDTO> rows = bankService.findAll().stream()
                .map(bank -> new BankRowDTO(
                        bank.getId(),
                        bankService.creatorName(bank.getCreatedBy()),
                        bankNameResolver.resolve(bank.getBankName()),
                        bank.getBankAcc(),
                        bank.getIfscCode(),
                        resolveAccountType(bank.getAccType()),
                        bank.getStatus() == null ? 1 : bank.getStatus()))
                .toList();
        model.addAttribute("banks", rows);
        model.addAttribute("activePage", "bank");
        return "bank/index";
    }

    @GetMapping("/bank/create")
    public String create(Model model) {
        if (!model.containsAttribute("bank")) {
            model.addAttribute("bank", new BankCreateRequest());
        }
        prepareCreatePage(model);
        return "bank/create";
    }

    @GetMapping("/bank/{id}/edit")
    public String edit(@org.springframework.web.bind.annotation.PathVariable Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        return bankService.findById(id).map(bank -> {
            BankCreateRequest request = new BankCreateRequest();
            request.setBankDetails(bank.getBankDetails());
            request.setBankName(bank.getBankName());
            request.setBranch(bank.getBranch());
            request.setBankAcc(bank.getBankAcc());
            request.setIfscCode(bank.getIfscCode());
            request.setAccType(bank.getAccType());
            request.setMicrCode(bank.getMicrCode());
            request.setSwiftCode(bank.getSwiftCode());
            model.addAttribute("bank", request);
            prepareEditPage(model, id);
            return "bank/create";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Bank record not found.");
            return "redirect:/bank";
        });
    }

    @PostMapping("/bank")
    public String store(
            @ModelAttribute("bank") BankCreateRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (request.getBankName() == null || isBlank(request.getBankAcc()) || request.getAccType() == null) {
            model.addAttribute("errorMessage", "Bank name, account number, and account type are required.");
            prepareCreatePage(model);
            return "bank/create";
        }

        if (!bankService.create(request)) {
            model.addAttribute("errorMessage", "A bank with this account number already exists.");
            prepareCreatePage(model);
            return "bank/create";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Bank saved successfully.");
        return "redirect:/bank";
    }

    @PostMapping("/bank/{id}")
    public String update(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @ModelAttribute("bank") BankCreateRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (request.getBankName() == null || isBlank(request.getBankAcc()) || request.getAccType() == null) {
            model.addAttribute("errorMessage", "Bank name, account number, and account type are required.");
            prepareEditPage(model, id);
            return "bank/create";
        }

        try {
            if (!bankService.update(id, request)) {
                model.addAttribute("errorMessage", "A bank with this account number already exists.");
                prepareEditPage(model, id);
                return "bank/create";
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/bank";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Bank updated successfully.");
        return "redirect:/bank";
    }

    @PostMapping("/bank/{id}/delete")
    public String delete(@org.springframework.web.bind.annotation.PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            bankService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Bank deleted successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/bank";
    }

    private void preparePage(Model model) {
        model.addAttribute("activePage", "bank");
        model.addAttribute("bankNames", bankNameResolver.options());
    }

    private void prepareCreatePage(Model model) {
        preparePage(model);
        model.addAttribute("pageTitle", "Add New Bank");
        model.addAttribute("pageSubtitle", "Enter bank details to add a new bank account.");
        model.addAttribute("formAction", "/bank");
    }

    private void prepareEditPage(Model model, Long id) {
        preparePage(model);
        model.addAttribute("pageTitle", "Edit Bank");
        model.addAttribute("pageSubtitle", "Update the selected bank account details.");
        model.addAttribute("formAction", "/bank/" + id);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveAccountType(Integer accountType) {
        if (accountType == null) return "—";
        return switch (accountType) {
            case 1 -> "Savings Account";
            case 2 -> "Current Account";
            case 3 -> "Cash Credit Account";
            case 4 -> "Overdraft Account";
            case 5 -> "Salary Account";
            case 6 -> "Fixed Deposit Account";
            case 7 -> "Recurring Deposit Account";
            case 8 -> "NRE Account";
            case 9 -> "NRO Account";
            case 10 -> "FCNR Account";
            case 11 -> "Demat Account";
            case 12 -> "Loan Account";
            default -> "Account Type " + accountType;
        };
    }
}
