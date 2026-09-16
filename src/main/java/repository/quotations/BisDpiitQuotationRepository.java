package repository.quotations;

import model.BisDpiitQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BisDpiitQuotationRepository extends JpaRepository<BisDpiitQuotation, Long> {
    Optional<BisDpiitQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisDpiitQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}