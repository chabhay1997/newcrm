package service;

import dto.BankCreateRequest;
import model.Bank;
import model.GlobalSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.BankRepository;
import repository.GlobalSettingRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class BankService {

    @Autowired private BankRepository bankRepository;
    @Autowired private GlobalSettingRepository globalSettingRepository;
    @Autowired private UserRepository userRepository;

    public List<Bank> findAll() {
        return bankRepository.findAllByOrderByIdDesc();
    }

    public Optional<Bank> findById(Long id) {
        return bankRepository.findById(id);
    }

    /** Mirrors bank(): reject duplicate account numbers. */
    public boolean create(BankCreateRequest request) {
        if (bankRepository.existsByBankAcc(request.getBankAcc())) {
            return false;
        }

        Bank bank = new Bank();
        bank.setBankDetails(request.getBankDetails());
        bank.setBankName(request.getBankName());
        bank.setBranch(request.getBranch());
        bank.setBankAcc(request.getBankAcc());
        bank.setIfscCode(request.getIfscCode());
        bank.setAccType(request.getAccType());
        bank.setMicrCode(request.getMicrCode());
        bank.setSwiftCode(request.getSwiftCode());
        bank.setCreatedBy(currentUserId());
        bank.setStatus(1);
        bank.setCreatedAt(LocalDateTime.now());
        bank.setUpdatedAt(LocalDateTime.now());

        bankRepository.save(bank);
        return true;
    }

    public boolean update(Long id, BankCreateRequest request) {
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank record not found."));
        if (bankRepository.existsByBankAccAndIdNot(request.getBankAcc(), id)) {
            return false;
        }

        bank.setBankDetails(request.getBankDetails());
        bank.setBankName(request.getBankName());
        bank.setBranch(request.getBranch());
        bank.setBankAcc(request.getBankAcc());
        bank.setIfscCode(request.getIfscCode());
        bank.setAccType(request.getAccType());
        bank.setMicrCode(request.getMicrCode());
        bank.setSwiftCode(request.getSwiftCode());
        bank.setUpdatedAt(LocalDateTime.now());
        bankRepository.save(bank);
        return true;
    }

    public void delete(Long id) {
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank record not found."));
        bankRepository.delete(bank);
    }

    public String creatorName(Long userId) {
        if (userId == null) return "—";
        return userRepository.findById(userId).map(user -> user.getName()).orElse("—");
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByEmail(authentication.getName()).map(user -> user.getId()).orElse(null);
    }

    /** Mirrors chooseBank(): updates the single global settings row (id=1). */
    public boolean chooseBank(Long bankChoose) {
        Optional<GlobalSetting> globalOpt = globalSettingRepository.findById(1L);
        if (globalOpt.isEmpty()) {
            return false;
        }
        GlobalSetting global = globalOpt.get();
        global.setBankChoose(bankChoose);
        globalSettingRepository.save(global);
        return true;
    }
}
