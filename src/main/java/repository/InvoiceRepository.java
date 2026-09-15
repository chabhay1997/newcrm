package repository;

import model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    // used for next invoice/PO number generation
    Optional<Invoice> findTopByInvTypeOrderByIdDesc(Integer invType);
    List<Invoice> findAllByInvTypeOrderByIdDesc(Integer invType);
    boolean existsByInvTypeAndInvNoIgnoreCase(Integer invType, String invNo);
}
