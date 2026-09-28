package repository;
import model.IsiChecklist; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface IsiChecklistRepository extends JpaRepository<IsiChecklist,Long>{ Optional<IsiChecklist> findByOperationId(Long id); void deleteByOperationId(Long id); }
