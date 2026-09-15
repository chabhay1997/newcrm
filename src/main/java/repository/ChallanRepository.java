package repository;

import model.Challan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChallanRepository extends JpaRepository<Challan, Long> {
    @Query(value = "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'challans'", nativeQuery = true)
    Long findNextAutoIncrement();
    long countByIdLessThanEqual(Long id);
    boolean existsByChallanNoIgnoreCase(String challanNo);
    List<Challan> findBySampleReturnDateBetweenOrderBySampleReturnDateAsc(LocalDate startDate, LocalDate endDate);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE challans SET challan_no = REPLACE(challan_no, 'Evtl/Challan/', 'Evtl/DC/') WHERE challan_no LIKE 'Evtl/Challan/%'", nativeQuery = true)
    int migrateLegacyChallanNumbersToDc();
    Page<Challan> findByChallanNoContainingIgnoreCaseOrClientNameContainingIgnoreCaseOrItemNameContainingIgnoreCase(
            String challanNo, String clientName, String itemName, Pageable pageable);

    @Query("""
            select c from Challan c
            where (:query = '' or lower(coalesce(c.challanNo, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.clientName, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.itemName, '')) like lower(concat('%', :query, '%')))
              and (:startDate is null or c.date >= :startDate)
              and (:endDate is null or c.date <= :endDate)
            """)
    Page<Challan> findFiltered(@Param("query") String query, @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("""
            select c from Challan c
            where (:query = '' or lower(coalesce(c.challanNo, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.clientName, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(c.itemName, '')) like lower(concat('%', :query, '%')))
              and (:startDate is null or c.date >= :startDate)
              and (:endDate is null or c.date <= :endDate)
            """)
    List<Challan> findFiltered(@Param("query") String query, @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate, Sort sort);
}
