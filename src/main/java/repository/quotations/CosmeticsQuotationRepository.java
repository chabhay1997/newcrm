package repository.quotations;

import model.quotations.CosmeticsQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CosmeticsQuotationRepository extends JpaRepository<CosmeticsQuotation, Long> {
    Optional<CosmeticsQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<CosmeticsQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}
