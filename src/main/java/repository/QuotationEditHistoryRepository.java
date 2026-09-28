package repository;

import model.QuotationEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuotationEditHistoryRepository extends JpaRepository<QuotationEditHistory, Long> {
    List<QuotationEditHistory> findByTestingEquipmentIdOrderByEditedAtDesc(Integer testingEquipmentId);
    void deleteByTestingEquipmentId(Integer testingEquipmentId);
}
