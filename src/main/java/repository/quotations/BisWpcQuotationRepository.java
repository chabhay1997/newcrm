package repository.quotations;

import model.BisWpcQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BisWpcQuotationRepository extends JpaRepository<BisWpcQuotation, Long> {
    Optional<BisWpcQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisWpcQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}