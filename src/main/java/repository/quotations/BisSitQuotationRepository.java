package repository.quotations;

import model.quotations.BisSitQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BisSitQuotationRepository extends JpaRepository<BisSitQuotation, Long> {
    Optional<BisSitQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisSitQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}