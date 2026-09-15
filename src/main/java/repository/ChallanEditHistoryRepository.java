package repository;

import model.ChallanEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallanEditHistoryRepository extends JpaRepository<ChallanEditHistory, Long> {
    List<ChallanEditHistory> findByChallanIdOrderByEditedAtDesc(Long challanId);
    void deleteByChallanId(Long challanId);
}
