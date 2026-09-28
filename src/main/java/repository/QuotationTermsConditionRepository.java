package repository;
import model.QuotationTermsCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface QuotationTermsConditionRepository extends JpaRepository<QuotationTermsCondition, Long> { Optional<QuotationTermsCondition> findFirstByOrderByIdDesc(); }
