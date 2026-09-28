package repository;

import model.BisIsiAmcQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BisIsiAmcQuotationRepository extends JpaRepository<BisIsiAmcQuotation, Long> {
    Optional<BisIsiAmcQuotation> findByOperationId(Long operationId);
    List<BisIsiAmcQuotation> findByOperationIdIn(Collection<Long> operationIds);
}
