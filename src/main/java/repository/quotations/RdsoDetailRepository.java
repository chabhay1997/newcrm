package repository.quotations;

import model.quotations.RdsoDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RdsoDetailRepository extends JpaRepository<RdsoDetail, Long> {
    List<RdsoDetail> findByRdsoFkIdOrderByIdAsc(Long rdsoFkId);
    void deleteByRdsoFkId(Long rdsoFkId);
}