package repository.quotations;

import model.BisCrsQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BisCrsQuotationRepository extends JpaRepository<BisCrsQuotation, Long> {
    Optional<BisCrsQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<BisCrsQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}
