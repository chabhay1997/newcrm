package repository;

import model.QuotationTestingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationTestingEquipmentRepository extends JpaRepository<QuotationTestingEquipment, Integer> {
    boolean existsByInvoiceNo(String invoiceNo);
}
