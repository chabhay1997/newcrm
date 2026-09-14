package repository.quotations;

import model.BisFmcsQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BisFmcsQuotationRepository extends JpaRepository<BisFmcsQuotation, Long> {
    Optional<BisFmcsQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisFmcsQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}
