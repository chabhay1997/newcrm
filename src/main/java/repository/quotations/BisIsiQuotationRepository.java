package repository.quotations;

import model.BisIsiQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BisIsiQuotationRepository extends JpaRepository<BisIsiQuotation, Long> {
    Optional<BisIsiQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisIsiQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}
