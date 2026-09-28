package repository;

import model.BisIsiAmcQuotationRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BisIsiAmcQuotationRevisionRepository extends JpaRepository<BisIsiAmcQuotationRevision, Long> {
    List<BisIsiAmcQuotationRevision> findByOperationIdOrderByRevisionNumberAsc(Long operationId);
    Optional<BisIsiAmcQuotationRevision> findByOperationIdAndRevisionNumber(Long operationId, int revisionNumber);
}
