package repository.quotations;

import model.quotations.BeeQuotationFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeeQuotationFeeRepository extends JpaRepository<BeeQuotationFee, Long> {
    List<BeeQuotationFee> findByBeeQuotationIdOrderBySortOrderAsc(Long beeQuotationId);
    void deleteByBeeQuotationId(Long beeQuotationId);
}