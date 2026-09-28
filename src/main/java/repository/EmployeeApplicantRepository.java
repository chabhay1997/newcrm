package repository;

import model.EmployeeApplicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeApplicantRepository extends JpaRepository<EmployeeApplicant, Long> {

    /**
     * Explicitly fetch every application.  This avoids any implicit paging or
     * derived-query interpretation on the list screen.
     */
    @Query(value = "SELECT * FROM employee_applications ORDER BY created_at DESC, id DESC", nativeQuery = true)
    List<EmployeeApplicant> findAllForListing();

    Page<EmployeeApplicant> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
            String name, String email, String phone, Pageable pageable);
    Page<EmployeeApplicant> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
