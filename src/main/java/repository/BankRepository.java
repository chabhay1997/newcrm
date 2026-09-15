package repository;

import model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    boolean existsByBankAcc(String bankAcc);
    boolean existsByBankAccAndIdNot(String bankAcc, Long id);
    List<Bank> findAllByOrderByIdDesc();
    Optional<Bank> findFirstByOrderByIdAsc();
    Optional<Bank> findFirstByBankDetailsContainingIgnoreCaseOrderByIdAsc(String bankDetails);
}
