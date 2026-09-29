package repository.quotations;

import model.quotations.CbRdsoQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CbRdsoQuotationRepository extends JpaRepository<CbRdsoQuotation, Long> {
    Optional<CbRdsoQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<CbRdsoQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}