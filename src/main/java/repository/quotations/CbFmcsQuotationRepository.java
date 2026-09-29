package repository.quotations;

import model.quotations.CbFmcsQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CbFmcsQuotationRepository extends JpaRepository<CbFmcsQuotation, Long> {
    Optional<CbFmcsQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<CbFmcsQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}