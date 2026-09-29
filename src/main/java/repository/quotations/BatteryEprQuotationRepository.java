package repository.quotations;

import model.quotations.BatteryEprQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatteryEprQuotationRepository extends JpaRepository<BatteryEprQuotation, Long> {
    Optional<BatteryEprQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BatteryEprQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}