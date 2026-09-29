package repository.quotations;

import model.quotations.PlasticEprQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlasticEprQuotationRepository extends JpaRepository<PlasticEprQuotation, Long> {
    Optional<PlasticEprQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<PlasticEprQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}