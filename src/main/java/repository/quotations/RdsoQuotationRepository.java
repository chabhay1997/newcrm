package repository.quotations;

import model.quotations.RdsoQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RdsoQuotationRepository extends JpaRepository<RdsoQuotation, Long> {
    Optional<RdsoQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<RdsoQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}