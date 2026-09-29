package repository.quotations;

import model.quotations.CbIsiQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CbIsiQuotationRepository extends JpaRepository<CbIsiQuotation, Long> {
    Optional<CbIsiQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<CbIsiQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}