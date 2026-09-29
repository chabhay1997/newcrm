package repository.quotations;

import model.quotations.SchemeXForeignQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemeXForeignQuotationRepository extends JpaRepository<SchemeXForeignQuotation, Long> {
    Optional<SchemeXForeignQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<SchemeXForeignQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}