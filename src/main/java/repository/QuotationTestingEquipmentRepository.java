package repository;

import model.QuotationTestingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import java.util.List;

public interface QuotationTestingEquipmentRepository extends JpaRepository<QuotationTestingEquipment, Integer> {
    boolean existsByInvoiceNo(String invoiceNo);
    List<QuotationTestingEquipment> findByTestingEquipmentId(Integer testingEquipmentId);
    void deleteByTestingEquipmentId(Integer testingEquipmentId);
    QuotationTestingEquipment findFirstByTestingEquipmentIdOrderByDateDescIdDesc(Integer testingEquipmentId);
=======

public interface QuotationTestingEquipmentRepository extends JpaRepository<QuotationTestingEquipment, Integer> {
    boolean existsByInvoiceNo(String invoiceNo);
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
}
