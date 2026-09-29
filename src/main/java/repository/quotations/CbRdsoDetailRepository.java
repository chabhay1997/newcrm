package repository.quotations;

import model.quotations.CbRdsoDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CbRdsoDetailRepository extends JpaRepository<CbRdsoDetail, Long> {
    List<CbRdsoDetail> findByCbRdsoFkIdOrderByIdAsc(Long cbRdsoFkId);
    void deleteByCbRdsoFkId(Long cbRdsoFkId);
}