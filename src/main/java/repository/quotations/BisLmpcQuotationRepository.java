package repository.quotations;

import model.BisLmpcQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BisLmpcQuotationRepository extends JpaRepository<BisLmpcQuotation, Long> {
    Optional<BisLmpcQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisLmpcQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}