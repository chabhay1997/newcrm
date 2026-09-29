package repository.quotations;

import model.quotations.BeeQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeeQuotationRepository extends JpaRepository<BeeQuotation, Long> {
    Optional<BeeQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BeeQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}