package repository;

import model.InvoiceTermsCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceTermsConditionRepository extends JpaRepository<InvoiceTermsCondition, Long> {
    List<InvoiceTermsCondition> findByInvTypeOrderByIdAsc(Integer invType);
}