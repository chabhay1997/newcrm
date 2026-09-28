package repository;
import model.BisPreInspection; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface BisPreInspectionRepository extends JpaRepository<BisPreInspection,Long>{ Optional<BisPreInspection> findByOperationId(Long id); void deleteByOperationId(Long id); }
