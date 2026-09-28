package repository;

import model.TestingEquipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
=======
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4

public interface TestingEquipmentRepository extends JpaRepository<TestingEquipment, Integer> {

    Page<TestingEquipment> findByInvoiceNoContainingIgnoreCaseOrAttentionContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrIsCodeContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
            String invoiceNo, String attention, String clientName, String isCode, String companyName, Pageable pageable);
<<<<<<< HEAD

    Page<TestingEquipment> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("""
            select equipment from TestingEquipment equipment
            where equipment.date between :startDate and :endDate
              and (lower(coalesce(equipment.invoiceNo, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(equipment.attention, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(equipment.clientName, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(equipment.isCode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(equipment.companyName, '')) like lower(concat('%', :query, '%')))
            """)
    Page<TestingEquipment> searchByDateBetween(@Param("query") String query,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                Pageable pageable);

    List<TestingEquipment> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<TestingEquipment> findByInvoiceNoOrderByIdAsc(String invoiceNo);
=======
>>>>>>> 91cef887e4e2d034cf24dd0094d8272c8b034ea4
}
