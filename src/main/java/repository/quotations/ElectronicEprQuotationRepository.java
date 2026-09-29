package repository.quotations;

import model.quotations.ElectronicEprQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElectronicEprQuotationRepository extends JpaRepository<ElectronicEprQuotation, Long> {
    Optional<ElectronicEprQuotation> findFirstByLeadIdOrderByIdDesc(Long leadId);
    List<ElectronicEprQuotation> findByLeadIdOrderByIdAsc(Long leadId);
}