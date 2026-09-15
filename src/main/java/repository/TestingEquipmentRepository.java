package repository;

import model.TestingEquipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestingEquipmentRepository extends JpaRepository<TestingEquipment, Integer> {

    Page<TestingEquipment> findByInvoiceNoContainingIgnoreCaseOrAttentionContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrIsCodeContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
            String invoiceNo, String attention, String clientName, String isCode, String companyName, Pageable pageable);
}
