package repository;

import model.QuotationTestingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationTestingEquipmentRepository extends JpaRepository<QuotationTestingEquipment, Integer> {
    boolean existsByInvoiceNo(String invoiceNo);
    List<QuotationTestingEquipment> findByTestingEquipmentId(Integer testingEquipmentId);
    void deleteByTestingEquipmentId(Integer testingEquipmentId);
    QuotationTestingEquipment findFirstByTestingEquipmentIdOrderByDateDescIdDesc(Integer testingEquipmentId);
}
