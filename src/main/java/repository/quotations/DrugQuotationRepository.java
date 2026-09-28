package repository.quotations;

import model.DrugQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DrugQuotationRepository extends JpaRepository<DrugQuotation, Long> {
    Optional<DrugQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<DrugQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}