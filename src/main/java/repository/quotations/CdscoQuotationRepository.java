package repository.quotations;

import model.CdscoQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CdscoQuotationRepository extends JpaRepository<CdscoQuotation, Long> {
    Optional<CdscoQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<CdscoQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}
