package repository;

import model.InvoiceEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceEditHistoryRepository extends JpaRepository<InvoiceEditHistory, Long> {
    List<InvoiceEditHistory> findByInvoiceIdOrderByEditedAtDesc(Long invoiceId);
}
